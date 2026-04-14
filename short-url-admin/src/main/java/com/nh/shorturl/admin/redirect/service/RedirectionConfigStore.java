package com.nh.shorturl.admin.redirect.service;

import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 리다이렉트 전역 설정을 보관한다. 단일 애플리케이션 통합 후에는 동일 JVM 의
 * application.yml 값을 직접 주입받으므로 WebClient 폴링이 불필요하다.
 */
@Component
public class RedirectionConfigStore {

    private final RedirectionConfigResponse config;

    public RedirectionConfigStore(
            @Value("${short-url.redirection.fallback-url}") String fallbackUrl,
            @Value("${short-url.redirection.default-host}") String defaultHost,
            @Value("${short-url.redirection.show-error-page}") Boolean showErrorPage,
            @Value("${short-url.redirection.tracking-fields}") String trackingFields) {
        this.config = RedirectionConfigResponse.builder()
                .fallbackUrl(fallbackUrl)
                .defaultHost(defaultHost)
                .showErrorPage(showErrorPage)
                .trackingFields(trackingFields)
                .build();
    }

    public RedirectionConfigResponse getConfig() {
        return config;
    }
}
