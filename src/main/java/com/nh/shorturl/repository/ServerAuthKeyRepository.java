package com.nh.shorturl.repository;

import com.nh.shorturl.entity.ServerAuthKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServerAuthKeyRepository extends JpaRepository<ServerAuthKey, Long> {
    Optional<ServerAuthKey> findByKeyValueAndDeletedFalse(String keyValue);

    List<ServerAuthKey> findAllByDeletedFalseOrderByCreatedAtDesc();
}
