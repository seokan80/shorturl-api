package com.nh.shorturl.dto.request.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedirectionHistoryRequest {
    private String shortUrlKey;
    private String referer;
    private String userAgent;
    private String ip;
}
