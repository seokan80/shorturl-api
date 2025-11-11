package com.nh.shorturl.admin.service.history;

import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface RedirectionHistoryService {
    int getRedirectCount(Long shortUrlId);

    void saveRedirectionHistory(String shortUrl, HttpServletRequest request);

    void saveRedirectionHistory(RedirectionHistoryRequest request);

    List<Map<String, Object>> getStats(Long shortUrlId, RedirectionStatsRequest request);
}
