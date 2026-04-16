package com.nh.shorturl.redirect.controller;

import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.redirect.service.RedirectionConfigStore;
import com.nh.shorturl.redirect.service.RedirectionHistoryService;
import com.nh.shorturl.redirect.service.ShortUrlCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlRedirectController {

    private final ShortUrlCacheService cacheService;
    private final RedirectionHistoryService historyService;
    private final RedirectionConfigStore configStore;

    private String errorHtmlTemplate;

    @PostConstruct
    void loadErrorTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/redirect-error.html");
            errorHtmlTemplate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("[redirect] Failed to load error template, using fallback inline HTML", e);
            errorHtmlTemplate = "<!DOCTYPE html><html><body><h1>이동 실패</h1><p>%s</p></body></html>";
        }
    }

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
                sb.append(first ? "?" : "&")
                  .append(URLEncoder.encode(field.trim(), StandardCharsets.UTF_8))
                  .append("=")
                  .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
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
        return String.format(errorHtmlTemplate, escapeHtml(reason));
    }

    /**
     * HTML 특수문자 이스케이프 (XSS 방어).
     * 외부 라이브러리 의존 없이 OWASP 권장 5개 문자 치환.
     */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }
}
