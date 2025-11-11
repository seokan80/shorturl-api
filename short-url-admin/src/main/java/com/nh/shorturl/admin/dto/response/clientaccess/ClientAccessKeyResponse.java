package com.nh.shorturl.admin.dto.response.clientaccess;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class ClientAccessKeyResponse {
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

    public static ClientAccessKeyResponse from(ClientAccessKey entity) {
        return new ClientAccessKeyResponse(
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
