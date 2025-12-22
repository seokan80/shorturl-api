package com.nh.shorturl.dto.request.shorturl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nh.shorturl.type.BotType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Short URL 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ShortUrlRequest {
    private String longUrl;
    private LocalDateTime expireDate;

    // 봇 정보 (선택 사항)
    private BotType botType;
    private String botServiceKey; // 전화번호(콜봇) 또는 세션키(챗봇)

    // 설문 정보
    @JsonProperty("surveyId")
    private String surveyId;

    @JsonProperty("surveyVer")
    private String surveyVer;
}
