package com.nh.shorturl.dto.response.shorturl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Short URL 응답 DTO
 */
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

    /** 생성자 ID */
    private Long createdBy;

    /** 생성일 */
    private String createdAt;

    /** 만료일 */
    private String expiredAt;
}