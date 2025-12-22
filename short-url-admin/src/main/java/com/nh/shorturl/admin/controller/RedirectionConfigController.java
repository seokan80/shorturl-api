package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.control.RedirectionConfigService;
import com.nh.shorturl.dto.request.control.RedirectionConfigUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redirection-configs")
@RequiredArgsConstructor
public class RedirectionConfigController {

    private final RedirectionConfigService redirectionConfigService;

    @GetMapping
    public ResultEntity<RedirectionConfigResponse> getConfig() {
        return ResultEntity.ok(redirectionConfigService.getConfig());
    }

    @PutMapping
    public ResultEntity<RedirectionConfigResponse> updateConfig(@RequestBody RedirectionConfigUpdateRequest request) {
        return ResultEntity.ok(redirectionConfigService.updateConfig(request));
    }
}
