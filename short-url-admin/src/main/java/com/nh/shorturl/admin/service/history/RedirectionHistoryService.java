package com.nh.shorturl.admin.service.history;

import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.dto.response.history.RedirectionHistoryResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface RedirectionHistoryService {
    int getRedirectCount(Long shortUrlId);

    void saveRedirectionHistory(String shortUrl, HttpServletRequest request);

    void saveRedirectionHistory(RedirectionHistoryRequest request);

    List<Map<String, Object>> getStats(Long shortUrlId, RedirectionStatsRequest request);

    Page<RedirectionHistoryResponse> findAll(Pageable pageable);

    RedirectionHistoryResponse findById(Long id);
}
