package com.nh.shorturl.dto.request.shorturl;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
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
     * 만료 일시
     * ISO 8601 형식으로 전달 (예: "2025-12-31T23:59:59")
     */
    @NotNull(message = "만료 일시는 필수입니다.")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiredAt;
}
