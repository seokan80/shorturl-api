package com.nh.shorturl.admin.service.clientaccess;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.repository.ClientAccessKeyRepository;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyCreateRequest;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyUpdateRequest;
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
public class ClientAccessKeyServiceImpl implements ClientAccessKeyService {

    private final ClientAccessKeyRepository clientAccessKeyRepository;

    @Override
    @Transactional
    public ClientAccessKey create(ClientAccessKeyCreateRequest request) {
        String keyValue = UUID.randomUUID().toString().replaceAll("-", "");
        String name = StringUtils.hasText(request.getName()) ? request.getName() : "Client Access Key";
        LocalDateTime expiresAt = request.getExpiresAt() != null ? request.getExpiresAt()
                : LocalDateTime.now().plusYears(100);
        ClientAccessKey entity = ClientAccessKey.builder()
                .name(name)
                .issuedBy(request.getIssuedBy())
                .description(request.getDescription())
                .expiresAt(expiresAt)
                .keyValue(keyValue)
                .active(true)
                .build();
        return clientAccessKeyRepository.save(entity);
    }

    @Override
    @Transactional
    public ClientAccessKey update(Long id, ClientAccessKeyUpdateRequest request) {
        ClientAccessKey entity = clientAccessKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client access key not found with id: " + id));

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
        ClientAccessKey entity = clientAccessKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client access key not found with id: " + id));
        clientAccessKeyRepository.delete(entity);
    }

    @Override
    public List<ClientAccessKey> getKeys() {
        return clientAccessKeyRepository.findAllByDeletedFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public ClientAccessKey validateActiveKey(String keyValue) {
        ClientAccessKey key = clientAccessKeyRepository.findByKeyValueAndDeletedFalse(keyValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid client access key"));

        if (Boolean.FALSE.equals(key.getActive())) {
            throw new IllegalArgumentException("Client access key is inactive");
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Client access key expired");
        }
        key.setLastUsedAt(LocalDateTime.now());
        return key;
    }
}
