package com.nh.shorturl.redirect.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Configuration
public class AppConfig {

    @Value("${short-url.admin.api.base-url}")
    private String adminApiBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(adminApiBaseUrl)
                .build();
    }

    /**
     * 단축 URL 캐시.
     *
     * <p>항목별 만료(per-entry expiry): 각 ShortUrlResponse 의 expiredAt 시각까지 유효하며,
     * 시각이 도달하면 Caffeine 이 자동 제거한다. 2중화 환경에서 서버 간 통보 없이
     * 양쪽이 동일 시점에 독립적으로 만료된다.
     *
     * <p>expiredAt 이 null 인 경우 24시간 기본 TTL 을 적용한다.
     */
    @Bean
    public Cache<String, ShortUrlResponse> shortUrlCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfter(new Expiry<String, ShortUrlResponse>() {
                    @Override
                    public long expireAfterCreate(String key, ShortUrlResponse value, long currentTime) {
                        return ttlNanos(value);
                    }

                    @Override
                    public long expireAfterUpdate(String key, ShortUrlResponse value,
                                                  long currentTime, @NonNegative long currentDuration) {
                        return ttlNanos(value);
                    }

                    @Override
                    public long expireAfterRead(String key, ShortUrlResponse value,
                                                long currentTime, @NonNegative long currentDuration) {
                        return currentDuration; // 읽기로 TTL 변경 없음
                    }
                })
                .build();
    }

    private static long ttlNanos(ShortUrlResponse value) {
        if (value.getExpiredAt() == null) {
            return TimeUnit.HOURS.toNanos(24);
        }
        try {
            LocalDateTime expiry = LocalDateTime.parse(value.getExpiredAt());
            Duration remaining = Duration.between(LocalDateTime.now(), expiry);
            return remaining.isNegative() ? 0 : remaining.toNanos();
        } catch (Exception e) {
            return TimeUnit.HOURS.toNanos(24);
        }
    }
}
