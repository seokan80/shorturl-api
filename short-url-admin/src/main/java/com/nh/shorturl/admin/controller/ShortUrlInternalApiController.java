package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.shorturl.ShortUrlService;
import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class ShortUrlInternalApiController {

    private final ShortUrlService shortUrlService;

    @Value("${short-url.redirection.fallback-url}")
    private String fallbackUrl;

    @Value("${short-url.redirection.default-host}")
    private String defaultHost;

    @Value("${short-url.redirection.show-error-page}")
    private Boolean showErrorPage;

    @Value("${short-url.redirection.tracking-fields}")
    private String trackingFields;

    /**
     * short-url-redirect 모듈로부터 단축 URL 키를 받아 원본 URL 정보를 반환합니다.
     */
    @GetMapping("/short-urls/{shortUrlKey}")
    public ShortUrlResponse getShortUrl(@PathVariable String shortUrlKey) {
        return shortUrlService.getShortUrlByKey(shortUrlKey);
    }

    /**
     * 캐시 초기화를 위해 모든 활성 단축 URL 정보를 반환합니다.
     */
    @GetMapping("/short-urls/all")
    public List<ShortUrlResponse> getAllShortUrlsForCaching() {
        return shortUrlService.findAllForCaching();
    }

    /**
     * 리디렉션 제어 설정을 반환합니다.
     */
    @GetMapping("/redirection-config")
    public RedirectionConfigResponse getRedirectionConfig() {
        return RedirectionConfigResponse.builder()
                .fallbackUrl(fallbackUrl)
                .defaultHost(defaultHost)
                .showErrorPage(showErrorPage)
                .trackingFields(trackingFields)
                .build();
    }
}
