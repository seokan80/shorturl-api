package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.history.RedirectionHistoryService;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.history.RedirectionHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Analytics", description = "단축 URL 리다이렉션 통계 및 이력 관리 API")
@RestController
@RequestMapping("/api/redirections/history")
@RequiredArgsConstructor
public class RedirectionHistoryController {

    private final RedirectionHistoryService redirectionHistoryService;

    @Operation(summary = "리다이렉션 이력 목록", description = "발생한 모든 리다이렉션 이력을 페이징하여 조회합니다.")
    @GetMapping
    public ResultEntity<Page<RedirectionHistoryResponse>> getAll(Pageable pageable) {
        return ResultEntity.ok(redirectionHistoryService.findAll(pageable));
    }

    @Operation(summary = "리다이렉션 이력 상세", description = "특정 리다이렉션 이벤트의 상세 정보(IP, UA 등)를 조회합니다.")
    @GetMapping("/{id}")
    public ResultEntity<RedirectionHistoryResponse> getById(@Parameter(description = "이력 ID") @PathVariable Long id) {
        return ResultEntity.ok(redirectionHistoryService.findById(id));
    }
}
