package com.nh.shorturl.dto.request.shorturl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Short URL 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ShortUrlRequest {
    private String longUrl;
}
