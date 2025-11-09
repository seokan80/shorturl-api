package com.nh.shorturl.repository;

import com.nh.shorturl.entity.ClientAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientAccessKeyRepository extends JpaRepository<ClientAccessKey, Long> {
    Optional<ClientAccessKey> findByKeyValueAndDeletedFalse(String keyValue);

    List<ClientAccessKey> findAllByDeletedFalseOrderByCreatedAtDesc();
}
