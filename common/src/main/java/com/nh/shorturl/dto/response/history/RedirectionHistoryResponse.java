package com.nh.shorturl.dto.response.history;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedirectionHistoryResponse {
    private Long id;
    private Long shortUrlId;
    private String shortKey;
    private String referer;
    private String userAgent;
    private String ip;
    private String deviceType;
    private String os;
    private String browser;
    private String country;
    private String city;
    private LocalDateTime redirectAt;
}
