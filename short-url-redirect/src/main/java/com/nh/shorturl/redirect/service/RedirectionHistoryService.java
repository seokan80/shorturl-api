package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 리다이렉트 이력을 admin 서버에 비동기로 전송한다.
 * redirect 서버는 DB 에 직접 접근하지 않는다.
 *
 * <p>요청 헤더/IP 는 호출 스레드(서블릿 스레드)에서 추출해 값으로 전달받는다.
 * WebClient 의 리액티브 {@code subscribe()} 가 이미 논블로킹이므로 {@code @Async} 를 쓰지 않는다.
 * (HttpServletRequest 를 비동기 스레드에서 읽으면 요청 재활용으로 손상될 수 있다.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectionHistoryService {

    private final WebClient webClient;

    @Value("${short-url.admin.api.uri.history}")
    private String uriHistory;

    public void save(String shortKey, String referer, String userAgent, String ip) {
        RedirectionHistoryRequest payload = RedirectionHistoryRequest.builder()
                .shortUrlKey(shortKey)
                .referer(referer)
                .userAgent(userAgent)
                .ip(ip)
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
