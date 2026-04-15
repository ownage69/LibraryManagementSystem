package com.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private PublisherRepository publisherRepository;

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookFilterIndex bookFilterIndex;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(
                bookRepository,
                publisherRepository,
                authorRepository,
                categoryRepository,
                loanRepository,
                new BookMapper(),
                bookFilterIndex
        );
    }

    @Test
    void findAllShouldReturnMappedBooks() {
        when(bookRepository.findAllWithGraph()).thenReturn(List.of(createFullBook(1L, "1984")));

        List<BookDto> result = bookService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("1984");
        assertThat(result.get(0).getPublisherName()).isEqualTo("Demo Publisher");
    }

    @Test
    void findAllWithNplusoneShouldReturnMappedBooks() {
        when(bookRepository.findAll()).thenReturn(List.of(createFullBook(2L, "Dune")));

        List<BookDto> result = bookService.findAllWithNplusone();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Dune");
    }

    @Test
    void findAllWithEntityGraphShouldReturnMappedBooks() {
        when(bookRepository.findAllWithGraph()).thenReturn(List.of(createFullBook(3L, "Foundation")));

        List<BookDto> result = bookService.findAllWithEntityGraph();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Foundation");
    }

    @Test
    void findByIdShouldReturnMappedBook() {
        when(bookRepository.findById(4L)).thenReturn(Optional.of(createFullBook(4L, "Hyperion")));

        BookDto result = bookService.findById(4L);

        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getTitle()).isEqualTo("Hyperion");
    }

    @Test
    void findByIdShouldThrowWhenBookMissing() {
        when(bookRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.findById(404L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Book not found with id: 404");
    }

    @Test
    void searchBooksByAuthorShouldReturnAllBooksWhenFilterNull() {
        when(bookRepository.findAllWithGraph()).thenReturn(List.of(createFullBook(44L, "Dracula")));

        List<BookDto> result = bookService.searchBooksByAuthor(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Dracula");
    }

    @Test
    void searchBooksByAuthorShouldReturnAllBooksWhenFilterBlank() {
        when(bookRepository.findAllWithGraph()).thenReturn(List.of(createFullBook(5L, "It")));

        List<BookDto> result = bookService.searchBooksByAuthor("   ");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("It");
    }

    @Test
    void searchBooksByAuthorShouldUseRepositoryFilterWhenValuePresent() {
        when(bookRepository.findByAuthorName("King"))
                .thenReturn(List.of(createFullBook(6L, "The Stand")));

        List<BookDto> result = bookService.searchBooksByAuthor("King");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("The Stand");
    }

    @Test
    void filterBooksJpqlShouldReturnCachedCopy() {
        BookPageDto cachedPage = new BookPageDto(
                List.of(createBookDto(1L, "Cached Book")),
                0,
                5,
                1,
                1,
                "jpql"
        );
        when(bookFilterIndex.get(any(BookFilterCacheKey.class))).thenReturn(Optional.of(cachedPage));

        BookPageDto result = bookService.filterBooksJpql(" Orwell ", null, null, 0, 5);

        assertThat(result).isNotSameAs(cachedPage);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Cached Book");
        verify(bookRepository, never()).findBookIdsByFiltersJpql(
                anyString(),
                anyString(),
                anyString(),
                any(Pageable.class)
        );
    }

    @Test
    void filterBooksJpqlShouldLoadFromRepositoryAndStoreCache() {
        Book book = createFullBook(7L, "Animal Farm");
        Page<Long> bookIdPage = new PageImpl<>(List.of(7L), PageRequest.of(0, 5), 1);
        when(bookFilterIndex.get(any(BookFilterCacheKey.class))).thenReturn(Optional.empty());
        when(bookRepository.findBookIdsByFiltersJpql(
                eq("orwell"),
                eq("classic"),
                eq("uk"),
                any(Pageable.class)
        )).thenReturn(bookIdPage);
        when(bookRepository.findAllByIdInWithGraph(List.of(7L))).thenReturn(List.of(book));

        BookPageDto result = bookService.filterBooksJpql(" Orwell ", " Classic ", " UK ", 0, 5);

        assertThat(result.getQueryType()).isEqualTo("jpql");
        assertThat(result.getContent()).extracting(BookDto::getTitle).containsExactly("Animal Farm");
        verify(bookFilterIndex).put(any(BookFilterCacheKey.class), any(BookPageDto.class));
    }

    @Test
    void filterBooksJpqlShouldTreatBlankFiltersAsEmptyStrings() {
        when(bookFilterIndex.get(any(BookFilterCacheKey.class))).thenReturn(Optional.empty());
        when(bookRepository.findBookIdsByFiltersJpql(eq(""), eq(""), eq(""), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 5)));

        BookPageDto result = bookService.filterBooksJpql("   ", "   ", "   ", 0, 5);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getQueryType()).isEqualTo("jpql");
    }

    @Test
    void filterBooksNativeShouldLoadBooksWithGraphAndPreserveOrder() {
        Page<Long> nativePage = new PageImpl<>(
                List.of(20L, 10L),
                PageRequest.of(0, 5),
                2
        );
        when(bookFilterIndex.get(any(BookFilterCacheKey.class))).thenReturn(Optional.empty());
        when(bookRepository.findBookIdsByFiltersNative(eq(""), eq(""), eq(""), any(Pageable.class)))
                .thenReturn(nativePage);
        when(bookRepository.findAllByIdInWithGraph(List.of(20L, 10L))).thenReturn(List.of(
                createFullBook(10L, "Book 10"),
                createFullBook(20L, "Book 20")
        ));

        BookPageDto result = bookService.filterBooksNative(null, null, null, 0, 5);

        assertThat(result.getQueryType()).isEqualTo("native");
        assertThat(result.getContent())
                .extracting(BookDto::getId)
                .containsExactly(20L, 10L);
    }

    @Test
    void filterBooksNativeShouldReturnEmptyContentWhenPageHasNoIds() {
        when(bookFilterIndex.get(any(BookFilterCacheKey.class))).thenReturn(Optional.empty());
        when(bookRepository.findBookIdsByFiltersNative(eq(""), eq(""), eq(""), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 5)));

        BookPageDto result = bookService.filterBooksNative(null, null, null, 0, 5);

        assertThat(result.getContent()).isEmpty();
        verify(bookRepository, never()).findAllByIdInWithGraph(anyList());
    }

    @Test
    void createShouldSaveBookWithRelationsAndInvalidateCache() {
        BookCreateDto createDto = createBookCreateDto();
        Publisher publisher = createPublisher(9L, "Publisher", "BY");
        Author author1 = createAuthor(1L, "George", "Orwell");
        Author author2 = createAuthor(2L, "Ray", "Bradbury");
        Category category = createCategory(3L, "Classic");

        when(bookRepository.existsByIsbn("9780306406157")).thenReturn(false);
        when(publisherRepository.findById(9L)).thenReturn(Optional.of(publisher));
        when(authorRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(author1, author2));
        when(categoryRepository.findAllById(Set.of(3L))).thenReturn(List.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book savedBook = invocation.getArgument(0);
            savedBook.setId(11L);
            return savedBook;
        });

        BookDto result = bookService.create(createDto);

        ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(bookCaptor.capture());

        Book savedBook = bookCaptor.getValue();
        assertThat(savedBook.getIsbn()).isEqualTo("9780306406157");
        assertThat(savedBook.getPublisher()).isEqualTo(publisher);
        assertThat(savedBook.getAuthors()).containsExactlyInAnyOrder(author1, author2);
        assertThat(savedBook.getCategories()).containsExactly(category);

        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getPublisherName()).isEqualTo("Publisher");
        verify(bookFilterIndex).invalidateAll();
    }

    @Test
    void createShouldThrowWhenDuplicateIsbnExists() {
        BookCreateDto createDto = createBookCreateDto();
        when(bookRepository.existsByIsbn("9780306406157")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(createDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book with ISBN '9780306406157' already exists");
    }

    @Test
    void createShouldThrowWhenAuthorIsMissing() {
        BookCreateDto createDto = createBookCreateDto();
        when(bookRepository.existsByIsbn("9780306406157")).thenReturn(false);
        when(publisherRepository.findById(9L))
                .thenReturn(Optional.of(createPublisher(9L, "Publisher", "BY")));
        when(authorRepository.findAllById(Set.of(1L, 2L)))
                .thenReturn(List.of(createAuthor(1L, "George", "Orwell")));

        assertThatThrownBy(() -> bookService.create(createDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("One or more authors not found");
    }

    @Test
    void createShouldThrowWhenPublisherMissing() {
        BookCreateDto createDto = createBookCreateDto();
        when(bookRepository.existsByIsbn("9780306406157")).thenReturn(false);
        when(publisherRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.create(createDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Publisher not found with id: 9");
    }

    @Test
    void createShouldThrowWhenCategoryIsMissing() {
        BookCreateDto createDto = createBookCreateDto();
        when(bookRepository.existsByIsbn("9780306406157")).thenReturn(false);
        when(publisherRepository.findById(9L))
                .thenReturn(Optional.of(createPublisher(9L, "Publisher", "BY")));
        when(authorRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(
                createAuthor(1L, "George", "Orwell"),
                createAuthor(2L, "Ray", "Bradbury")
        ));
        when(categoryRepository.findAllById(Set.of(3L))).thenReturn(List.of());

        assertThatThrownBy(() -> bookService.create(createDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("One or more categories not found");
    }

    @Test
    void createShouldHandleNullIsbnWhenServiceCalledDirectly() {
        BookCreateDto createDto = new BookCreateDto(
                "Book Without Isbn",
                null,
                "Description",
                2024,
                9L,
                Set.of(1L),
                Set.of(3L)
        );
        Publisher publisher = createPublisher(9L, "Publisher", "BY");
        Author author = createAuthor(1L, "George", "Orwell");
        Category category = createCategory(3L, "Classic");

        when(bookRepository.existsByIsbn(null)).thenReturn(false);
        when(publisherRepository.findById(9L)).thenReturn(Optional.of(publisher));
        when(authorRepository.findAllById(Set.of(1L))).thenReturn(List.of(author));
        when(categoryRepository.findAllById(Set.of(3L))).thenReturn(List.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book savedBook = invocation.getArgument(0);
            savedBook.setId(77L);
            return savedBook;
        });

        BookDto result = bookService.create(createDto);

        assertThat(result.getId()).isEqualTo(77L);
        assertThat(result.getIsbn()).isNull();
    }

    @Test
    void updateShouldModifyBookAndInvalidateCache() {
        Book existingBook = createFullBook(12L, "Old Title");
        BookCreateDto updateDto = new BookCreateDto(
                "New Title",
                " 9780306407001 ",
                "Updated description",
                2024,
                19L,
                Set.of(11L),
                Set.of(21L)
        );
        Publisher publisher = createPublisher(19L, "New Publisher", "PL");
        Author author = createAuthor(11L, "Frank", "Herbert");
        Category category = createCategory(21L, "Sci-Fi");

        when(bookRepository.findById(12L)).thenReturn(Optional.of(existingBook));
        when(bookRepository.existsByIsbnAndIdNot("9780306407001", 12L)).thenReturn(false);
        when(publisherRepository.findById(19L)).thenReturn(Optional.of(publisher));
        when(authorRepository.findAllById(Set.of(11L))).thenReturn(List.of(author));
        when(categoryRepository.findAllById(Set.of(21L))).thenReturn(List.of(category));
        when(bookRepository.save(existingBook)).thenReturn(existingBook);

        BookDto result = bookService.update(12L, updateDto);

        assertThat(existingBook.getTitle()).isEqualTo("New Title");
        assertThat(existingBook.getIsbn()).isEqualTo("9780306407001");
        assertThat(existingBook.getPublisher()).isEqualTo(publisher);
        assertThat(existingBook.getAuthors()).containsExactly(author);
        assertThat(existingBook.getCategories()).containsExactly(category);

        assertThat(result.getId()).isEqualTo(12L);
        assertThat(result.getPublisherName()).isEqualTo("New Publisher");
        verify(bookFilterIndex).invalidateAll();
    }

    @Test
    void updateShouldThrowWhenBookMissing() {
        BookCreateDto updateDto = createBookCreateDto();
        when(bookRepository.findById(120L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(120L, updateDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Book not found with id: 120");
    }

    @Test
    void deleteShouldRemoveLoansClearRelationsAndInvalidateCache() {
        Book book = createFullBook(13L, "Delete Me");
        when(bookRepository.findById(13L)).thenReturn(Optional.of(book));
        when(loanRepository.deleteByBookId(13L)).thenReturn(2L);

        bookService.delete(13L);

        assertThat(book.getAuthors()).isEmpty();
        assertThat(book.getCategories()).isEmpty();
        verify(bookRepository).delete(book);
        verify(bookFilterIndex).invalidateAll();
    }

    @Test
    void deleteShouldThrowWhenBookMissing() {
        when(bookRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.delete(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Book not found with id: 99");
    }

    @Test
    void invalidateFilterCacheShouldDelegateToIndex() {
        bookService.invalidateFilterCache();

        verify(bookFilterIndex).invalidateAll();
    }

    private BookCreateDto createBookCreateDto() {
        return new BookCreateDto(
                "Clean Code",
                " 9780306406157 ",
                "A handbook of agile software craftsmanship",
                2008,
                9L,
                Set.of(1L, 2L),
                Set.of(3L)
        );
    }

    private BookDto createBookDto(Long id, String title) {
        return new BookDto(
                id,
                title,
                "isbn",
                "desc",
                2024,
                1L,
                "Publisher",
                Set.of(1L),
                Set.of("Author"),
                Set.of(2L),
                Set.of("Category")
        );
    }

    private Book createFullBook(Long id, String title) {
        Publisher publisher = createPublisher(1L, "Demo Publisher", "United Kingdom");
        Author author = createAuthor(2L, "George", "Orwell");
        Category category = createCategory(3L, "Classic");

        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setIsbn("isbn-" + id);
        book.setDescription("Description " + id);
        book.setPublishYear(2020);
        book.setPublisher(publisher);
        book.setAuthors(new LinkedHashSet<>(Set.of(author)));
        book.setCategories(new LinkedHashSet<>(Set.of(category)));
        return book;
    }

    private Publisher createPublisher(Long id, String name, String country) {
        Publisher publisher = new Publisher();
        publisher.setId(id);
        publisher.setName(name);
        publisher.setCountry(country);
        return publisher;
    }

    private Author createAuthor(Long id, String firstName, String lastName) {
        Author author = new Author();
        author.setId(id);
        author.setFirstName(firstName);
        author.setLastName(lastName);
        return author;
    }

    private Category createCategory(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
