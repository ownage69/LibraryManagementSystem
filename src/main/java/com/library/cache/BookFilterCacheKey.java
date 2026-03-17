package com.library.cache;

import java.util.Objects;

public final class BookFilterCacheKey {

    private final String queryType;
    private final String authorLastName;
    private final String categoryName;
    private final String publisherCountry;
    private final int page;
    private final int size;

    public BookFilterCacheKey(
            String queryType,
            String authorLastName,
            String categoryName,
            String publisherCountry,
            int page,
            int size
    ) {
        this.queryType = normalize(queryType);
        this.authorLastName = normalize(authorLastName);
        this.categoryName = normalize(categoryName);
        this.publisherCountry = normalize(publisherCountry);
        this.page = page;
        this.size = size;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BookFilterCacheKey other)) {
            return false;
        }
        return page == other.page
                && size == other.size
                && Objects.equals(queryType, other.queryType)
                && Objects.equals(authorLastName, other.authorLastName)
                && Objects.equals(categoryName, other.categoryName)
                && Objects.equals(publisherCountry, other.publisherCountry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                queryType,
                authorLastName,
                categoryName,
                publisherCountry,
                page,
                size
        );
    }
}
