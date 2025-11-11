package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.history.RedirectionHistoryService;
import com.nh.shorturl.admin.service.shorturl.ShortUrlService;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class ShortUrlInternalApiController {

    private final ShortUrlService shortUrlService;
    private final RedirectionHistoryService redirectionHistoryService;

    /**
     * short-url-redirect 모듈로부터 단축 URL 키를 받아 원본 URL 정보를 반환합니다.
     */
    @GetMapping("/short-urls/{shortUrlKey}")
    public ShortUrlResponse getShortUrl(@PathVariable String shortUrlKey) {
        return shortUrlService.getShortUrlByKey(shortUrlKey);
    }

    /**
     * short-url-redirect 모듈로부터 리디렉션 발생 기록을 받아 저장합니다.
     */
    @PostMapping("/redirection-histories")
    public ResponseEntity<Void> saveHistory(@RequestBody RedirectionHistoryRequest request) {
        // 이 요청을 처리하기 위해 RedirectionHistoryService에 request를 받는 메서드가 필요합니다.
        redirectionHistoryService.saveRedirectionHistory(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 캐시 초기화를 위해 모든 활성 단축 URL 정보를 반환합니다.
     */
    @GetMapping("/short-urls/all")
    public List<ShortUrlResponse> getAllShortUrlsForCaching() {
        return shortUrlService.findAllForCaching();
    }
}
