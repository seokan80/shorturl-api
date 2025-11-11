// 이 파일은 이전 답변의 내용과 동일합니다.
// (WebClient 빈, @EnableCaching, @EnableAsync 포함)
package com.nh.shorturl.redirect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;

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
}
