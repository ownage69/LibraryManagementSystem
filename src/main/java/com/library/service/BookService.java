package com.library.service;

import com.library.cache.BookFilterCacheKey;
import com.library.cache.BookFilterIndex;
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
import com.library.repository.LoanRepository;
import com.library.repository.PublisherRepository;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final LoanRepository loanRepository;
    private final BookMapper bookMapper;
    private final BookFilterIndex bookFilterIndex;

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
        long startedAt = System.currentTimeMillis();
        log.debug(
                "Выполнение метода: BookService.filterBooksJpql с аргументами: "
                        + "[authorLastName={}, categoryName={}, "
                        + "publisherCountry={}, page={}, size={}]",
                authorLastName,
                categoryName,
                publisherCountry,
                page,
                size
        );
        BookPageDto response = filterBooks(
                authorLastName,
                categoryName,
                publisherCountry,
                page,
                size,
                BookFilterQueryType.JPQL
        );
        log.debug(
                "Метод BookService.filterBooksJpql выполнен за {} мс",
                System.currentTimeMillis() - startedAt
        );
        return response;
    }

    @Transactional(readOnly = true)
    public BookPageDto filterBooksNative(
            String authorLastName,
            String categoryName,
            String publisherCountry,
            int page,
            int size
    ) {
        long startedAt = System.currentTimeMillis();
        log.debug(
                "Выполнение метода: BookService.filterBooksNative с аргументами: "
                        + "[authorLastName={}, categoryName={}, "
                        + "publisherCountry={}, page={}, size={}]",
                authorLastName,
                categoryName,
                publisherCountry,
                page,
                size
        );
        BookPageDto response = filterBooks(
                authorLastName,
                categoryName,
                publisherCountry,
                page,
                size,
                BookFilterQueryType.NATIVE
        );
        log.debug(
                "Метод BookService.filterBooksNative выполнен за {} мс",
                System.currentTimeMillis() - startedAt
        );
        return response;
    }

    @Transactional
    public BookDto create(BookCreateDto bookCreateDto) {
        validateUniqueIsbn(bookCreateDto.getIsbn(), null);
        Book book = bookMapper.toEntity(bookCreateDto);
        book.setIsbn(normalizeIsbn(book.getIsbn()));
        applyRelations(book, bookCreateDto);
        Book saved = bookRepository.save(book);
        invalidateFilterCache();
        return bookMapper.toDto(saved);
    }

    @Transactional
    public BookDto update(Long id, BookCreateDto bookCreateDto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + id));

        validateUniqueIsbn(bookCreateDto.getIsbn(), id);
        bookMapper.updateEntityFromDto(bookCreateDto, book);
        book.setIsbn(normalizeIsbn(book.getIsbn()));
        applyRelations(book, bookCreateDto);

        Book saved = bookRepository.save(book);
        invalidateFilterCache();
        return bookMapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + id));
        final long removedLoans = loanRepository.deleteByBookId(id);
        book.getAuthors().clear();
        book.getCategories().clear();
        bookRepository.delete(book);
        log.debug("Удалено займов для книги id={}: {}", id, removedLoans);
        invalidateFilterCache();
    }

    private void applyRelations(Book book, BookCreateDto bookCreateDto) {
        Publisher publisher = publisherRepository.findById(bookCreateDto.getPublisherId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Publisher not found with id: " + bookCreateDto.getPublisherId()
                ));

        Set<Author> authors = resolveAuthors(bookCreateDto.getAuthorIds());
        final Set<Category> categories = resolveCategories(bookCreateDto.getCategoryIds());

        book.setPublisher(publisher);
        book.getAuthors().clear();
        book.getAuthors().addAll(authors);
        book.getCategories().clear();
        book.getCategories().addAll(categories);
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
        bookFilterIndex.invalidateAll();
        log.info("cache invalidated");
    }

    private BookPageDto filterBooks(
            String authorLastName,
            String categoryName,
            String publisherCountry,
            int page,
            int size,
            BookFilterQueryType queryType
    ) {
        String normalizedAuthorLastName = normalizeFilter(authorLastName);
        String normalizedCategoryName = normalizeFilter(categoryName);
        String normalizedPublisherCountry = normalizeFilter(publisherCountry);
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        BookFilterCacheKey key = new BookFilterCacheKey(
                normalizedAuthorLastName,
                normalizedCategoryName,
                normalizedPublisherCountry,
                pageable,
                queryType
        );
        Optional<BookPageDto> cachedResponse = bookFilterIndex.get(key);
        if (cachedResponse.isPresent()) {
            return copyPage(cachedResponse.get());
        }

        Page<Book> bookPage;
        if (BookFilterQueryType.NATIVE == queryType) {
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

        List<Book> books = BookFilterQueryType.NATIVE == queryType
                ? loadBooksForNativePage(bookPage)
                : bookPage.getContent();
        BookPageDto response = new BookPageDto(
                books.stream().map(bookMapper::toDto).toList(),
                bookPage.getNumber(),
                bookPage.getSize(),
                bookPage.getTotalElements(),
                bookPage.getTotalPages(),
                queryType.name().toLowerCase(Locale.ROOT)
        );
        bookFilterIndex.put(key, response);
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

    private BookPageDto copyPage(BookPageDto source) {
        return new BookPageDto(
                source.getContent(),
                source.getPage(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.getQueryType()
        );
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.trim();
    }

    private void validateUniqueIsbn(String isbn, Long bookId) {
        String normalizedIsbn = normalizeIsbn(isbn);
        boolean duplicate = bookId == null
                ? bookRepository.existsByIsbn(normalizedIsbn)
                : bookRepository.existsByIsbnAndIdNot(normalizedIsbn, bookId);
        if (duplicate) {
            throw new IllegalArgumentException(
                    "Book with ISBN '" + normalizedIsbn + "' already exists"
            );
        }
    }
}
