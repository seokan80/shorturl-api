package com.nh.shorturl.dto.response.control;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedirectionConfigResponse {
    private String fallbackUrl;
    private String defaultHost;
    private Boolean showErrorPage;
    private String trackingFields;
}
