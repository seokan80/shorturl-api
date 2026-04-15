package com.nh.shorturl.admin.redirect.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Caffeine LoadingCache 에서 호출되는 로더.
 * DB 를 직접 조회하며, 존재하지 않거나 만료된 경우 negative cache 에 기록하고
 * {@link ShortUrlNotFoundException} 을 던진다. Caffeine LoadingCache 는 null 반환을
 * 허용하지 않으므로 예외로 "없음" 을 표현한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShortUrlLookupLoader {

    private final ShortUrlRepository shortUrlRepository;
    private final Cache<String, Boolean> shortUrlMissingCache;

    @Value("${short-url.redirect.public-url}")
    private String publicUrl;

    @Transactional(readOnly = true)
    public ShortUrlResponse load(String shortUrlKey) {
        Optional<ShortUrl> found = shortUrlRepository.findByShortUrl(shortUrlKey);
        if (found.isEmpty()) {
            shortUrlMissingCache.put(shortUrlKey, Boolean.TRUE);
            log.debug("ShortUrl not found, marked missing: {}", shortUrlKey);
            throw new ShortUrlNotFoundException(shortUrlKey);
        }
        ShortUrl entity = found.get();
        if (isExpired(entity)) {
            shortUrlMissingCache.put(shortUrlKey, Boolean.TRUE);
            log.debug("ShortUrl expired, marked missing: {}", shortUrlKey);
            throw new ShortUrlNotFoundException(shortUrlKey);
        }
        return toResponse(entity);
    }

    private boolean isExpired(ShortUrl entity) {
        return entity.getExpiredAt() != null && entity.getExpiredAt().isBefore(LocalDateTime.now());
    }

    private ShortUrlResponse toResponse(ShortUrl entity) {
        return ShortUrlResponse.builder()
                .id(entity.getId())
                .shortKey(entity.getShortUrl())
                .shortUrl(publicUrl + entity.getShortUrl())
                .longUrl(entity.getLongUrl())
                .createdAt(entity.getCreatedAt())
                .expiredAt(entity.getExpiredAt() != null ? entity.getExpiredAt().toString() : null)
                .botType(entity.getBotType())
                .botServiceKey(entity.getBotServiceKey())
                .surveyId(entity.getSurveyId())
                .surveyVer(entity.getSurveyVer())
                .build();
    }

    /** CacheLoader 에서 "없음/만료" 를 표현하기 위한 sentinel 예외. */
    public static class ShortUrlNotFoundException extends RuntimeException {
        public ShortUrlNotFoundException(String key) {
            super(key);
        }
    }
}
