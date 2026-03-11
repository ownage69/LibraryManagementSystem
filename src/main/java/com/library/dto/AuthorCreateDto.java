package com.library.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorCreateDto {

    @NotBlank(message = "Author first name is required")
    private String firstName;

    @NotBlank(message = "Author last name is required")
    private String lastName;
}
