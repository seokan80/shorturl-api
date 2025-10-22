package com.nh.shorturl.dto.request.shorturl;

import jakarta.validation.constraints.NotBlank;
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
    // username 필드 제거
}
