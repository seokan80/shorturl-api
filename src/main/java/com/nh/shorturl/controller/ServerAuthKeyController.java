package com.nh.shorturl.controller;

import com.nh.shorturl.dto.request.serverauth.ServerAuthKeyCreateRequest;
import com.nh.shorturl.dto.request.serverauth.ServerAuthKeyUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.serverauth.ServerAuthKeyResponse;
import com.nh.shorturl.entity.ServerAuthKey;
import com.nh.shorturl.service.serverauth.ServerAuthKeyService;
import com.nh.shorturl.type.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/server-keys")
@RequiredArgsConstructor
public class ServerAuthKeyController {

    private final ServerAuthKeyService serverAuthKeyService;

    @GetMapping
    public ResultEntity<?> list() {
        List<ServerAuthKeyResponse> response = serverAuthKeyService.getKeys().stream()
            .map(ServerAuthKeyResponse::from)
            .toList();
        return new ResultEntity<>(response);
    }

    @PostMapping
    public ResultEntity<?> issue(@RequestBody ServerAuthKeyCreateRequest request) {
        ServerAuthKey created = serverAuthKeyService.create(request);
        return new ResultEntity<>(ServerAuthKeyResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResultEntity<?> update(@PathVariable Long id,
                                  @RequestBody ServerAuthKeyUpdateRequest request) {
        try {
            ServerAuthKey updated = serverAuthKeyService.update(id, request);
            return new ResultEntity<>(ServerAuthKeyResponse.from(updated));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResultEntity<?> delete(@PathVariable Long id) {
        try {
            serverAuthKeyService.delete(id);
            return ResultEntity.True();
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        }
    }
}
