package com.library.service;

import com.library.dto.ScenarioCreateDto;
import com.library.entity.Author;
import com.library.entity.Book;
import com.library.entity.Category;
import com.library.entity.Loan;
import com.library.entity.Publisher;
import com.library.entity.Reader;
import com.library.repository.AuthorRepository;
import com.library.repository.BookRepository;
import com.library.repository.CategoryRepository;
import com.library.repository.LoanRepository;
import com.library.repository.PublisherRepository;
import com.library.repository.ReaderRepository;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final PublisherRepository publisherRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final LoanRepository loanRepository;

    public String createWithoutTransaction(ScenarioCreateDto scenarioCreateDto) {
        saveLinkedEntitiesByIds(scenarioCreateDto);
        return "Scenario without transaction completed";
    }

    @Transactional
    public String createWithTransaction(ScenarioCreateDto scenarioCreateDto) {
        saveLinkedEntitiesByIds(scenarioCreateDto);
        return "Scenario with transaction completed";
    }

    private void saveLinkedEntitiesByIds(ScenarioCreateDto scenarioCreateDto) {
        Publisher publisher = new Publisher();
        publisher.setName(scenarioCreateDto.getPublisherName() + "-" + System.nanoTime());
        publisher.setCountry("Россия");
        Publisher savedPublisher = publisherRepository.save(publisher);

        final Author author = authorRepository.findById(scenarioCreateDto.getAuthorId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Author not found with id: " + scenarioCreateDto.getAuthorId()
                ));
        final Category category = categoryRepository.findById(scenarioCreateDto.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Category not found with id: " + scenarioCreateDto.getCategoryId()
                ));
        final Reader reader = readerRepository.findById(scenarioCreateDto.getReaderId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Reader not found with id: " + scenarioCreateDto.getReaderId()
                ));

        Book book = new Book();
        book.setTitle(scenarioCreateDto.getBookTitle());
        book.setIsbn(scenarioCreateDto.getBookIsbn());
        book.setDescription("Scenario generated book");
        book.setPublishYear(LocalDate.now().getYear());
        book.setPublisher(savedPublisher);
        book.setAuthors(new HashSet<>());
        book.getAuthors().add(author);
        book.setCategories(new HashSet<>());
        book.getCategories().add(category);

        Book savedBook = bookRepository.save(book);

        Loan loan = new Loan();
        loan.setBook(savedBook);
        loan.setReader(reader);
        loan.setLoanDate(LocalDate.now());
        loan.setDueDate(scenarioCreateDto.getDueDate());
        loan.setReturned(false);

        loanRepository.save(loan);
    }
}
