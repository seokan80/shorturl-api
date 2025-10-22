package com.nh.shorturl.controller;

import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.service.shorturl.ShortUrlService;
import com.nh.shorturl.type.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

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
    public ResultEntity<?> create(@RequestBody ShortUrlRequest request, Principal principal) {
        try {
            // JWT 토큰에서 추출한 username을 서비스로 전달
            return new ResultEntity<>(shortUrlService.createShortUrl(request, principal.getName()));
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    /**
     * 단축 URL 단건 조회 (ID 기반).
     */
    @GetMapping("/{id}")
    public ResultEntity<?> getById(@PathVariable Long id) {
        try {
            return ResultEntity.ok(shortUrlService.getShortUrl(id));
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        }
    }

    /**
     * 단축 URL 키 기반 조회.
     */
    @GetMapping("/key/{shortUrl}")
    public ResultEntity<?> getByKey(@PathVariable String shortUrl) {
        try {
            return ResultEntity.ok(shortUrlService.getShortUrlByKey(shortUrl));
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        }
    }

    /**
     * 단축 URL 삭제.
     */
    @PostMapping("/delete/{id}")
    public ResultEntity<?> delete(@PathVariable Long id) {
        try {
            shortUrlService.deleteShortUrl(id);
            return ResultEntity.True();
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }
}