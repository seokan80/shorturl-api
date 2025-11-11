package com.nh.shorturl.admin.repository.history;

import com.nh.shorturl.type.GroupingType;

import java.util.List;

public interface RedirectionHistoryRepositoryCustom {
    List<Object[]> getStatsByShortUrlId(Long shortUrlId, List<GroupingType> groupBy);
}