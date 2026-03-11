package com.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReaderCreateDto {

    @NotBlank(message = "Reader first name is required")
    private String firstName;

    @NotBlank(message = "Reader last name is required")
    private String lastName;

    @NotBlank(message = "Reader email is required")
    @Email(message = "Reader email must be valid")
    private String email;
}
