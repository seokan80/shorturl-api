package com.nh.shorturl.redirect.controller;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.redirect.service.RedirectionHistoryService;
import com.nh.shorturl.redirect.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlRedirectController {

    private final ShortUrlService shortUrlService;
    private final RedirectionHistoryService redirectionHistoryService;

    @GetMapping("/{shortUrl}")
    public void redirectToOriginal(
            @PathVariable String shortUrl,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        try {
            ShortUrlResponse shortUrlResponse = shortUrlService.getShortUrlByKey(shortUrl);
            if (shortUrlResponse == null || shortUrlResponse.getLongUrl() == null) {
                throw new IllegalStateException("URL not found");
            }

            // 통계 저장을 비동기로 요청 (Fire-and-Forget)
            redirectionHistoryService.saveRedirectionHistory(shortUrl, request);

            response.sendRedirect(shortUrlResponse.getLongUrl());

        } catch (Exception e) {
            log.error("Redirection failed for shortUrl: {}. Reason: {}", shortUrl, e.getMessage());
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "The requested URL was not found.");
        }
    }
}