package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.service.shorturl.ShortUrlService;
import com.nh.shorturl.config.ClientAccessKeyValidationFilter;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.type.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * 단축 URL 관련 REST API 컨트롤러.
 */
@RestController
@RequestMapping("/api/short-url")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    /**
     * 단축 URL 생성 API.
     * 로그인 사용자(JWT) 또는 비회원(X-access-key) 모두 지원
     */
    @PostMapping
    public ResultEntity<?> create(@RequestBody @Valid ShortUrlRequest request,
            Principal principal,
            HttpServletRequest httpRequest) {
        try {
            // 1. 비회원 (X-access-key 인증) 확인 - 우선 순위
            ClientAccessKey validatedKey = (ClientAccessKey) httpRequest.getAttribute(
                    ClientAccessKeyValidationFilter.CLIENT_ACCESS_KEY_ATTRIBUTE);

            if (validatedKey != null) {
                return new ResultEntity<>(shortUrlService.createShortUrlForClient(request, validatedKey));
            }

            // 2. 로그인 사용자 (JWT 인증)
            if (principal != null && !"anonymous".equals(principal.getName())) {
                return new ResultEntity<>(shortUrlService.createShortUrl(request, principal.getName()));
            }

            // 둘 다 없거나 익명이면 인증 실패
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
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
    @DeleteMapping("/{id}")
    public ResultEntity<?> delete(@PathVariable Long id, Principal principal) {
        try {
            if (principal == null) {
                return ResultEntity.of(ApiResult.UNAUTHORIZED);
            }
            shortUrlService.deleteShortUrl(id, principal.getName());
            return ResultEntity.True();
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        } catch (IllegalStateException e) {
            return ResultEntity.of(ApiResult.FORBIDDEN);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    /**
     * 단축 URL 목록 조회 (페이징).
     * 로그인한 사용자는 자신이 생성한 URL만 조회, 비로그인 시 전체 조회
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param sort 정렬 기준 (기본값: createdAt,desc)
     */
    @GetMapping
    public ResultEntity<ResultList<ShortUrlResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            Principal principal) {

        String username = null;
        // ClientKey를 통해 "anonymous"로 인증된 경우, 전체 조회를 위해 username을 null로 유지하거나
        // 명시적으로 처리할 수 있습니다. 여기서는 JWT 실사용자인 경우만 username을 할당합니다.
        if (principal != null && !"anonymous".equals(principal.getName())) {
            username = principal.getName();
        }

        // 정렬 정보 파싱 (예: "id,desc" -> Sort.by(Sort.Direction.DESC, "id"))
        String[] sortParts = sort.split(",");
        Sort sortObj = Sort.by(sortParts[0]);
        if (sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])) {
            sortObj = sortObj.descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        return ResultEntity.ok(shortUrlService.listShortUrls(pageable, username));
    }

    /**
     * 단축 URL 만료일 수정 API.
     */
    @PutMapping("/{id}/expiration")
    public ResultEntity<?> updateExpiration(
            @PathVariable Long id,
            @RequestBody @Valid ShortUrlUpdateRequest request,
            Principal principal) {
        try {
            if (principal == null) {
                return ResultEntity.of(ApiResult.UNAUTHORIZED);
            }
            return ResultEntity.ok(shortUrlService.updateShortUrlExpiration(id, request, principal.getName()));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        } catch (IllegalStateException e) {
            return ResultEntity.of(ApiResult.FORBIDDEN);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }
}
