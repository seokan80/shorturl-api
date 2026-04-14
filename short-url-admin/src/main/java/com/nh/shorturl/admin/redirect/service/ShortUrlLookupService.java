package com.nh.shorturl.admin.redirect.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletionException;

/**
 * 리다이렉트 요청 경로에서 호출되는 조회 진입점.
 *
 * 1) negative cache 적중 → 즉시 null 반환 (404 처리)
 * 2) positive cache (LoadingCache) → 존재 시 값 반환, refreshAfterWrite 경과 시 비동기 재로드
 * 3) 로더가 sentinel 예외를 던지면 null 로 정규화하여 상위 컨트롤러에 전달
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlLookupService {

    private final LoadingCache<String, ShortUrlResponse> shortUrlCache;
    private final Cache<String, Boolean> shortUrlMissingCache;

    public ShortUrlResponse findByKey(String shortUrlKey) {
        if (shortUrlMissingCache.getIfPresent(shortUrlKey) != null) {
            log.debug("Negative cache hit: {}", shortUrlKey);
            return null;
        }
        try {
            return shortUrlCache.get(shortUrlKey);
        } catch (ShortUrlLookupLoader.ShortUrlNotFoundException e) {
            return null;
        } catch (CompletionException e) {
            if (e.getCause() instanceof ShortUrlLookupLoader.ShortUrlNotFoundException) {
                return null;
            }
            throw e;
        }
    }
}
