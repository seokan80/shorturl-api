package com.nh.shorturl.dto.request.clientaccess;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ClientAccessKeyUpdateRequest {
    private String name;
    private String description;
    private Boolean active;
    private LocalDateTime expiresAt;
}
