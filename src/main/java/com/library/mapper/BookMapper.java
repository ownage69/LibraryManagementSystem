package com.library.mapper;

import com.library.dto.BookCreateDto;
import com.library.dto.BookDto;
import com.library.entity.Book;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public BookDto toDto(Book book) {
        if (book == null) {
            return null;
        }

        Set<Long> authorIds = book.getAuthors()
                .stream()
                .map(author -> author.getId())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        Set<String> authorNames = book.getAuthors()
                .stream()
                .map(author -> author.getFirstName() + " " + author.getLastName())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        Set<Long> categoryIds = book.getCategories()
                .stream()
                .map(category -> category.getId())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        Set<String> categoryNames = book.getCategories()
                .stream()
                .map(category -> category.getName())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        return new BookDto(
                book.getId(),
                book.getTitle(),
                book.getIsbn(),
                book.getDescription(),
                book.getPublishYear(),
                book.getPublisher().getId(),
                book.getPublisher().getName(),
                authorIds,
                authorNames,
                categoryIds,
                categoryNames
        );
    }

    public Book toEntity(BookCreateDto bookCreateDto) {
        if (bookCreateDto == null) {
            return null;
        }

        Book book = new Book();
        book.setTitle(bookCreateDto.getTitle());
        book.setIsbn(bookCreateDto.getIsbn());
        book.setDescription(bookCreateDto.getDescription());
        book.setPublishYear(bookCreateDto.getPublishYear());
        return book;
    }

    public void updateEntityFromDto(BookCreateDto bookCreateDto, Book book) {
        if (bookCreateDto == null || book == null) {
            return;
        }

        book.setTitle(bookCreateDto.getTitle());
        book.setIsbn(bookCreateDto.getIsbn());
        book.setDescription(bookCreateDto.getDescription());
        book.setPublishYear(bookCreateDto.getPublishYear());
    }
}
