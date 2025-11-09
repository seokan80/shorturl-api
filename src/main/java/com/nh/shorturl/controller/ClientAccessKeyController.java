package com.nh.shorturl.controller;

import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyCreateRequest;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyUpdateRequest;
import com.nh.shorturl.dto.response.clientaccess.ClientAccessKeyResponse;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.entity.ClientAccessKey;
import com.nh.shorturl.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.type.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/client-keys")
@RequiredArgsConstructor
public class ClientAccessKeyController {

    private final ClientAccessKeyService clientAccessKeyService;

    @GetMapping
    public ResultEntity<?> list() {
        List<ClientAccessKeyResponse> response = clientAccessKeyService.getKeys().stream()
            .map(ClientAccessKeyResponse::from)
            .toList();
        return new ResultEntity<>(response);
    }

    @PostMapping
    public ResultEntity<?> issue(@RequestBody ClientAccessKeyCreateRequest request) {
        ClientAccessKey created = clientAccessKeyService.create(request);
        return new ResultEntity<>(ClientAccessKeyResponse.from(created));
    }

    @PutMapping("/{id}")
    public ResultEntity<?> update(@PathVariable Long id,
                                  @RequestBody ClientAccessKeyUpdateRequest request) {
        try {
            ClientAccessKey updated = clientAccessKeyService.update(id, request);
            return new ResultEntity<>(ClientAccessKeyResponse.from(updated));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResultEntity<?> delete(@PathVariable Long id) {
        try {
            clientAccessKeyService.delete(id);
            return ResultEntity.True();
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.NOT_FOUND);
        }
    }
}
