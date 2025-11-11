package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ShortUrlServiceImpl implements ShortUrlService {

    private final WebClient webClient;

    @Override
    @Cacheable(value = "shortUrl", key = "#shortUrlKey")
    public ShortUrlResponse getShortUrlByKey(String shortUrlKey) {
        return webClient.get()
                .uri("/api/internal/short-urls/{shortUrlKey}", shortUrlKey)
                .retrieve()
                .bodyToMono(ShortUrlResponse.class)
                .onErrorResume(e -> Mono.empty()) // API 오류 시 null 반환
                .block();
    }
}
