package com.nh.shorturl.redirect.controller;

import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.redirect.service.RedirectionConfigStore;
import com.nh.shorturl.redirect.service.RedirectionHistoryService;
import com.nh.shorturl.redirect.service.ShortUrlCacheService;
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

    private final ShortUrlCacheService cacheService;
    private final RedirectionHistoryService historyService;
    private final RedirectionConfigStore configStore;

    /**
     * 배포 후 스모크 테스트용 고정 키.
     * DB·캐시 무관하게 동작 — 서버 기동 직후에도 즉시 사용 가능.
     * GET /verify  →  redirection-config 의 default-host 로 302
     */
    @GetMapping("/verify")
    public void verify(HttpServletResponse response) throws IOException {
        RedirectionConfigResponse config = configStore.getConfig();
        String target = config != null ? config.getDefaultHost() : null;
        if (target == null || target.isBlank()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "short-url.redirection.default-host 설정이 비어 있습니다.");
            return;
        }
        log.info("[smoke-test] /verify → {}", target);
        response.sendRedirect(target);
    }

    @GetMapping("/{shortKey}")
    public void redirect(@PathVariable String shortKey,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        RedirectionConfigResponse config = configStore.getConfig();
        try {
            ShortUrlResponse found = cacheService.get(shortKey);
            if (found == null || found.getLongUrl() == null) {
                log.warn("[redirect] cache miss for key={}", shortKey);
                handleFailure(response, config, "URL not found or expired");
                return;
            }

            historyService.save(shortKey, request);

            String targetUrl = appendTrackingFields(found.getLongUrl(), request, config);
            response.sendRedirect(targetUrl);

        } catch (Exception e) {
            log.error("[redirect] failed for key={}: {}", shortKey, e.getMessage());
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
            response.getWriter().write(buildErrorHtml(reason));
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND, reason);
    }

    private String buildErrorHtml(String reason) {
        return "<!DOCTYPE html><html><head><title>이동 실패</title>" +
                "<style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;" +
                "height:100vh;margin:0;background:#f8fafc;}" +
                ".card{background:white;padding:2rem;border-radius:1rem;" +
                "box-shadow:0 10px 15px -3px rgba(0,0,0,.1);max-width:400px;text-align:center;}" +
                "h1{color:#e11d48;}p{color:#64748b;line-height:1.6;}</style></head>" +
                "<body><div class='card'><h1>이동 실패</h1>" +
                "<p>요청하신 URL이 존재하지 않거나 만료되었습니다.</p>" +
                "<p><small>" + reason + "</small></p></div></body></html>";
    }
}
