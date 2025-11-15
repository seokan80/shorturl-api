package com.nh.shorturl.admin.repository;

import com.nh.shorturl.type.GroupingType;

import java.util.List;

public interface RedirectionHistoryRepositoryCustom {
    List<Object[]> getStatsByShortUrlId(Long shortUrlId, List<GroupingType> groupBy);
}