package com.nh.shorturl.dto.request.serverauth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ServerAuthKeyCreateRequest {
    private String name;
    private String issuedBy;
    private String description;
    private LocalDateTime expiresAt;
}
