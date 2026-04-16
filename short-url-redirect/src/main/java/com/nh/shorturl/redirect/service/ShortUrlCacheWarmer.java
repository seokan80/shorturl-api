package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * 기동 시 admin 의 /api/internal/short-urls/all 로 전체 단축 URL 을 Caffeine 캐시에 로드한다.
 * admin 서버가 준비되지 않은 상태면 경고 로그만 남기고 기동을 중단하지 않는다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShortUrlCacheWarmer implements ApplicationRunner {

    private final WebClient webClient;
    private final CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[cache-warmer] Starting Short URL cache warming...");
        try {
            List<ShortUrlResponse> all = webClient.get()
                    .uri("/api/internal/short-urls/all")
                    .retrieve()
                    .bodyToFlux(ShortUrlResponse.class)
                    .collectList()
                    .block();

            Cache cache = cacheManager.getCache("shortUrl");
            if (cache != null && all != null) {
                all.forEach(item -> cache.put(item.getShortKey(), item));
                log.info("[cache-warmer] Completed. {} items cached.", all.size());
            }
        } catch (Exception e) {
            log.warn("[cache-warmer] Failed — redirect will serve only requests that hit admin API. Cause: {}", e.getMessage());
        }
    }
}
