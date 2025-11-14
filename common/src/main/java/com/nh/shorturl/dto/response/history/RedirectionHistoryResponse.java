package com.nh.shorturl.dto.response.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Redirection History 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class RedirectionHistoryResponse {

    /** ID */
    private Long id;

    /** Short URL ID */
    private Long shortUrlId;

    /** Short URL 키 */
    private String shortUrlKey;

    /** Referer */
    private String referer;

    /** User Agent */
    private String userAgent;

    /** IP 주소 */
    private String ip;

    /** Redirect 일시 */
    private LocalDateTime redirectAt;
}
