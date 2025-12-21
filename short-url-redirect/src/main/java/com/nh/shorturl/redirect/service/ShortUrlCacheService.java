package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ShortUrlCacheService {

    private final CacheManager cacheManager;

    /**
     * shortUrl 캐시를 업데이트합니다. (key는 response 객체의 shortUrl 필드)
     * 이 메소드의 반환값이 캐시에 저장됩니다.
     */
    @CachePut(value = "shortUrl", key = "#response.shortUrl")
    public ShortUrlResponse updateShortUrlInCache(ShortUrlResponse response) {
        Cache cache = cacheManager.getCache("shortUrl");
        if (cache != null && response != null) {
            cache.put(response.getShortUrl(), response);
        }
        return response;
    }

    /**
     * shortUrl 캐시에서 항목을 제거.
     */
    @CacheEvict(value = "shortUrl", key = "#shortUrlKey")
    public void evictShortUrlFromCache(String shortUrlKey) {
        // 내용은 비워도 어노테이션이 동작합니다.
    }
}
