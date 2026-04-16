package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 리다이렉트 이력을 admin 서버에 비동기로 전송한다.
 * redirect 서버는 DB 에 직접 접근하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectionHistoryService {

    private final WebClient webClient;

    @Value("${short-url.admin.api.uri.history}")
    private String uriHistory;

    @Async
    public void save(String shortKey, HttpServletRequest request) {
        RedirectionHistoryRequest payload = RedirectionHistoryRequest.builder()
                .shortUrlKey(shortKey)
                .referer(request.getHeader("Referer"))
                .userAgent(request.getHeader("User-Agent"))
                .ip(request.getRemoteAddr())
                .build();

        webClient.post()
                .uri(uriHistory)
                .body(Mono.just(payload), RedirectionHistoryRequest.class)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(r -> log.debug("[history] saved for key={}", shortKey))
                .doOnError(e -> log.error("[history] failed for key={}", shortKey, e))
                .subscribe();
    }
}
