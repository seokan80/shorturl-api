package com.nh.shorturl.redirect.controller;

import com.nh.shorturl.redirect.service.RedirectionConfigStore;
import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
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
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlRedirectController {

    private final ShortUrlService shortUrlService;
    private final RedirectionHistoryService redirectionHistoryService;
    private final RedirectionConfigStore configStore;

    @GetMapping("/")
    public void rootRedirect(HttpServletResponse response) throws IOException {
        RedirectionConfigResponse config = configStore.getConfig();
        if (config != null && config.getDefaultHost() != null && !config.getDefaultHost().isBlank()) {
            response.sendRedirect(config.getDefaultHost());
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping("/{shortUrl}")
    public void redirectToOriginal(
            @PathVariable String shortUrl,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        RedirectionConfigResponse config = configStore.getConfig();
        try {
            ShortUrlResponse shortUrlResponse = shortUrlService.getShortUrlByKey(shortUrl);
            if (shortUrlResponse == null || shortUrlResponse.getLongUrl() == null) {
                handleFailure(response, config, "URL not found or expired");
                return;
            }

            // 통계 저장을 비동기로 요청 (Fire-and-Forget)
            redirectionHistoryService.saveRedirectionHistory(shortUrl, request);

            // 추적 필드 추가
            String targetUrl = appendTrackingFields(shortUrlResponse.getLongUrl(), request, config);

            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            log.error("Redirection failed for shortUrl: {}. Reason: {}", shortUrl, e.getMessage());
            handleFailure(response, config, e.getMessage());
        }
    }

    private String appendTrackingFields(String longUrl, HttpServletRequest request, RedirectionConfigResponse config) {
        if (config == null || config.getTrackingFields() == null || config.getTrackingFields().isBlank()) {
            return longUrl;
        }

        String[] fields = config.getTrackingFields().split(",");
        StringBuilder sb = new StringBuilder(longUrl);
        boolean first = !longUrl.contains("?");

        for (String field : fields) {
            String value = request.getParameter(field.trim());
            if (value != null && !value.isBlank()) {
                sb.append(first ? "?" : "&").append(field.trim()).append("=").append(value);
                first = false;
            }
        }
        return sb.toString();
    }

    private void handleFailure(HttpServletResponse response, RedirectionConfigResponse config, String reason)
            throws IOException {
        if (config != null && config.getFallbackUrl() != null && !config.getFallbackUrl().isBlank()) {
            response.sendRedirect(config.getFallbackUrl());
            return;
        }

        if (config != null && Boolean.TRUE.equals(config.getShowErrorPage())) {
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().write("<!DOCTYPE html><html><head><title>Redirection Error</title>" +
                    "<style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;background:#f8fafc;color:#1e293b;}"
                    +
                    ".card{background:white;padding:2rem;border-radius:1rem;box-shadow:0 10px 15px -3px rgba(0,0,0,0.1);max-width:400px;text-align:center;}"
                    +
                    "h1{color:#e11d48;margin-top:0;}p{color:#64748b;line-height:1.6;}</style></head>" +
                    "<body><div class='card'><h1>이동 실패</h1><p>" + reason + "</p>" +
                    "<p>요청하신 URL이 존재하지 않거나 만료되었습니다.</p></div></body></html>");
            return;
        }

        response.sendError(HttpServletResponse.SC_NOT_FOUND, reason);
    }
}