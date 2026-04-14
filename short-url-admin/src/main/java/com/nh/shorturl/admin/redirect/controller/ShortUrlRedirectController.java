package com.nh.shorturl.admin.redirect.controller;

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
 * 단축 URL 리다이렉트 엔드포인트. /r/{shortKey} 경로로 서비스한다.
 * (admin UI 가 "/" 를 소유하므로 기존 redirect 모듈의 "/{shortUrl}" 매핑은 사용하지 않는다.)
 */
@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlRedirectController {

    private final ShortUrlLookupService shortUrlLookupService;
    private final RedirectionHistoryAsyncWriter historyWriter;
    private final RedirectionConfigStore configStore;

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

            historyWriter.write(
                    shortKey,
                    request,
                    found.getBotType(),
                    found.getBotServiceKey(),
                    found.getSurveyId(),
                    found.getSurveyVer());

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
            response.getWriter().write("<!DOCTYPE html><html><head><title>Redirection Error</title>"
                    + "<style>body{font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;background:#f8fafc;color:#1e293b;}"
                    + ".card{background:white;padding:2rem;border-radius:1rem;box-shadow:0 10px 15px -3px rgba(0,0,0,0.1);max-width:400px;text-align:center;}"
                    + "h1{color:#e11d48;margin-top:0;}p{color:#64748b;line-height:1.6;}</style></head>"
                    + "<body><div class='card'><h1>이동 실패</h1><p>" + reason + "</p>"
                    + "<p>요청하신 URL이 존재하지 않거나 만료되었습니다.</p></div></body></html>");
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND, reason);
    }
}
