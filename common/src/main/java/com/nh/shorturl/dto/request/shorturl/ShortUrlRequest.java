package com.nh.shorturl.dto.request.shorturl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nh.shorturl.type.BotType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Short URL 생성 요청 DTO
 */
@Schema(description = "단축 URL 생성 요청")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ShortUrlRequest {
    @Schema(description = "원본 URL", example = "https://www.google.com")
    private String longUrl;

    @Schema(description = "만료 일시", example = "2025-12-31T23:59:59")
    private LocalDateTime expireDate;

    // 봇 정보 (선택 사항)
    @Schema(description = "봇 구분 (CALLBOT, CHATBOT)", example = "CALLBOT")
    private BotType botType;

    @Schema(description = "봇 서비스 식별 키 (전화번호/세션키)", example = "01012345678")
    private String botServiceKey;

    // 설문 정보
    @Schema(description = "연관 설문 ID", example = "survey-123")
    @JsonProperty("surveyId")
    private String surveyId;

    @Schema(description = "연관 설문 버전", example = "1.0")
    @JsonProperty("surveyVer")
    private String surveyVer;
}
