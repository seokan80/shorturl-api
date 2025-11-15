package com.nh.shorturl.admin.repository;

import com.nh.shorturl.admin.entity.RedirectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedirectionHistoryRepository extends JpaRepository<RedirectionHistory, Long>, RedirectionHistoryRepositoryCustom {
    long countByShortUrlId(Long shortUrlId);
}