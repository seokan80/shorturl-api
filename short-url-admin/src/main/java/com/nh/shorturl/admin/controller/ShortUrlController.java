package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.exception.GlobalExceptionHandler;
import com.nh.shorturl.admin.service.shorturl.ShortUrlService;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/**
 * 단축 URL 관련 REST API 컨트롤러.
 * 폐쇄망 내부 도구 전제로 인증이 제거된 상태.
 *
 * <p>예외는 {@link GlobalExceptionHandler} 가 공통 매핑한다. 컨트롤러는 예외 처리 없이 서비스에 위임만 한다.
 */
@Tag(name = "Short URL", description = "단축 URL 생성 및 관리 API")
@RestController
@RequestMapping("/api/short-url")
@RequiredArgsConstructor
@Slf4j
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    @Operation(summary = "단축 URL 생성", description = "긴 URL을 단축 키로 변환합니다.")
    @PostMapping
    public ResultEntity<ShortUrlResponse> create(@RequestBody @Valid ShortUrlRequest request) {
        return new ResultEntity<>(shortUrlService.createShortUrl(request));
    }

    @Operation(summary = "단축 URL 상세 조회 (ID)", description = "고유 ID를 기반으로 단축 URL 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResultEntity<ShortUrlResponse> getById(@Parameter(description = "단축 URL 고유 ID") @PathVariable Long id) {
        return ResultEntity.ok(shortUrlService.getShortUrl(id));
    }

    @Operation(summary = "단축 URL 상세 조회 (Key)", description = "단축 키(Short Key)를 기반으로 단축 URL 정보를 조회합니다.")
    @GetMapping("/key/{shortUrl}")
    public ResultEntity<ShortUrlResponse> getByKey(@Parameter(description = "단축 키") @PathVariable String shortUrl) {
        return ResultEntity.ok(shortUrlService.getShortUrlByKey(shortUrl));
    }

    @Operation(summary = "단축 URL 삭제", description = "단축 URL 을 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResultEntity<?> delete(@Parameter(description = "단축 URL 고유 ID") @PathVariable Long id) {
        shortUrlService.deleteShortUrl(id);
        return ResultEntity.True();
    }

    @Operation(summary = "단축 URL 목록 조회", description = "페이징 처리된 단축 URL 목록을 반환합니다.")
    @GetMapping
    public ResultEntity<ResultList<ShortUrlResponse>> list(
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준 (필드명,asc|desc)") @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort sortObj = Sort.by(sortParts[0]);
        if (sortParts.length > 1 && "desc".equalsIgnoreCase(sortParts[1])) {
            sortObj = sortObj.descending();
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);
        return ResultEntity.ok(shortUrlService.listShortUrls(pageable));
    }

    @Operation(summary = "단축 URL 만료일 수정", description = "단축 URL의 만료 일시를 변경합니다.")
    @PutMapping("/{id}/expiration")
    public ResultEntity<ShortUrlResponse> updateExpiration(
            @Parameter(description = "단축 URL 고유 ID") @PathVariable Long id,
            @RequestBody @Valid ShortUrlUpdateRequest request) {
        return ResultEntity.ok(shortUrlService.updateShortUrlExpiration(id, request));
    }
}
