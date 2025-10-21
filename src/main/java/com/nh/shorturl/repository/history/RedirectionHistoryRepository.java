package com.nh.shorturl.repository.history;

import com.nh.shorturl.entity.history.RedirectionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface RedirectionHistoryRepository extends JpaRepository<RedirectionHistory, Long>, RedirectionHistoryRepositoryCustom {
    long countByShortUrlId(Long shortUrlId);
}