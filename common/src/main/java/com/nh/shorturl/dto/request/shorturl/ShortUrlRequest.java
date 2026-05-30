package com.nh.shorturl.dto.request.shorturl;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Short URL 생성 요청 DTO
 */
@Schema(description = "단축 URL 생성 요청")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class ShortUrlRequest {

    @Schema(description = "원본 URL", example = "https://www.google.com")
    @NotBlank(message = "원본 URL 은 필수입니다.")
    @Size(max = 2000, message = "원본 URL 은 2000자를 넘을 수 없습니다.")
    @Pattern(regexp = "(?i)^https?://.+", message = "원본 URL 은 http:// 또는 https:// 로 시작해야 합니다.")
    private String longUrl;

    @Schema(description = "만료 일시 (절대 시각)", example = "2025-12-31T23:59:59")
    private LocalDateTime expireDate;

    @Schema(description = "유효 기간(일). 지정 시 오늘 기준 validDays 일 뒤의 23:59:59 로 만료 일시를 계산하며 expireDate 보다 우선한다. (예: validDays=1 → 내일 자정 직전)", example = "7")
    private Integer validDays;
}
