package com.nh.shorturl.dto.request.serverauth;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ServerAuthKeyUpdateRequest {
    private String name;
    private String description;
    private Boolean active;
    private LocalDateTime expiresAt;
}
