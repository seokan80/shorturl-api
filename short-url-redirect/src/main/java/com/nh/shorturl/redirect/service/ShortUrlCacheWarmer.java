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

@Component
@RequiredArgsConstructor
@Slf4j
public class ShortUrlCacheWarmer implements ApplicationRunner {

    private final WebClient webClient;
    private final CacheManager cacheManager;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting Short URL cache warming...");
        try {
            List<ShortUrlResponse> allShortUrls = webClient.get()
                    .uri("/api/internal/short-urls/all")
                    .retrieve()
                    .bodyToFlux(ShortUrlResponse.class)
                    .collectList()
                    .block(); // 시작 시에는 동기적으로 처리

            Cache cache = cacheManager.getCache("shortUrl");
            if (cache != null && allShortUrls != null) {
                allShortUrls.forEach(shortUrl -> {
                    log.debug("Caching: key={}, value={}", shortUrl.getShortUrl(), shortUrl.getLongUrl());
                    cache.put(shortUrl.getShortUrl(), shortUrl);
                });
                log.info("Short URL cache warming completed. {} items cached.", allShortUrls.size());
            }
        } catch (Exception e) {
            log.error("Failed to warm up Short URL cache.", e);
        }
    }
}
