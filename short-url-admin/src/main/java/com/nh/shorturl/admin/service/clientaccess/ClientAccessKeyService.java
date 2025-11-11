package com.nh.shorturl.admin.service.clientaccess;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyCreateRequest;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyUpdateRequest;

import java.util.List;

public interface ClientAccessKeyService {
    ClientAccessKey create(ClientAccessKeyCreateRequest request);

    ClientAccessKey update(Long id, ClientAccessKeyUpdateRequest request);

    void delete(Long id);

    List<ClientAccessKey> getKeys();

    ClientAccessKey validateActiveKey(String keyValue);
}
