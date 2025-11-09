package com.nh.shorturl.service.clientaccess;

import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyCreateRequest;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyUpdateRequest;
import com.nh.shorturl.entity.ClientAccessKey;

import java.util.List;

public interface ClientAccessKeyService {
    ClientAccessKey create(ClientAccessKeyCreateRequest request);

    ClientAccessKey update(Long id, ClientAccessKeyUpdateRequest request);

    void delete(Long id);

    List<ClientAccessKey> getKeys();

    ClientAccessKey validateActiveKey(String keyValue);
}
