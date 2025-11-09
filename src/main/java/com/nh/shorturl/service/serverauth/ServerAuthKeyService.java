package com.nh.shorturl.service.serverauth;

import com.nh.shorturl.dto.request.serverauth.ServerAuthKeyCreateRequest;
import com.nh.shorturl.dto.request.serverauth.ServerAuthKeyUpdateRequest;
import com.nh.shorturl.entity.ServerAuthKey;

import java.util.List;

public interface ServerAuthKeyService {
    ServerAuthKey create(ServerAuthKeyCreateRequest request);

    ServerAuthKey update(Long id, ServerAuthKeyUpdateRequest request);

    void delete(Long id);

    List<ServerAuthKey> getKeys();

    ServerAuthKey validateActiveKey(String keyValue);
}
