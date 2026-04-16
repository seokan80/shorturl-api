package com.nh.shorturl.redirect.controller;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.redirect.service.ShortUrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * admin 서버가 단축 URL 생성/수정/삭제 시 redirect 서버 캐시를 동기화하기 위한 내부 API.
 * 실제 운영 시 방화벽/리버스 프록시에서 외부 접근을 차단할 것.
 */
@RestController
@RequestMapping("/api/internal/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheManagementController {

    private final ShortUrlCacheService cacheService;

    @PutMapping("/short-urls")
    public ResponseEntity<Void> update(@RequestBody ShortUrlResponse response) {
        log.info("[cache] update key={}", response.getShortKey());
        cacheService.put(response);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/short-urls/{shortKey}")
    public ResponseEntity<Void> evict(@PathVariable String shortKey) {
        log.info("[cache] evict key={}", shortKey);
        cacheService.evict(shortKey);
        return ResponseEntity.ok().build();
    }
}
