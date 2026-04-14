package com.nh.shorturl.admin.redirect.service;

import com.nh.shorturl.admin.service.history.RedirectionHistoryService;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.type.BotType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 리다이렉트 시 비동기로 히스토리를 기록한다.
 * 단일 애플리케이션 내에서 {@link RedirectionHistoryService} 를 직접 호출한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectionHistoryAsyncWriter {

    private final RedirectionHistoryService redirectionHistoryService;

    @Async
    public void write(String shortUrlKey,
                      HttpServletRequest request,
                      BotType botType,
                      String botServiceKey,
                      String surveyId,
                      String surveyVer) {
        try {
            RedirectionHistoryRequest payload = RedirectionHistoryRequest.builder()
                    .shortUrlKey(shortUrlKey)
                    .referer(request.getHeader("Referer"))
                    .userAgent(request.getHeader("User-Agent"))
                    .ip(request.getRemoteAddr())
                    .botType(botType)
                    .botServiceKey(botServiceKey)
                    .surveyId(surveyId)
                    .surveyVer(surveyVer)
                    .build();
            redirectionHistoryService.saveRedirectionHistory(payload);
        } catch (Exception e) {
            log.error("Failed to save redirection history for {}", shortUrlKey, e);
        }
    }
}
