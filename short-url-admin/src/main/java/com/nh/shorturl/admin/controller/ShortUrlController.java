package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.service.clientaccess.ClientAccessKeyService;
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
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final ClientAccessKeyService clientAccessKeyService;

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
        } catch (IllegalArgumentException e) {
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
     * 단축 URL 삭제.
     * 로그인한 사용자만 자신이 생성한 URL을 삭제할 수 있음
     */
    @PostMapping("/delete/{id}")
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
        try {
            // Sort 파라미터 파싱
            String[] sortParams = sort.split(",");
            String sortField = sortParams[0];
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

            String username = principal != null ? principal.getName() : null;
            ResultList<ShortUrlResponse> result = shortUrlService.listShortUrls(pageable, username);

            return ResultEntity.ok(result);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    /**
     * 단축 URL 만료일 수정.
     * 로그인한 사용자만 자신이 생성한 URL의 만료일을 수정할 수 있음
     */
    @PutMapping("/{id}/expiration")
    public ResultEntity<ShortUrlResponse> updateExpiration(
            @PathVariable Long id,
            @Valid @RequestBody ShortUrlUpdateRequest request,
            Principal principal) {
        try {
            if (principal == null) {
                return ResultEntity.of(ApiResult.UNAUTHORIZED);
            }

            ShortUrlResponse response = shortUrlService.updateShortUrlExpiration(id, request, principal.getName());
            return ResultEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        } catch (IllegalStateException e) {
            return ResultEntity.of(ApiResult.FORBIDDEN);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    private String resolveAccessKey(HttpServletRequest request) {
        String header = request.getHeader("X-access-key");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        header = request.getHeader("X-CLIENTACCESS-KEY");
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return null;
    }
}