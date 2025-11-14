package com.nh.shorturl.admin.service.history;

import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.history.RedirectionHistoryResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface RedirectionHistoryService {
    int getRedirectCount(Long shortUrlId);

    void saveRedirectionHistory(String shortUrl, HttpServletRequest request);

    void saveRedirectionHistory(RedirectionHistoryRequest request);

    List<Map<String, Object>> getStats(Long shortUrlId, RedirectionStatsRequest request);

    /**
     * 리다이렉션 히스토리 목록 조회 (페이징).
     */
    ResultList<RedirectionHistoryResponse> listRedirectionHistories(Pageable pageable);

    /**
     * 리다이렉션 히스토리 상세 조회.
     */
    RedirectionHistoryResponse getRedirectionHistory(Long id);
}
