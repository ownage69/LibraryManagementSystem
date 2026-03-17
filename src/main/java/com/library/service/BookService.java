package com.library.service;

import com.library.cache.BookFilterCache;
import com.library.cache.BookFilterCacheKey;
import com.library.dto.BookCreateDto;
import com.library.dto.BookDto;
import com.library.dto.BookPageDto;
import com.library.entity.Author;
import com.library.entity.Book;
import com.library.entity.Category;
import com.library.entity.Publisher;
import com.library.mapper.BookMapper;
import com.library.repository.AuthorRepository;
import com.library.repository.BookRepository;
import com.library.repository.CategoryRepository;
import com.library.repository.PublisherRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookMapper bookMapper;
    private final BookFilterCache bookFilterCache;

    @Transactional(readOnly = true)
    public List<BookDto> findAll() {
        return bookRepository.findAllWithGraph()
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookDto> findAllWithNplusone() {
        return bookRepository.findAll()
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookDto> findAllWithEntityGraph() {
        return bookRepository.findAllWithGraph()
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookDto findById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + id));
        return bookMapper.toDto(book);
    }

    @Transactional(readOnly = true)
    public List<BookDto> searchBooksByAuthor(String author) {
        if (author == null || author.isBlank()) {
            return bookRepository.findAllWithGraph()
                    .stream()
                    .map(bookMapper::toDto)
                    .toList();
        }

        return bookRepository.findByAuthorName(author)
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookPageDto filterBooksJpql(
            String authorLastName,
            String categoryName,
            String publisherCountry,
            int page,
            int size
    ) {
        return filterBooks(authorLastName, categoryName, publisherCountry, page, size, "jpql");
    }

    @Transactional(readOnly = true)
    public BookPageDto filterBooksNative(
            String authorLastName,
            String categoryName,
            String publisherCountry,
            int page,
            int size
    ) {
        return filterBooks(authorLastName, categoryName, publisherCountry, page, size, "native");
    }

    public BookDto create(BookCreateDto bookCreateDto) {
        Book book = bookMapper.toEntity(bookCreateDto);
        applyRelations(book, bookCreateDto);
        Book saved = bookRepository.save(book);
        invalidateFilterCache();
        return bookMapper.toDto(saved);
    }

    public BookDto update(Long id, BookCreateDto bookCreateDto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + id));

        bookMapper.updateEntityFromDto(bookCreateDto, book);
        applyRelations(book, bookCreateDto);

        Book saved = bookRepository.save(book);
        invalidateFilterCache();
        return bookMapper.toDto(saved);
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new NoSuchElementException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
        invalidateFilterCache();
    }

    private void applyRelations(Book book, BookCreateDto bookCreateDto) {
        Publisher publisher = publisherRepository.findById(bookCreateDto.getPublisherId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Publisher not found with id: " + bookCreateDto.getPublisherId()
                ));

        Set<Author> authors = resolveAuthors(bookCreateDto.getAuthorIds());
        Set<Category> categories = resolveCategories(bookCreateDto.getCategoryIds());

        book.setPublisher(publisher);
        book.setAuthors(authors);
        book.setCategories(categories);
    }

    private Set<Author> resolveAuthors(Set<Long> authorIds) {
        List<Author> authors = authorRepository.findAllById(authorIds);
        if (authors.size() != authorIds.size()) {
            throw new NoSuchElementException("One or more authors not found");
        }
        return new HashSet<>(authors);
    }

    private Set<Category> resolveCategories(Set<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllById(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new NoSuchElementException("One or more categories not found");
        }
        return new HashSet<>(categories);
    }

    public void invalidateFilterCache() {
        bookFilterCache.clear();
    }

    private BookPageDto filterBooks(
            String authorLastName,
            String categoryName,
            String publisherCountry,
            int page,
            int size,
            String queryType
    ) {
        String normalizedAuthorLastName = normalizeFilter(authorLastName);
        String normalizedCategoryName = normalizeFilter(categoryName);
        String normalizedPublisherCountry = normalizeFilter(publisherCountry);
        BookFilterCacheKey key = new BookFilterCacheKey(
                queryType,
                normalizedAuthorLastName,
                normalizedCategoryName,
                normalizedPublisherCountry,
                page,
                size
        );
        BookPageDto cachedResponse = bookFilterCache.get(key);
        if (cachedResponse != null) {
            return copyPage(cachedResponse, true);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Book> bookPage;
        if ("native".equals(queryType)) {
            bookPage = bookRepository.findByFiltersNative(
                    normalizedAuthorLastName,
                    normalizedCategoryName,
                    normalizedPublisherCountry,
                    pageable
            );
        } else {
            bookPage = bookRepository.findByFiltersJpql(
                    normalizedAuthorLastName,
                    normalizedCategoryName,
                    normalizedPublisherCountry,
                    pageable
            );
        }

        List<Book> books = "native".equals(queryType)
                ? loadBooksForNativePage(bookPage)
                : bookPage.getContent();
        BookPageDto response = new BookPageDto(
                books.stream().map(bookMapper::toDto).toList(),
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages(),
                false,
                queryType
        );
        bookFilterCache.put(key, response);
        return response;
    }

    private List<Book> loadBooksForNativePage(Page<Book> bookPage) {
        List<Long> ids = bookPage.getContent().stream().map(Book::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<Long, Book> booksById = bookRepository.findAllByIdInWithGraph(ids)
                .stream()
                .collect(
                        LinkedHashMap::new,
                        (map, book) -> map.put(book.getId(), book),
                        LinkedHashMap::putAll
                );
        return ids.stream()
                .map(booksById::get)
                .toList();
    }

    private BookPageDto copyPage(BookPageDto source, boolean cached) {
        return new BookPageDto(
                source.getContent(),
                source.getPage(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                cached,
                source.getQueryType()
        );
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
