package com.nh.shorturl.dto.request.control;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedirectionConfigUpdateRequest {
    private String fallbackUrl;
    private String defaultHost;
    private Boolean showErrorPage;
    private String trackingFields;
}
