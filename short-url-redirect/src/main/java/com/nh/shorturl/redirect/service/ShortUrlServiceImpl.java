package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlServiceImpl implements ShortUrlService {

    private final CacheManager cacheManager;

    @Override
    public ShortUrlResponse getShortUrlByKey(String shortUrlKey) {
        Cache cache = cacheManager.getCache("shortUrl");
        if (cache != null) {
            Cache.ValueWrapper valueWrapper = cache.get(shortUrlKey);
            if (valueWrapper != null) {
                log.debug("Cache hit for key: {}", shortUrlKey);
                return (ShortUrlResponse) valueWrapper.get();
            }
        }
        // 캐시 초기화 및 동기화 아키텍처에 따라, 캐시에 없으면 유효하지 않은 URL로 간주합니다.
        log.warn("Invalid Key : {}", shortUrlKey);
        return null;
    }
}