package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioCreateDto {

    @NotBlank(message = "Publisher name is required")
    private String publisherName;

    @NotBlank(message = "Book title is required")
    private String bookTitle;

    @NotBlank(message = "Book isbn is required")
    private String bookIsbn;

    @NotNull(message = "Author id is required")
    private Long authorId;

    @NotNull(message = "Category id is required")
    private Long categoryId;

    @NotNull(message = "Reader id is required")
    private Long readerId;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
}
