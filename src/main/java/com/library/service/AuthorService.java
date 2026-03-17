package com.library.service;

import com.library.dto.AuthorCreateDto;
import com.library.dto.AuthorDto;
import com.library.entity.Author;
import com.library.repository.AuthorRepository;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookService bookService;

    public List<AuthorDto> findAll() {
        return authorRepository.findAll()
                .stream()
                .map(author -> new AuthorDto(
                        author.getId(),
                        author.getFirstName(),
                        author.getLastName()
                ))
                .toList();
    }

    public AuthorDto findById(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Author not found with id: " + id));
        return new AuthorDto(author.getId(), author.getFirstName(), author.getLastName());
    }

    public AuthorDto create(AuthorCreateDto authorCreateDto) {
        Author author = new Author();
        author.setFirstName(authorCreateDto.getFirstName());
        author.setLastName(authorCreateDto.getLastName());
        Author saved = authorRepository.save(author);
        bookService.invalidateFilterCache();
        return new AuthorDto(saved.getId(), saved.getFirstName(), saved.getLastName());
    }

    public AuthorDto update(Long id, AuthorCreateDto authorCreateDto) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Author not found with id: " + id));
        author.setFirstName(authorCreateDto.getFirstName());
        author.setLastName(authorCreateDto.getLastName());
        Author saved = authorRepository.save(author);
        bookService.invalidateFilterCache();
        return new AuthorDto(saved.getId(), saved.getFirstName(), saved.getLastName());
    }

    public void delete(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new NoSuchElementException("Author not found with id: " + id);
        }
        authorRepository.deleteById(id);
        bookService.invalidateFilterCache();
    }
}
