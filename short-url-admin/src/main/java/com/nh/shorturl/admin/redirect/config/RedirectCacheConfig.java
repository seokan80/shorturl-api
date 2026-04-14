package com.nh.shorturl.admin.redirect.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nh.shorturl.admin.redirect.service.ShortUrlLookupLoader;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;

/**
 * 리다이렉트 전용 Caffeine 캐시 구성.
 *
 * - shortUrl (positive): LoadingCache, expireAfterWrite + refreshAfterWrite 로
 *   비동기 DB 재로드 수행. 로더는 존재하지 않거나 만료된 키에 대해
 *   {@link ShortUrlLookupLoader.ShortUrlNotFoundException} 을 던진다.
 * - shortUrlMissing (negative): 404 응답을 짧은 TTL 로 캐시해 DB 폭주를 차단.
 *
 * Spring CacheManager 를 사용하지 않고 직접 주입받아 사용한다.
 */
@Configuration
@EnableAsync
public class RedirectCacheConfig {

    @Value("${short-url.cache.short-url.maximum-size:10000}")
    private long shortUrlMaxSize;

    @Value("${short-url.cache.short-url.expire-after-write-seconds:300}")
    private long shortUrlExpireSeconds;

    @Value("${short-url.cache.short-url.refresh-after-write-seconds:60}")
    private long shortUrlRefreshSeconds;

    @Value("${short-url.cache.missing.maximum-size:5000}")
    private long missingMaxSize;

    @Value("${short-url.cache.missing.expire-after-write-seconds:30}")
    private long missingExpireSeconds;

    @Bean
    public LoadingCache<String, ShortUrlResponse> shortUrlCache(
            ObjectProvider<ShortUrlLookupLoader> loaderProvider) {
        return Caffeine.newBuilder()
                .maximumSize(shortUrlMaxSize)
                .expireAfterWrite(Duration.ofSeconds(shortUrlExpireSeconds))
                .refreshAfterWrite(Duration.ofSeconds(shortUrlRefreshSeconds))
                .recordStats()
                .build(key -> loaderProvider.getObject().load(key));
    }

    @Bean
    public Cache<String, Boolean> shortUrlMissingCache() {
        return Caffeine.newBuilder()
                .maximumSize(missingMaxSize)
                .expireAfterWrite(Duration.ofSeconds(missingExpireSeconds))
                .recordStats()
                .build();
    }
}
