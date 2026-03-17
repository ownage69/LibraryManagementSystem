package com.library.service;

import com.library.dto.PublisherCreateDto;
import com.library.dto.PublisherDto;
import com.library.entity.Publisher;
import com.library.repository.PublisherRepository;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublisherService {

    private final PublisherRepository publisherRepository;
    private final BookService bookService;

    public List<PublisherDto> findAll() {
        return publisherRepository.findAll()
                .stream()
                .map(publisher -> new PublisherDto(
                        publisher.getId(),
                        publisher.getName(),
                        publisher.getCountry()
                ))
                .toList();
    }

    public PublisherDto findById(Long id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Publisher not found with id: " + id
                ));
        return new PublisherDto(publisher.getId(), publisher.getName(), publisher.getCountry());
    }

    public PublisherDto create(PublisherCreateDto publisherCreateDto) {
        Publisher publisher = new Publisher();
        publisher.setName(publisherCreateDto.getName());
        publisher.setCountry(publisherCreateDto.getCountry());
        Publisher saved = publisherRepository.save(publisher);
        bookService.invalidateFilterCache();
        return new PublisherDto(saved.getId(), saved.getName(), saved.getCountry());
    }

    public PublisherDto update(Long id, PublisherCreateDto publisherCreateDto) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Publisher not found with id: " + id
                ));
        publisher.setName(publisherCreateDto.getName());
        publisher.setCountry(publisherCreateDto.getCountry());
        Publisher saved = publisherRepository.save(publisher);
        bookService.invalidateFilterCache();
        return new PublisherDto(saved.getId(), saved.getName(), saved.getCountry());
    }

    public void delete(Long id) {
        if (!publisherRepository.existsById(id)) {
            throw new NoSuchElementException("Publisher not found with id: " + id);
        }
        publisherRepository.deleteById(id);
        bookService.invalidateFilterCache();
    }
}
