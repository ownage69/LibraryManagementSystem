package com.library.service;

import com.library.dto.BookCreateDto;
import com.library.dto.BookDto;
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
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
            return findAll();
        }

        return bookRepository.findByAuthorName(author)
                .stream()
                .map(bookMapper::toDto)
                .toList();
    }

    public BookDto create(BookCreateDto bookCreateDto) {
        Book book = bookMapper.toEntity(bookCreateDto);
        applyRelations(book, bookCreateDto);
        Book saved = bookRepository.save(book);
        return bookMapper.toDto(saved);
    }

    public BookDto update(Long id, BookCreateDto bookCreateDto) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Book not found with id: " + id));

        bookMapper.updateEntityFromDto(bookCreateDto, book);
        applyRelations(book, bookCreateDto);

        Book saved = bookRepository.save(book);
        return bookMapper.toDto(saved);
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new NoSuchElementException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
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
}
