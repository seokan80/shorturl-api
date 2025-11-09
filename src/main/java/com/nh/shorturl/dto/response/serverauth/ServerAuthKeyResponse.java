package com.nh.shorturl.dto.response.serverauth;

import com.nh.shorturl.entity.ServerAuthKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ServerAuthKeyResponse {
    private Long id;
    private String name;
    private String keyValue;
    private String issuedBy;
    private String description;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ServerAuthKeyResponse from(ServerAuthKey entity) {
        return new ServerAuthKeyResponse(
            entity.getId(),
            entity.getName(),
            entity.getKeyValue(),
            entity.getIssuedBy(),
            entity.getDescription(),
            entity.getExpiresAt(),
            entity.getLastUsedAt(),
            entity.getActive(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
