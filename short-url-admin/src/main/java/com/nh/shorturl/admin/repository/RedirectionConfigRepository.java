package com.nh.shorturl.admin.repository;

import com.nh.shorturl.admin.entity.RedirectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RedirectionConfigRepository extends JpaRepository<RedirectionConfig, Long> {
    Optional<RedirectionConfig> findTopByOrderByIdAsc();
}
