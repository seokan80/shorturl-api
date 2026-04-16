package com.nh.shorturl.redirect.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 단축 URL Caffeine 캐시에 대한 단순 래퍼.
 * per-entry expiry 는 {@code AppConfig} 에서 설정된 Expiry 가 자동 처리한다.
 */
@Service
@RequiredArgsConstructor
public class ShortUrlCacheService {

    private final Cache<String, ShortUrlResponse> shortUrlCache;

    public void put(ShortUrlResponse response) {
        if (response != null && response.getShortKey() != null) {
            shortUrlCache.put(response.getShortKey(), response);
        }
    }

    public void evict(String shortKey) {
        shortUrlCache.invalidate(shortKey);
    }

    public ShortUrlResponse get(String shortKey) {
        return shortUrlCache.getIfPresent(shortKey);
    }

    public long size() {
        return shortUrlCache.estimatedSize();
    }
}
