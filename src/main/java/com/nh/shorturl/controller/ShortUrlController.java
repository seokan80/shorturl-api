package com.nh.shorturl.controller;

import com.nh.shorturl.config.ClientAccessKeyValidationFilter;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.entity.ClientAccessKey;
import com.nh.shorturl.service.shorturl.ShortUrlService;
import com.nh.shorturl.type.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
     * 로그인 사용자(JWT) 또는 비회원(X-access-key) 모두 지원
     */
    @PostMapping
    public ResultEntity<?> create(@RequestBody ShortUrlRequest request,
                                  Principal principal,
                                  HttpServletRequest httpRequest) {
        try {
            // 로그인 사용자 (JWT 인증)
            if (principal != null) {
                return new ResultEntity<>(shortUrlService.createShortUrl(request, principal.getName()));
            }

            // 비회원 (X-access-key 인증)
            ClientAccessKey validatedKey = (ClientAccessKey) httpRequest.getAttribute(
                    ClientAccessKeyValidationFilter.CLIENT_ACCESS_KEY_ATTRIBUTE);

            if (validatedKey != null) {
                return new ResultEntity<>(shortUrlService.createShortUrlForClient(request, validatedKey));
            }

            // 둘 다 없으면 인증 실패
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
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
     * 단축 URL 목록 조회 (페이징).
     * 기본 정렬: 생성일 내림차순, 기본 페이지 크기: 20
     */
    @GetMapping("/list")
    public ResultEntity<?> getList(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<?> result = shortUrlService.getShortUrlList(pageable);
            return ResultEntity.ok(result);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    /**
     * 단축 URL 수정 (만료기간).
     */
    @PutMapping("/{id}")
    public ResultEntity<?> update(@PathVariable Long id, @RequestBody ShortUrlUpdateRequest request) {
        try {
            return ResultEntity.ok(shortUrlService.updateShortUrl(id, request));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
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