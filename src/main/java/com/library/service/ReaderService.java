package com.library.service;

import com.library.dto.ReaderCreateDto;
import com.library.dto.ReaderDto;
import com.library.entity.Reader;
import com.library.repository.ReaderRepository;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReaderService {

    private final ReaderRepository readerRepository;

    public List<ReaderDto> findAll() {
        return readerRepository.findAll()
                .stream()
                .map(reader -> new ReaderDto(
                        reader.getId(),
                        reader.getFirstName(),
                        reader.getLastName(),
                        reader.getEmail()
                ))
                .toList();
    }

    public ReaderDto findById(Long id) {
        Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reader not found with id: " + id));
        return new ReaderDto(
                reader.getId(),
                reader.getFirstName(),
                reader.getLastName(),
                reader.getEmail()
        );
    }

    public ReaderDto create(ReaderCreateDto readerCreateDto) {
        Reader reader = new Reader();
        reader.setFirstName(readerCreateDto.getFirstName());
        reader.setLastName(readerCreateDto.getLastName());
        reader.setEmail(readerCreateDto.getEmail());
        Reader saved = readerRepository.save(reader);
        return new ReaderDto(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail()
        );
    }

    public ReaderDto update(Long id, ReaderCreateDto readerCreateDto) {
        Reader reader = readerRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reader not found with id: " + id));
        reader.setFirstName(readerCreateDto.getFirstName());
        reader.setLastName(readerCreateDto.getLastName());
        reader.setEmail(readerCreateDto.getEmail());
        Reader saved = readerRepository.save(reader);
        return new ReaderDto(
                saved.getId(),
                saved.getFirstName(),
                saved.getLastName(),
                saved.getEmail()
        );
    }

    public void delete(Long id) {
        if (!readerRepository.existsById(id)) {
            throw new NoSuchElementException("Reader not found with id: " + id);
        }
        readerRepository.deleteById(id);
    }
}
