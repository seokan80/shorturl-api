package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectionConfigStore {

    private final WebClient webClient;
    private final AtomicReference<RedirectionConfigResponse> configContent = new AtomicReference<>();

    @PostConstruct
    public void init() {
        refreshConfig();
    }

    @Scheduled(fixedRate = 60000) // 1분마다 갱신
    public void refreshConfig() {
        try {
            RedirectionConfigResponse response = webClient.get()
                    .uri("/api/internal/redirection-config")
                    .retrieve()
                    .bodyToMono(RedirectionConfigResponse.class)
                    .block();
            if (response != null) {
                configContent.set(response);
                log.debug("Redirection config refreshed: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to refresh redirection config", e);
        }
    }

    public RedirectionConfigResponse getConfig() {
        return configContent.get();
    }
}
