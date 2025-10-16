package com.nh.shorturl.dto.request.shorturl;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Short URL 생성 요청 DTO
 */
@Getter
@Setter
public class ShortUrlRequest {

    /** 단축시킬 원본 URL */
    @NotBlank(message = "원본 URL은 필수입니다.")
    private String longUrl;

    /** 해당 요청을 구분하기 위한 고객 ID (선택적) */
    private String username;
}
