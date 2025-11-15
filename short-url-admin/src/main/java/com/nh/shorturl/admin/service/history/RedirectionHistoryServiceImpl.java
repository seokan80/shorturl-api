package com.nh.shorturl.admin.service.history;

import com.nh.shorturl.admin.entity.RedirectionHistory;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.repository.RedirectionHistoryRepository;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.type.GroupingType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.nh.shorturl.admin.util.RequestInfoUtils.*;

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
    @Override
    @Transactional
    public void saveRedirectionHistory(RedirectionHistoryRequest request) {
        // 1. DTO에 담겨온 shortUrlKey로 ShortUrl 엔티티를 찾습니다.
        ShortUrl shortUrl = shortUrlRepository.findByShortUrl(request.getShortUrlKey())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 단축 URL 키입니다: " + request.getShortUrlKey()));

        // 2. DTO의 정보를 바탕으로 RedirectionHistory 엔티티를 생성합니다.
        RedirectionHistory history = RedirectionHistory.builder()
                .shortUrl(shortUrl)
                .referer(request.getReferer())
                .userAgent(request.getUserAgent())
                .ip(request.getIp())
                .redirectAt(LocalDateTime.now()) // 저장 시점의 시간 기록
                .build();

        // 3. 데이터베이스에 저장합니다.
        redirectionHistoryRepository.save(history);
    }
}