// 이 파일은 이전 답변의 내용과 거의 동일합니다.
// (WebClient를 사용해 비동기로 통계 저장 API를 호출하는 구현체)
package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectionHistoryServiceImpl implements RedirectionHistoryService {

    private final WebClient webClient;

    @Async
    @Override
    public void saveRedirectionHistory(String shortUrlKey, HttpServletRequest request) {
        RedirectionHistoryRequest historyRequest = RedirectionHistoryRequest.builder()
                .shortUrlKey(shortUrlKey)
                .referer(request.getHeader("Referer"))
                .userAgent(request.getHeader("User-Agent"))
                .ip(request.getRemoteAddr())
                .build();

        webClient.post()
                .uri("/api/internal/redirection-histories")
                .body(Mono.just(historyRequest), RedirectionHistoryRequest.class)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("Successfully sent redirection history for {}", shortUrlKey))
                .doOnError(error -> log.error("Failed to send redirection history for {}", shortUrlKey, error))
                .subscribe();
    }
}
