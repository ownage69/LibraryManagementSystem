package com.library.service;

import com.library.dto.CategoryCreateDto;
import com.library.dto.CategoryDto;
import com.library.entity.Category;
import com.library.repository.CategoryRepository;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BookService bookService;

    public List<CategoryDto> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(category -> new CategoryDto(category.getId(), category.getName()))
                .toList();
    }

    public CategoryDto findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found with id: " + id));
        return new CategoryDto(category.getId(), category.getName());
    }

    public CategoryDto create(CategoryCreateDto categoryCreateDto) {
        Category category = new Category();
        category.setName(categoryCreateDto.getName());
        Category saved = categoryRepository.save(category);
        bookService.invalidateFilterCache();
        return new CategoryDto(saved.getId(), saved.getName());
    }

    public CategoryDto update(Long id, CategoryCreateDto categoryCreateDto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found with id: " + id));
        category.setName(categoryCreateDto.getName());
        Category saved = categoryRepository.save(category);
        bookService.invalidateFilterCache();
        return new CategoryDto(saved.getId(), saved.getName());
    }

    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NoSuchElementException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
        bookService.invalidateFilterCache();
    }
}
