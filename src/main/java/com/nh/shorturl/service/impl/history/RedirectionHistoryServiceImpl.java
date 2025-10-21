package com.nh.shorturl.service.impl.history;

import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.entity.ShortUrl;
import com.nh.shorturl.entity.history.RedirectionHistory;
import com.nh.shorturl.repository.ShortUrlRepository;
import com.nh.shorturl.repository.history.RedirectionHistoryRepository;
import com.nh.shorturl.service.history.RedirectionHistoryService;
import com.nh.shorturl.type.GroupingType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nh.shorturl.util.RequestInfoUtils.*;

@Service
@RequiredArgsConstructor
public class RedirectionHistoryServiceImpl implements RedirectionHistoryService {

    private static final Logger log = LoggerFactory.getLogger(RedirectionHistoryServiceImpl.class);

    private final RedirectionHistoryRepository redirectionHistoryRepository;

    private final ShortUrlRepository shortUrlRepository;

    @Override
    public int getRedirectCount(Long shortUrlId) {
        log.info("getRedirectCount: shortUrlId={}", shortUrlId);

        shortUrlRepository.findById(shortUrlId)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));

        return (int) redirectionHistoryRepository.countByShortUrlId(shortUrlId);
    }

    @Override
    public List<Map<String, Object>> getStats(Long shortUrlId, RedirectionStatsRequest request) {
        log.info("getStats: shortUrlId={}, groupBy={}", shortUrlId, request.getGroupBy());

        shortUrlRepository.findById(shortUrlId)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));

        List<Object[]> results = redirectionHistoryRepository.getStatsByShortUrlId(shortUrlId, request.getGroupBy());
        List<GroupingType> groupByEnums = request.getGroupBy();

        List<Map<String, Object>> stats = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> statRow = new LinkedHashMap<>();
            for (int i = 0; i < groupByEnums.size(); i++) {
                statRow.put(groupByEnums.get(i).name().toLowerCase(), row[i]);
            }
            statRow.put("count", row[groupByEnums.size()]);
            stats.add(statRow);
        }
        return stats;
    }

    @Override
    public void saveRedirectionHistory(String shortUrl, HttpServletRequest request) {
        log.info("saveRedirectionHistory: shortUrl={}, request={}", shortUrl, request);

        try {
            ShortUrl shortUrlEntity = shortUrlRepository.findByShortUrl(shortUrl)
                    .orElseThrow(() -> new IllegalArgumentException("URL not found"));

            RedirectionHistory redirectionHistory = RedirectionHistory.builder()
                    .ip(getClientIp(request))
                    .shortUrl(shortUrlEntity)
                    .referer(getReferer(request))
                    .userAgent(getUserAgent(request))
                    .redirectAt(LocalDateTime.now())
                    .build();

            redirectionHistoryRepository.save(redirectionHistory);
        } catch (Exception e) {
            log.error("saveRedirectionHistory error: {}", e.getMessage(), e);
        }
    }
}