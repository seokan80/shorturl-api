package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.history.RedirectionHistoryService;
import com.nh.shorturl.admin.service.shorturl.ShortUrlService;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * short-url-redirect 서버 전용 내부 API.
 * 외부 공개 API가 아니므로 Swagger 태그를 Internal 로 분리한다.
 * 실제 운영 시 방화벽/리버스 프록시에서 /api/internal/** 를 외부에서 차단할 것.
 */
@Tag(name = "Internal", description = "redirect 서버 전용 내부 API (외부 노출 금지)")
@RestController
@RequestMapping("/openapi/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalApiController {

    private final ShortUrlService shortUrlService;
    private final RedirectionHistoryService redirectionHistoryService;

    @Value("${short-url.redirection.fallback-url}")
    private String fallbackUrl;

    @Value("${short-url.redirection.default-host}")
    private String defaultHost;

    @Value("${short-url.redirection.show-error-page}")
    private Boolean showErrorPage;

    @Value("${short-url.redirection.tracking-fields}")
    private String trackingFields;

    /**
     * redirect 서버 기동 시 캐시 워밍 용도.
     * 만료되지 않은 전체 단축 URL 목록을 반환한다.
     */
    @Operation(summary = "[내부] 전체 단축 URL 목록", description = "redirect 서버 캐시 워밍 전용. 외부 노출 금지.")
    @GetMapping("/short-url/all")
    public List<ShortUrlResponse> getAllShortUrls() {
        return shortUrlService.findAllForCaching();
    }

    /**
     * 지정 시각 이후 변경(생성·수정·삭제)된 단축 URL 목록.
     * redirect 서버의 증분 캐시 동기화(5분 폴링) 전용.
     */
    @Operation(summary = "[내부] 변경분 단축 URL 조회", description = "redirect 서버 증분 폴링 전용. 외부 노출 금지.")
    @GetMapping("/short-urls/changes")
    public List<ShortUrlResponse> getChangedShortUrls(
            @RequestParam("since") LocalDateTime since) {
        return shortUrlService.findChangedSince(since);
    }

    /**
     * redirect 서버의 리다이렉트 설정 조회.
     * 1분 주기로 폴링하여 설정을 최신 유지한다.
     */
    @Operation(summary = "[내부] 리다이렉트 설정 조회", description = "redirect 서버 설정 폴링 전용. 외부 노출 금지.")
    @GetMapping("/short-url/redirection-config")
    public RedirectionConfigResponse getRedirectionConfig() {
        return RedirectionConfigResponse.builder()
                .fallbackUrl(fallbackUrl)
                .defaultHost(defaultHost)
                .showErrorPage(showErrorPage)
                .trackingFields(trackingFields)
                .build();
    }

    /**
     * redirect 서버에서 발생한 리다이렉트 이력을 admin DB 에 저장.
     * redirect 서버는 DB 에 직접 접근하지 않고 이 API 를 통해 저장한다.
     */
    @Operation(summary = "[내부] 리다이렉트 이력 저장", description = "redirect 서버 이력 기록 전용. 외부 노출 금지.")
    @PostMapping("/short-url/redirections/history")
    public ResponseEntity<Void> saveHistory(@RequestBody RedirectionHistoryRequest request) {
        log.debug("[internal] save history for key={}", request.getShortUrlKey());
        redirectionHistoryService.saveRedirectionHistory(request);
        return ResponseEntity.ok().build();
    }
}
