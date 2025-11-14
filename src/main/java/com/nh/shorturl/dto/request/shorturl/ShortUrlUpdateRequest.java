package com.nh.shorturl.dto.request.shorturl;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Short URL 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class ShortUrlUpdateRequest {

    /**
     * 만료일 (필수)
     */
    private LocalDateTime expiredAt;
}
