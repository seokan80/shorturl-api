package com.nh.shorturl.dto.request.shorturl;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Schema(description = "단축 URL 만료일 수정 요청")
@Getter
@Setter
@NoArgsConstructor
public class ShortUrlUpdateRequest {
    @Schema(description = "새로운 만료 일시", example = "2025-12-31T23:59:59")
    private LocalDateTime expiredAt;
}
