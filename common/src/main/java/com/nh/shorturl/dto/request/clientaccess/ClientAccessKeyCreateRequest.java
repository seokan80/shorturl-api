package com.nh.shorturl.dto.request.clientaccess;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ClientAccessKeyCreateRequest {
    private String name;
    private String issuedBy;
    private String description;
    private LocalDateTime expiresAt;
}
