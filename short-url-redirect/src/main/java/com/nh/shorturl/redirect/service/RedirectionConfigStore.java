package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicReference;

/**
 * admin 서버의 리다이렉트 설정을 주기적으로 폴링해 최신 상태로 유지한다.
 * 폴링 주기: application.yml 의 short-url.sync.config-interval-ms
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectionConfigStore {

    private final WebClient webClient;
    private final AtomicReference<RedirectionConfigResponse> config = new AtomicReference<>();

    @Value("${short-url.admin.api.uri.config}")
    private String uriConfig;

    @PostConstruct
    public void init() {
        refreshConfig();
    }

    @Scheduled(fixedRateString = "${short-url.sync.config-interval-ms:60000}")
    public void refreshConfig() {
        try {
            ResultEntity<RedirectionConfigResponse> result = webClient.get()
                    .uri(uriConfig)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ResultEntity<RedirectionConfigResponse>>() {})
                    .block();
            if (result != null && result.getData() != null) {
                config.set(result.getData());
                log.debug("[config] refreshed: fallback={}, defaultHost={}",
                        result.getData().getFallbackUrl(), result.getData().getDefaultHost());
            }
        } catch (Exception e) {
            log.error("[config] refresh failed — using previous value", e);
        }
    }

    public RedirectionConfigResponse getConfig() {
        return config.get();
    }
}
