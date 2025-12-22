package com.nh.shorturl.redirect.controller;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.redirect.service.ShortUrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheManagementController {

    private final ShortUrlCacheService cacheService;

    @PutMapping("/short-urls")
    public ResponseEntity<Void> updateCache(@RequestBody ShortUrlResponse response) {
        log.info("Updating cache for key: {}", response.getShortKey());
        cacheService.updateShortUrlInCache(response);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/short-urls/{shortUrlKey}")
    public ResponseEntity<Void> evictCache(@PathVariable String shortUrlKey) {
        log.info("Evicting cache for key: {}", shortUrlKey);
        cacheService.evictShortUrlFromCache(shortUrlKey);
        return ResponseEntity.ok().build();
    }
}
