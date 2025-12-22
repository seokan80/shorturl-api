package com.nh.shorturl.admin.service.history;

import com.nh.shorturl.admin.entity.RedirectionHistory;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.repository.RedirectionHistoryRepository;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.dto.response.history.RedirectionHistoryResponse;
import com.nh.shorturl.type.GroupingType;
import com.nh.shorturl.admin.util.UserAgentParser.UserAgentMetadata;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.nh.shorturl.admin.util.RequestInfoUtils.*;
import static com.nh.shorturl.admin.util.UserAgentParser.*;

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

        List<GroupingType> groupByEnums = Optional.ofNullable(request.getGroupBy()).orElseGet(List::of);
        if (CollectionUtils.isEmpty(groupByEnums)) {
            return List.of();
        }

        List<Object[]> results = redirectionHistoryRepository.getStatsByShortUrlId(shortUrlId, groupByEnums);

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

            String userAgent = getUserAgent(request);
            UserAgentMetadata userAgentMetadata = parse(userAgent);

            RedirectionHistory redirectionHistory = RedirectionHistory.builder()
                    .ip(getClientIp(request))
                    .shortUrl(shortUrlEntity)
                    .referer(getReferer(request))
                    .userAgent(userAgent)
                    .deviceType(userAgentMetadata.deviceType())
                    .os(userAgentMetadata.os())
                    .browser(userAgentMetadata.browser())
                    .country(getCountry(request))
                    .city(getCity(request))
                    .botType(shortUrlEntity.getBotType())
                    .botServiceKey(shortUrlEntity.getBotServiceKey())
                    .surveyId(shortUrlEntity.getSurveyId())
                    .surveyVer(shortUrlEntity.getSurveyVer())
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

        UserAgentMetadata userAgentMetadata = parse(request.getUserAgent());

        // 2. DTO의 정보를 바탕으로 RedirectionHistory 엔티티를 생성합니다.
        RedirectionHistory history = RedirectionHistory.builder()
                .shortUrl(shortUrl)
                .referer(request.getReferer())
                .userAgent(request.getUserAgent())
                .ip(request.getIp())
                .deviceType(userAgentMetadata.deviceType())
                .os(userAgentMetadata.os())
                .browser(userAgentMetadata.browser())
                .country(request.getCountry())
                .city(request.getCity())
                .botType(request.getBotType())
                .botServiceKey(request.getBotServiceKey())
                .surveyId(request.getSurveyId())
                .surveyVer(request.getSurveyVer())
                .redirectAt(LocalDateTime.now()) // 저장 시점의 시간 기록
                .build();

        // 3. 데이터베이스에 저장합니다.
        redirectionHistoryRepository.save(history);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RedirectionHistoryResponse> findAll(Pageable pageable) {
        return redirectionHistoryRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RedirectionHistoryResponse findById(Long id) {
        return redirectionHistoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("History not found: " + id));
    }

    private RedirectionHistoryResponse toResponse(RedirectionHistory history) {
        return RedirectionHistoryResponse.builder()
                .id(history.getId())
                .shortUrlId(history.getShortUrl().getId())
                .shortKey(history.getShortUrl().getShortUrl())
                .referer(history.getReferer())
                .userAgent(history.getUserAgent())
                .ip(history.getIp())
                .deviceType(history.getDeviceType())
                .os(history.getOs())
                .browser(history.getBrowser())
                .country(history.getCountry())
                .city(history.getCity())
                .botType(history.getBotType())
                .botServiceKey(history.getBotServiceKey())
                .surveyId(history.getSurveyId())
                .surveyVer(history.getSurveyVer())
                .redirectAt(history.getRedirectAt())
                .build();
    }
}
