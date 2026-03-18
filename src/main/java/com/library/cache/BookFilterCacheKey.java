package com.library.cache;

import com.library.service.BookFilterQueryType;
import java.util.Objects;
import org.springframework.data.domain.Pageable;

public class BookFilterCacheKey {

    private final String authorLastName;
    private final String categoryName;
    private final String publisherCountry;
    private final int page;
    private final int size;
    private final String sort;
    private final BookFilterQueryType queryType;

    public BookFilterCacheKey(
            String authorLastName,
            String categoryName,
            String publisherCountry,
            Pageable pageable,
            BookFilterQueryType queryType
    ) {
        this.authorLastName = authorLastName;
        this.categoryName = categoryName;
        this.publisherCountry = publisherCountry;
        this.page = pageable.isPaged() ? pageable.getPageNumber() : -1;
        this.size = pageable.isPaged() ? pageable.getPageSize() : -1;
        this.sort = pageable.getSort().toString();
        this.queryType = queryType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        BookFilterCacheKey that = (BookFilterCacheKey) object;
        return page == that.page
                && size == that.size
                && Objects.equals(authorLastName, that.authorLastName)
                && Objects.equals(categoryName, that.categoryName)
                && Objects.equals(publisherCountry, that.publisherCountry)
                && Objects.equals(sort, that.sort)
                && queryType == that.queryType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                authorLastName,
                categoryName,
                publisherCountry,
                page,
                size,
                sort,
                queryType
        );
    }

    @Override
    public String toString() {
        return "queryType=" + queryType
                + ", authorLastName='" + authorLastName + '\''
                + ", categoryName='" + categoryName + '\''
                + ", publisherCountry='" + publisherCountry + '\''
                + ", page=" + page
                + ", size=" + size
                + ", sort=" + sort;
    }
}
