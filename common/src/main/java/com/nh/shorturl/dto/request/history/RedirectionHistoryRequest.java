package com.nh.shorturl.dto.request.history;

import com.nh.shorturl.type.BotType;
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
    private String deviceType;
    private String os;
    private String browser;
    private String country;
    private String city;

    // 봇 정보
    private BotType botType;
    private String botServiceKey;

    // 설문 정보
    private String surveyId;
    private String surveyVer;
}
