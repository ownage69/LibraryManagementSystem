package com.library.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;

    @Transactional(readOnly = true)
    public List<LoanDto> findAll() {
        return loanRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public LoanDto findById(Long id) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Loan not found with id: " + id));
        return toDto(loan);
    }

    public LoanDto create(LoanCreateDto loanCreateDto) {
        Book book = bookRepository.findById(loanCreateDto.getBookId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Book not found with id: " + loanCreateDto.getBookId()
                ));

        Reader reader = readerRepository.findById(loanCreateDto.getReaderId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Reader not found with id: " + loanCreateDto.getReaderId()
                ));

        Loan loan = new Loan();
        loan.setBook(book);
        loan.setReader(reader);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(loanCreateDto.getDueDate());
        loan.setReturned(false);

        Loan saved = loanRepository.save(loan);
        return toDto(saved);
    }

    public LoanDto update(Long id, LoanCreateDto loanCreateDto) {
        Loan loan = loanRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Loan not found with id: " + id));

        Book book = bookRepository.findById(loanCreateDto.getBookId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Book not found with id: " + loanCreateDto.getBookId()
                ));

        Reader reader = readerRepository.findById(loanCreateDto.getReaderId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Reader not found with id: " + loanCreateDto.getReaderId()
                ));

        loan.setBook(book);
        loan.setReader(reader);
        loan.setDueDate(loanCreateDto.getDueDate());

        Loan saved = loanRepository.save(loan);
        return toDto(saved);
    }

    public void delete(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new NoSuchElementException("Loan not found with id: " + id);
        }
        loanRepository.deleteById(id);
    }

    private LoanDto toDto(Loan loan) {
        String readerName = loan.getReader().getFirstName() + " " + loan.getReader().getLastName();
        return new LoanDto(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getReader().getId(),
                readerName,
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.isReturned()
        );
    }
}
