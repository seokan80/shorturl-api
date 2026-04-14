package com.nh.shorturl.dto.request.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 사용자 정보 수정 요청 DTO
 */
@Schema(description = "사용자 정보 수정 요청")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class UserUpdateRequest {

    @Schema(description = "고객사명 (그룹명)", example = "NH농협은행")
    private String groupName;
}
