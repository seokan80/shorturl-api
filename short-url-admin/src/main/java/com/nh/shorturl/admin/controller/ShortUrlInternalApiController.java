package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.history.RedirectionHistoryService;
import com.nh.shorturl.admin.service.shorturl.ShortUrlService;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.history.RedirectionHistoryResponse;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
     * 리다이렉션 히스토리 목록 조회 (페이징).
     */
    @GetMapping("/redirection-histories")
    public ResultList<RedirectionHistoryResponse> listRedirectionHistories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "redirectAt,desc") String sort) {

        // sort 파라미터 파싱 (예: "redirectAt,desc" -> Sort.by(Direction.DESC, "redirectAt"))
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        return redirectionHistoryService.listRedirectionHistories(pageable);
    }

    /**
     * 리다이렉션 히스토리 상세 조회.
     */
    @GetMapping("/redirection-histories/{id}")
    public RedirectionHistoryResponse getRedirectionHistory(@PathVariable Long id) {
        return redirectionHistoryService.getRedirectionHistory(id);
    }

    /**
     * 캐시 초기화를 위해 모든 활성 단축 URL 정보를 반환합니다.
     */
    @GetMapping("/short-urls/all")
    public List<ShortUrlResponse> getAllShortUrlsForCaching() {
        return shortUrlService.findAllForCaching();
    }
}
