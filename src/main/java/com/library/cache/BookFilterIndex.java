package com.library.cache;

import com.library.dto.BookPageDto;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BookFilterIndex {

    private final Map<BookFilterCacheKey, BookPageDto> index = new HashMap<>();

    public synchronized Optional<BookPageDto> get(BookFilterCacheKey key) {
        BookPageDto value = index.get(key);
        if (value == null) {
            log.debug("Book filter cache miss: {}", key);
        } else {
            log.debug("Book filter cache hit: {}", key);
        }
        return Optional.ofNullable(value);
    }

    public synchronized void put(BookFilterCacheKey key, BookPageDto value) {
        index.put(key, value);
        log.debug("Book filter cache store: key={}, indexSize={}", key, index.size());
    }

    public synchronized void invalidateAll() {
        int removedEntries = index.size();
        index.clear();
        log.debug("Book filter cache invalidated: removedEntries={}", removedEntries);
    }
}
