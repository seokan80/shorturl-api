package com.nh.shorturl.admin.redirect.controller;

import com.nh.shorturl.admin.redirect.service.RedirectErrorPageRenderer;
import com.nh.shorturl.admin.redirect.service.RedirectionConfigStore;
import com.nh.shorturl.admin.redirect.service.RedirectionHistoryAsyncWriter;
import com.nh.shorturl.admin.redirect.service.ShortUrlLookupService;
import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 단축 URL 리다이렉트 엔드포인트. /{shortKey} 경로로 서비스한다.
 * (admin UI 가 "/" 를 소유하므로 기존 redirect 모듈의 "/{shortUrl}" 매핑은 사용하지 않는다.)
 */
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlRedirectController {

    private final ShortUrlLookupService shortUrlLookupService;
    private final RedirectionHistoryAsyncWriter historyWriter;
    private final RedirectionConfigStore configStore;
    private final RedirectErrorPageRenderer errorPageRenderer;

    /**
     * 배포 후 스모크 테스트용 고정 키.
     * DB·캐시와 무관하게 동작하므로 서비스 시작 직후에도 즉시 사용 가능하다.
     * GET /verify  →  short-url.redirection.default-host 로 302 리다이렉트
     */
    @GetMapping("/verify")
    public void verify(HttpServletResponse response) throws IOException {
        String target = configStore.getConfig().getDefaultHost();
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
            ShortUrlResponse found = shortUrlLookupService.findByKey(shortKey);
            if (found == null || found.getLongUrl() == null) {
                handleFailure(response, config, "URL not found or expired");
                return;
            }

            historyWriter.write(shortKey, request);

            String targetUrl = appendTrackingFields(found.getLongUrl(), request, config);
            response.sendRedirect(targetUrl);
        } catch (Exception e) {
            log.error("Redirection failed for shortKey: {}. Reason: {}", shortKey, e.getMessage());
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
            response.getWriter().write(errorPageRenderer.render(reason));
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND, reason);
    }
}
