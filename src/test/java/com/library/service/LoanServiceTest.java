package com.library.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.dto.LoanCreateDto;
import com.library.dto.LoanDto;
import com.library.entity.Book;
import com.library.entity.Loan;
import com.library.entity.Reader;
import com.library.repository.BookRepository;
import com.library.repository.LoanRepository;
import com.library.repository.ReaderRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReaderRepository readerRepository;

    @InjectMocks
    private LoanService loanService;

    @Test
    void findAllShouldReturnMappedLoanDtos() {
        Loan firstLoan = createLoan(
                1L,
                createBook(3L, "1984"),
                createReader(7L, "Ivan", "Petrov")
        );
        Loan secondLoan = createLoan(
                2L,
                createBook(4L, "Dune"),
                createReader(8L, "Anna", "Sidorova")
        );
        when(loanRepository.findAll()).thenReturn(List.of(firstLoan, secondLoan));

        List<LoanDto> result = loanService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(LoanDto::getBookTitle)
                .containsExactly("1984", "Dune");
    }

    @Test
    void findByIdShouldReturnMappedLoanDto() {
        Loan loan = createLoan(15L, createBook(3L, "1984"), createReader(7L, "Ivan", "Petrov"));
        when(loanRepository.findById(15L)).thenReturn(Optional.of(loan));

        LoanDto result = loanService.findById(15L);

        assertThat(result.getId()).isEqualTo(15L);
        assertThat(result.getBookId()).isEqualTo(3L);
        assertThat(result.getBookTitle()).isEqualTo("1984");
        assertThat(result.getReaderId()).isEqualTo(7L);
        assertThat(result.getReaderName()).isEqualTo("Ivan Petrov");
    }

    @Test
    void findByIdShouldThrowWhenLoanMissing() {
        when(loanRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.findById(404L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Loan not found with id: 404");
    }

    @Test
    void createShouldSaveLoanWhenBookIsAvailable() {
        LoanCreateDto loanCreateDto = new LoanCreateDto(1L, 2L, LocalDate.now().plusDays(14));
        Book book = createBook(1L, "Clean Code");
        Reader reader = createReader(2L, "Anna", "Smirnova");

        when(bookRepository.findAllById(List.of(1L))).thenReturn(List.of(book));
        when(readerRepository.findAllById(List.of(2L))).thenReturn(List.of(reader));
        when(loanRepository.countByBookIdAndReturnedFalse(1L)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setId(100L);
            return savedLoan;
        });

        LoanDto result = loanService.create(loanCreateDto);

        ArgumentCaptor<Loan> loanCaptor = ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository).save(loanCaptor.capture());

        Loan savedLoan = loanCaptor.getValue();
        assertThat(savedLoan.getBook()).isEqualTo(book);
        assertThat(savedLoan.getReader()).isEqualTo(reader);
        assertThat(savedLoan.getDueDate()).isEqualTo(loanCreateDto.getDueDate());
        assertThat(savedLoan.isReturned()).isFalse();

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getBookTitle()).isEqualTo("Clean Code");
        assertThat(result.getReaderName()).isEqualTo("Anna Smirnova");
    }

    @Test
    void createShouldThrowWhenBookAlreadyHasActiveLoan() {
        LoanCreateDto loanCreateDto = new LoanCreateDto(1L, 2L, LocalDate.now().plusDays(7));
        Book book = createBook(1L, "Dune");
        book.setTotalCopies(1);
        Reader reader = createReader(2L, "Pavel", "Morozov");

        when(bookRepository.findAllById(List.of(1L))).thenReturn(List.of(book));
        when(readerRepository.findAllById(List.of(2L))).thenReturn(List.of(reader));
        when(loanRepository.countByBookIdAndReturnedFalse(1L)).thenReturn(1L);

        assertThatThrownBy(() -> loanService.create(loanCreateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No available copy remaining for book id: 1");

        verify(loanRepository, never()).save(any(Loan.class));
    }

    @Test
    void createBulkWithoutTransactionShouldStopAfterFirstSavedLoanWhenSecondBookMissing() {
        LoanCreateDto firstLoan = new LoanCreateDto(1L, 2L, LocalDate.now().plusDays(10));
        LoanCreateDto secondLoan = new LoanCreateDto(99L, 2L, LocalDate.now().plusDays(15));
        List<LoanCreateDto> loanCreateDtos = List.of(firstLoan, secondLoan);
        Book firstBook = createBook(1L, "Refactoring");
        Reader reader = createReader(2L, "Maria", "Sokolova");

        when(bookRepository.findAllById(List.of(1L, 99L))).thenReturn(List.of(firstBook));
        when(readerRepository.findAllById(List.of(2L))).thenReturn(List.of(reader));
        when(loanRepository.countByBookIdAndReturnedFalse(1L)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setId(200L);
            return savedLoan;
        });

        assertThatThrownBy(() -> loanService.createBulkWithoutTransaction(loanCreateDtos))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Book not found with id: 99");

        verify(loanRepository, times(1)).save(any(Loan.class));
    }

    @Test
    void createBulkWithoutTransactionShouldReturnSavedLoans() {
        LoanCreateDto firstLoan = new LoanCreateDto(1L, 2L, LocalDate.now().plusDays(10));
        LoanCreateDto secondLoan = new LoanCreateDto(3L, 4L, LocalDate.now().plusDays(15));
        Book firstBook = createBook(1L, "Refactoring");
        Book secondBook = createBook(3L, "DDD");
        Reader firstReader = createReader(2L, "Maria", "Sokolova");
        Reader secondReader = createReader(4L, "Oleg", "Ivanov");

        when(bookRepository.findAllById(List.of(1L, 3L))).thenReturn(List.of(firstBook, secondBook));
        when(readerRepository.findAllById(List.of(2L, 4L)))
                .thenReturn(List.of(firstReader, secondReader));
        when(loanRepository.countByBookIdAndReturnedFalse(1L)).thenReturn(0L);
        when(loanRepository.countByBookIdAndReturnedFalse(3L)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setId(savedLoan.getBook().getId() + 100L);
            return savedLoan;
        });

        List<LoanDto> result = loanService.createBulkWithoutTransaction(List.of(firstLoan, secondLoan));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LoanDto::getId).containsExactly(101L, 103L);
    }

    @Test
    void createBulkWithTransactionShouldReturnSavedLoans() {
        LoanCreateDto firstLoan = new LoanCreateDto(1L, 2L, LocalDate.now().plusDays(10));
        LoanCreateDto secondLoan = new LoanCreateDto(3L, 4L, LocalDate.now().plusDays(15));
        Book firstBook = createBook(1L, "Refactoring");
        Book secondBook = createBook(3L, "Domain-Driven Design");
        Reader firstReader = createReader(2L, "Maria", "Sokolova");
        Reader secondReader = createReader(4L, "Oleg", "Ivanov");

        when(bookRepository.findAllById(List.of(1L, 3L))).thenReturn(List.of(firstBook, secondBook));
        when(readerRepository.findAllById(List.of(2L, 4L)))
                .thenReturn(List.of(firstReader, secondReader));
        when(loanRepository.countByBookIdAndReturnedFalse(1L)).thenReturn(0L);
        when(loanRepository.countByBookIdAndReturnedFalse(3L)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            if (savedLoan.getBook().getId().equals(1L)) {
                savedLoan.setId(201L);
            } else {
                savedLoan.setId(202L);
            }
            return savedLoan;
        });

        List<LoanDto> result = loanService.createBulkWithTransaction(List.of(firstLoan, secondLoan));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LoanDto::getId).containsExactly(201L, 202L);
    }

    @Test
    void createBulkWithTransactionShouldThrowWhenReaderMissing() {
        LoanCreateDto loanCreateDto = new LoanCreateDto(3L, 99L, LocalDate.now().plusDays(15));
        List<LoanCreateDto> loanCreateDtos = List.of(loanCreateDto);
        Book book = createBook(3L, "DDD");

        when(bookRepository.findAllById(List.of(3L))).thenReturn(List.of(book));
        when(readerRepository.findAllById(List.of(99L))).thenReturn(List.of());

        assertThatThrownBy(() -> loanService.createBulkWithTransaction(loanCreateDtos))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Reader not found with id: 99");
    }

    @Test
    void updateShouldAllowExistingActiveLoanForSameLoanRecord() {
        Loan existingLoan = createLoan(
                5L,
                createBook(1L, "The Pragmatic Programmer"),
                createReader(2L, "Elena", "Romanova")
        );
        existingLoan.getBook().setTotalCopies(1);
        LoanCreateDto updateDto = new LoanCreateDto(1L, 3L, LocalDate.now().plusDays(21));
        Reader newReader = createReader(3L, "Kirill", "Fedorov");

        when(loanRepository.findById(5L)).thenReturn(Optional.of(existingLoan));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingLoan.getBook()));
        when(readerRepository.findById(3L)).thenReturn(Optional.of(newReader));
        when(loanRepository.countByBookIdAndReturnedFalse(1L)).thenReturn(1L);
        when(loanRepository.save(existingLoan)).thenReturn(existingLoan);

        LoanDto result = loanService.update(5L, updateDto);

        assertThat(result.getReaderId()).isEqualTo(3L);
        assertThat(result.getReaderName()).isEqualTo("Kirill Fedorov");
        assertThat(existingLoan.getDueDate()).isEqualTo(updateDto.getDueDate());
    }

    @Test
    void updateShouldThrowWhenLoanMissing() {
        LoanCreateDto updateDto = new LoanCreateDto(1L, 2L, LocalDate.now().plusDays(7));
        when(loanRepository.findById(90L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.update(90L, updateDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Loan not found with id: 90");
    }

    @Test
    void updateShouldThrowWhenBookMissing() {
        Loan existingLoan = createLoan(
                5L,
                createBook(1L, "The Pragmatic Programmer"),
                createReader(2L, "Elena", "Romanova")
        );
        LoanCreateDto updateDto = new LoanCreateDto(99L, 2L, LocalDate.now().plusDays(7));
        when(loanRepository.findById(5L)).thenReturn(Optional.of(existingLoan));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.update(5L, updateDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Book not found with id: 99");
    }

    @Test
    void updateShouldThrowWhenReaderMissing() {
        Loan existingLoan = createLoan(
                5L,
                createBook(1L, "The Pragmatic Programmer"),
                createReader(2L, "Elena", "Romanova")
        );
        LoanCreateDto updateDto = new LoanCreateDto(1L, 99L, LocalDate.now().plusDays(7));
        when(loanRepository.findById(5L)).thenReturn(Optional.of(existingLoan));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existingLoan.getBook()));
        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.update(5L, updateDto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Reader not found with id: 99");
    }

    @Test
    void updateShouldThrowWhenAnotherActiveLoanExistsForBook() {
        Book book = createBook(1L, "The Pragmatic Programmer");
        book.setTotalCopies(1);
        Loan existingLoan = createLoan(5L, book, createReader(2L, "Elena", "Romanova"));
        LoanCreateDto updateDto = new LoanCreateDto(1L, 4L, LocalDate.now().plusDays(21));

        when(loanRepository.findById(5L)).thenReturn(Optional.of(existingLoan));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(readerRepository.findById(4L)).thenReturn(Optional.of(createReader(4L, "Kirill", "Fedorov")));
        when(loanRepository.countByBookIdAndReturnedFalse(1L)).thenReturn(2L);

        assertThatThrownBy(() -> loanService.update(5L, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No available copy remaining for book id: 1");
    }

    @Test
    void deleteShouldRemoveLoanWhenItExists() {
        when(loanRepository.existsById(15L)).thenReturn(true);

        loanService.delete(15L);

        verify(loanRepository).deleteById(15L);
    }

    @Test
    void deleteShouldThrowWhenLoanMissing() {
        when(loanRepository.existsById(77L)).thenReturn(false);

        assertThatThrownBy(() -> loanService.delete(77L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Loan not found with id: 77");
    }

    private Loan createLoan(Long id, Book book, Reader reader) {
        Loan loan = new Loan();
        loan.setId(id);
        loan.setBook(book);
        loan.setReader(reader);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(14));
        loan.setReturned(false);
        return loan;
    }

    private Book createBook(Long id, String title) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        return book;
    }

    private Reader createReader(Long id, String firstName, String lastName) {
        Reader reader = new Reader();
        reader.setId(id);
        reader.setFirstName(firstName);
        reader.setLastName(lastName);
        reader.setEmail(firstName.toLowerCase() + "@example.com");
        return reader;
    }
}
