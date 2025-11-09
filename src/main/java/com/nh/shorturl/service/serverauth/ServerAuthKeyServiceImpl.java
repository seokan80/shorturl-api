package com.nh.shorturl.service.serverauth;

import com.nh.shorturl.dto.request.serverauth.ServerAuthKeyCreateRequest;
import com.nh.shorturl.dto.request.serverauth.ServerAuthKeyUpdateRequest;
import com.nh.shorturl.entity.ServerAuthKey;
import com.nh.shorturl.repository.ServerAuthKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServerAuthKeyServiceImpl implements ServerAuthKeyService {

    private final ServerAuthKeyRepository serverAuthKeyRepository;

    @Override
    @Transactional
    public ServerAuthKey create(ServerAuthKeyCreateRequest request) {
        String keyValue = UUID.randomUUID().toString().replaceAll("-", "");
        String name = StringUtils.hasText(request.getName()) ? request.getName() : "Server Auth Key";
        ServerAuthKey entity = ServerAuthKey.builder()
            .name(name)
            .issuedBy(request.getIssuedBy())
            .description(request.getDescription())
            .expiresAt(request.getExpiresAt())
            .keyValue(keyValue)
            .active(true)
            .build();
        return serverAuthKeyRepository.save(entity);
    }

    @Override
    @Transactional
    public ServerAuthKey update(Long id, ServerAuthKeyUpdateRequest request) {
        ServerAuthKey entity = serverAuthKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Server auth key not found with id: " + id));

        if (StringUtils.hasText(request.getName())) {
            entity.setName(request.getName());
        }
        entity.setDescription(request.getDescription());
        entity.setExpiresAt(request.getExpiresAt());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        return entity;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ServerAuthKey entity = serverAuthKeyRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Server auth key not found with id: " + id));
        serverAuthKeyRepository.delete(entity);
    }

    @Override
    public List<ServerAuthKey> getKeys() {
        return serverAuthKeyRepository.findAllByDeletedFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public ServerAuthKey validateActiveKey(String keyValue) {
        ServerAuthKey key = serverAuthKeyRepository.findByKeyValueAndDeletedFalse(keyValue)
            .orElseThrow(() -> new IllegalArgumentException("Invalid server authentication key"));

        if (Boolean.FALSE.equals(key.getActive())) {
            throw new IllegalArgumentException("Server authentication key is inactive");
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Server authentication key expired");
        }
        key.setLastUsedAt(LocalDateTime.now());
        return key;
    }
}
