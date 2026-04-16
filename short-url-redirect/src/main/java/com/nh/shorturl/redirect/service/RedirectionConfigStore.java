package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicReference;

/**
 * admin 서버의 /api/internal/redirection-config 를 1분마다 폴링해 설정을 최신 상태로 유지한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectionConfigStore {

    private final WebClient webClient;
    private final AtomicReference<RedirectionConfigResponse> config = new AtomicReference<>();

    @PostConstruct
    public void init() {
        refreshConfig();
    }

    @Scheduled(fixedRate = 60_000)
    public void refreshConfig() {
        try {
            RedirectionConfigResponse response = webClient.get()
                    .uri("/api/internal/redirection-config")
                    .retrieve()
                    .bodyToMono(RedirectionConfigResponse.class)
                    .block();
            if (response != null) {
                config.set(response);
                log.debug("[config] refreshed: fallback={}", response.getFallbackUrl());
            }
        } catch (Exception e) {
            log.error("[config] refresh failed — using previous value", e);
        }
    }

    public RedirectionConfigResponse getConfig() {
        return config.get();
    }
}
