package com.nh.shorturl.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${short-url.redirect.api.base-url}")
    private String redirectApiBaseUrl;

    @Bean
    public WebClient redirectApiClient() {
        return WebClient.builder()
                .baseUrl(redirectApiBaseUrl)
                .build();
    }
}
