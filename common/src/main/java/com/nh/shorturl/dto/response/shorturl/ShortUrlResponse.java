package com.nh.shorturl.dto.response.shorturl;

import com.nh.shorturl.type.BotType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Short URL 응답 DTO
 */
@Schema(description = "단축 URL 응답 정보")
@Getter
@Builder
@AllArgsConstructor
public class ShortUrlResponse {

    private Long id;

    /** 생성된 단축 URL 키 */
    private String shortKey;

    /** 단축된 URL (ex: https://a.com/{shortKey}) */
    private String shortUrl;

    /** 원본 URL */
    private String longUrl;

    /** 생성일 */
    private LocalDateTime createdAt;

    /** 만료일 */
    private String expiredAt;

    // 봇 정보
    private BotType botType;
    private String botServiceKey;

    // 설문 정보
    private String surveyId;
    private String surveyVer;
}