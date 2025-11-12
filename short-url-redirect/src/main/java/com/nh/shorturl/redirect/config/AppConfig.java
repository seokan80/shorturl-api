package com.nh.shorturl.redirect.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Configuration
@EnableCaching
@EnableAsync
public class AppConfig {

    @Value("${short-url.admin.api.base-url}")
    private String adminApiBaseUrl;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(adminApiBaseUrl)
                .build();
    }

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("shortUrl");
        cacheManager.setAllowNullValues(false); // Null 값 캐싱 비허용
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10000)); // 캐시 최대 크기
        return cacheManager;
    }
}