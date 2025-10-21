package com.nh.shorturl.controller;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.service.history.RedirectionHistoryService;
import com.nh.shorturl.service.shorturl.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/r")
public class ShortUrlRedirectController {

    private final ShortUrlService shortUrlService;

    private final RedirectionHistoryService redirectionHistoryService;

    public ShortUrlRedirectController(ShortUrlService shortUrlService, RedirectionHistoryService redirectionHistoryService) {
        this.shortUrlService = shortUrlService;
        this.redirectionHistoryService = redirectionHistoryService;
    }

    /**
     * 단축 URL 키로 요청이 들어왔을 때 원본 URL로 리디렉션
     * 요청 접수 시 통계 적재
     *
     * @param shortUrl 단축 URL 키
     * @param response HttpServletResponse
     */
    @GetMapping("/{shortUrl}")
    public void redirectToOriginal(
            @PathVariable String shortUrl,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        try {
            ShortUrlResponse shortUrlResponse = shortUrlService.getShortUrlByKey(shortUrl);

            response.sendRedirect(shortUrlResponse.getLongUrl());

            redirectionHistoryService.saveRedirectionHistory(shortUrl, request);
        } catch (Exception e) {
            response.sendRedirect("/error");
        }
    }
}