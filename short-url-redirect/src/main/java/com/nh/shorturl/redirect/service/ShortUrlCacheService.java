package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

/**
 * admin 서버가 PUT/DELETE /api/internal/cache/short-urls 로 캐시를 동기화할 때 사용한다.
 */
@Service
@RequiredArgsConstructor
public class ShortUrlCacheService {

    private final CacheManager cacheManager;

    @CachePut(value = "shortUrl", key = "#response.shortKey")
    public ShortUrlResponse put(ShortUrlResponse response) {
        Cache cache = cacheManager.getCache("shortUrl");
        if (cache != null && response != null) {
            cache.put(response.getShortKey(), response);
        }
        return response;
    }

    @CacheEvict(value = "shortUrl", key = "#shortKey")
    public void evict(String shortKey) {
        // annotation drives eviction
    }

    public ShortUrlResponse get(String shortKey) {
        Cache cache = cacheManager.getCache("shortUrl");
        if (cache == null) return null;
        Cache.ValueWrapper wrapper = cache.get(shortKey);
        return wrapper != null ? (ShortUrlResponse) wrapper.get() : null;
    }
}
