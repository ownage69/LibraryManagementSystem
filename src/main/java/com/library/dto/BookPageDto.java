package com.library.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookPageDto {

    private List<BookDto> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean cached;
    private String queryType;
}
