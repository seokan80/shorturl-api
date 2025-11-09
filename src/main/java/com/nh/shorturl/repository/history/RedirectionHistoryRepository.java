package com.nh.shorturl.repository.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nh.shorturl.entity.RedirectionHistory;

public interface RedirectionHistoryRepository extends JpaRepository<RedirectionHistory, Long>, RedirectionHistoryRepositoryCustom {
    long countByShortUrlId(Long shortUrlId);
}