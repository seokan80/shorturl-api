package com.nh.shorturl.redirect.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableCaching
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
     * 단축 URL 캐시. 기동 시 캐시 워머가 admin 의 내부 API 로 전체 목록을 로드한다.
     * admin 이 PUT/DELETE /api/internal/cache/short-urls 를 호출해 캐시를 동기화한다.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("shortUrl");
        cacheManager.setAllowNullValues(false);
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(10000));
        return cacheManager;
    }
}
