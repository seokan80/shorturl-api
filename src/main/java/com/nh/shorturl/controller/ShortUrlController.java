package com.nh.shorturl.controller;

import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.service.shorturl.ShortUrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 단축 URL 관련 REST API 컨트롤러.
 */
@RestController
@RequestMapping("/api/short-url")
@RequiredArgsConstructor
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    /**
     * 단축 URL 생성 API.
     */
    @PostMapping
    public ResponseEntity<ShortUrlResponse> create(@RequestBody ShortUrlRequest request) {
        try {
            return ResponseEntity.ok(shortUrlService.createShortUrl(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 단축 URL 단건 조회 (ID 기반).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShortUrlResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(shortUrlService.getShortUrl(id));
    }

    /**
     * 단축 URL 키 기반 조회.
     */
    @GetMapping("/key/{shortUrl}")
    public ResponseEntity<ShortUrlResponse> getByKey(@PathVariable String shortUrl) {
        return ResponseEntity.ok(shortUrlService.getShortUrlByKey(shortUrl));
    }

    /**
     * 단축 URL 삭제.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shortUrlService.deleteShortUrl(id);
        return ResponseEntity.noContent().build();
    }
}