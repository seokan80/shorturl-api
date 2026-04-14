package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.dto.response.clientaccess.ClientAccessKeyResponse;
import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyCreateRequest;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.type.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Client Access Key", description = "단축 URL 생성을 위한 클라이언트 접근 키 관리 API")
@RestController
@RequestMapping("/api/client-keys")
@RequiredArgsConstructor
public class ClientAccessKeyController {

    private final ClientAccessKeyService clientAccessKeyService;

    @Operation(summary = "접근 키 목록 조회", description = "발급된 모든 클라이언트 접근 키 목록을 조회합니다.")
    @GetMapping
    public ResultEntity<?> list() {
        List<ClientAccessKeyResponse> response = clientAccessKeyService.getKeys().stream()
                .map(ClientAccessKeyResponse::from)
                .toList();
        return new ResultEntity<>(response);
    }

    @Operation(summary = "접근 키 발급", description = "신규 클라이언트 접근 키를 발급합니다.")
    @PostMapping
    public ResultEntity<?> issue(@RequestBody ClientAccessKeyCreateRequest request) {
        ClientAccessKey created = clientAccessKeyService.create(request);
        return new ResultEntity<>(ClientAccessKeyResponse.from(created));
    }

    @Operation(summary = "접근 키 수정", description = "기존 발급된 키의 명칭이나 만료일을 수정합니다.")
    @PutMapping("/{id}")
    public ResultEntity<?> update(@Parameter(description = "키 ID") @PathVariable Long id,
            @RequestBody ClientAccessKeyUpdateRequest request) {
        try {
            ClientAccessKey updated = clientAccessKeyService.update(id, request);
            return new ResultEntity<>(ClientAccessKeyResponse.from(updated));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        }
    }

    @Operation(summary = "접근 키 삭제", description = "발급된 클라이언트 접근 키를 폐기 처리합니다.")
    @DeleteMapping("/{id}")
    public ResultEntity<?> delete(@Parameter(description = "키 ID") @PathVariable Long id) {
        try {
            clientAccessKeyService.delete(id);
            return ResultEntity.True();
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        }
    }
}
