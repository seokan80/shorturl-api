package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.history.RedirectionHistoryService;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.history.RedirectionHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/redirections/history")
@RequiredArgsConstructor
public class RedirectionHistoryController {

    private final RedirectionHistoryService redirectionHistoryService;

    /**
     * 리디렉션 히스토리 목록 조회 (페이징)
     */
    @GetMapping
    public ResultEntity<Page<RedirectionHistoryResponse>> getAll(Pageable pageable) {
        return ResultEntity.ok(redirectionHistoryService.findAll(pageable));
    }

    /**
     * 리디렉션 히스토리 상세 조회
     */
    @GetMapping("/{id}")
    public ResultEntity<RedirectionHistoryResponse> getById(@PathVariable Long id) {
        return ResultEntity.ok(redirectionHistoryService.findById(id));
    }

    /**
     * 특정 URL의 리디렉션 횟수 조회
     */
    @GetMapping("/{shortUrlId}/count")
    public ResultEntity<?> getCount(@PathVariable Long shortUrlId) {
        int count = redirectionHistoryService.getRedirectCount(shortUrlId);
        return ResultEntity.ok(count);
    }

    /**
     * 특정 URL의 통계 데이터 조회
     */
    @PostMapping("/{shortUrlId}/stats")
    public ResultEntity<List<Map<String, Object>>> getStats(
            @PathVariable Long shortUrlId,
            @RequestBody RedirectionStatsRequest request) {
        List<Map<String, Object>> stats = redirectionHistoryService.getStats(shortUrlId, request);
        return ResultEntity.ok(stats);
    }

    /**
     * 내부 모듈(redirect) 에서 전달받은 히스토리 저장
     * 통합 관리를 위해 이 컨트롤러 내부에 배치 (보안 설정 시 경로 패턴 주의 필요)
     */
    @PostMapping("/internal")
    public ResponseEntity<Void> saveInternal(@RequestBody RedirectionHistoryRequest request) {
        redirectionHistoryService.saveRedirectionHistory(request);
        return ResponseEntity.ok().build();
    }
}
