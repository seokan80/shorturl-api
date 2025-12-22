package com.nh.shorturl.dto.response.history;

import com.nh.shorturl.type.BotType;
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

    // 봇 정보
    private BotType botType;
    private String botServiceKey;

    // 설문 정보
    private String surveyId;
    private String surveyVer;
}
