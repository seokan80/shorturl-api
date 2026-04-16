package com.nh.shorturl.dto.response.shorturl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Short URL 응답 DTO
 */
@Schema(description = "단축 URL 응답 정보")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShortUrlResponse {

    private Long id;

    /** 생성된 단축 URL 키 */
    private String shortKey;

    /** 단축된 URL (ex: https://a.com/{shortKey}) */
    private String shortUrl;

    /** 원본 URL */
    private String longUrl;

    /** 생성일 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 만료일 */
    private String expiredAt;

    /** 삭제 여부 (증분 동기화 시 evict 판단용) */
    private Boolean deleted;
}
