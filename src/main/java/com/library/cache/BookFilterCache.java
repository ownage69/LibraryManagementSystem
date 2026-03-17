package com.library.cache;

import com.library.dto.BookPageDto;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BookFilterCache {

    private final Map<BookFilterCacheKey, BookPageDto> cache =
            Collections.synchronizedMap(new HashMap<>());

    public BookPageDto get(BookFilterCacheKey key) {
        return cache.get(key);
    }

    public void put(BookFilterCacheKey key, BookPageDto value) {
        cache.put(key, value);
    }

    public void clear() {
        cache.clear();
    }
}
