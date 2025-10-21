package com.nh.shorturl.repository.history;

import com.nh.shorturl.type.GroupingType;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface RedirectionHistoryRepositoryCustom {
    List<Object[]> getStatsByShortUrlId(Long shortUrlId, List<GroupingType> groupBy);
}