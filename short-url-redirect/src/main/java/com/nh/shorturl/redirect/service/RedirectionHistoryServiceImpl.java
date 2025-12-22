package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.type.BotType;
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
    public void saveRedirectionHistory(String shortUrlKey, HttpServletRequest request, BotType botType,
            String botServiceKey, String surveyId, String surveyVer) {
        RedirectionHistoryRequest historyRequest = RedirectionHistoryRequest.builder()
                .shortUrlKey(shortUrlKey)
                .referer(request.getHeader("Referer"))
                .userAgent(request.getHeader("User-Agent"))
                .ip(request.getRemoteAddr())
                .botType(botType)
                .botServiceKey(botServiceKey)
                .surveyId(surveyId)
                .surveyVer(surveyVer)
                .build();

        webClient.post()
                .uri("/api/redirections/history/internal")
                .body(Mono.just(historyRequest), RedirectionHistoryRequest.class)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("Successfully sent redirection history for {}", shortUrlKey))
                .doOnError(error -> log.error("Failed to send redirection history for {}", shortUrlKey, error))
                .subscribe();
    }
}
