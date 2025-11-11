package com.nh.shorturl.dto.response.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class UserDetailResponse {
    private Long id;
    private String username;
    private String groupName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
