package com.nh.shorturl.admin.service.control;

import com.nh.shorturl.admin.entity.RedirectionConfig;
import com.nh.shorturl.admin.repository.RedirectionConfigRepository;
import com.nh.shorturl.dto.request.control.RedirectionConfigUpdateRequest;
import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RedirectionConfigService {

    private final RedirectionConfigRepository repository;

    @Transactional(readOnly = true)
    public RedirectionConfigResponse getConfig() {
        RedirectionConfig config = getOrCreateConfig();
        return toResponse(config);
    }

    @Transactional
    public RedirectionConfigResponse updateConfig(RedirectionConfigUpdateRequest request) {
        RedirectionConfig config = getOrCreateConfig();
        config.setFallbackUrl(request.getFallbackUrl());
        config.setDefaultHost(request.getDefaultHost());
        config.setShowErrorPage(request.getShowErrorPage());
        config.setTrackingFields(request.getTrackingFields());
        return toResponse(config);
    }

    private RedirectionConfig getOrCreateConfig() {
        return repository.findTopByOrderByIdAsc()
                .orElseGet(() -> {
                    RedirectionConfig newConfig = RedirectionConfig.builder()
                            .showErrorPage(true)
                            .build();
                    return repository.save(newConfig);
                });
    }

    private RedirectionConfigResponse toResponse(RedirectionConfig entity) {
        return RedirectionConfigResponse.builder()
                .fallbackUrl(entity.getFallbackUrl())
                .defaultHost(entity.getDefaultHost())
                .showErrorPage(entity.getShowErrorPage())
                .trackingFields(entity.getTrackingFields())
                .build();
    }
}
