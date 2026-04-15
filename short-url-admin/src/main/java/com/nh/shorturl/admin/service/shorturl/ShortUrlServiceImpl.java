package com.nh.shorturl.admin.service.shorturl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.admin.util.Base62;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShortUrlServiceImpl implements ShortUrlService {
    private static final Logger log = LoggerFactory.getLogger(ShortUrlServiceImpl.class);

    private final ShortUrlRepository shortUrlRepository;
    private final LoadingCache<String, ShortUrlResponse> shortUrlCache;
    private final Cache<String, Boolean> shortUrlMissingCache;

    @Value("${short-url.redirect.public-url}")
    private String publicUrl;

    public ShortUrlServiceImpl(ShortUrlRepository shortUrlRepository,
            LoadingCache<String, ShortUrlResponse> shortUrlCache,
            Cache<String, Boolean> shortUrlMissingCache) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortUrlCache = shortUrlCache;
        this.shortUrlMissingCache = shortUrlMissingCache;
    }

    @Override
    @Transactional
    public ShortUrlResponse createShortUrl(ShortUrlRequest request) {
        String shortUrl;
        do {
            shortUrl = Base62.encodeUUID(UUID.randomUUID());
        } while (shortUrlRepository.existsByShortUrl(shortUrl));

        log.info("Creating Short URL, request: {}", request);

        ShortUrl entity = ShortUrl.builder()
                .shortUrl(shortUrl)
                .longUrl(request.getLongUrl())
                .expiredAt(LocalDateTime.now().plusDays(1L))
                .botType(request.getBotType())
                .botServiceKey(request.getBotServiceKey())
                .surveyId(request.getSurveyId())
                .surveyVer(request.getSurveyVer())
                .build();

        ShortUrl saved = shortUrlRepository.save(entity);
        ShortUrlResponse response = toResponse(saved);

        notifyCacheUpdate(response);

        return response;
    }

    @Override
    @Transactional
    public void deleteShortUrl(Long shortUrlId) {
        ShortUrl shortUrl = shortUrlRepository.findById(shortUrlId)
                .orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 단축 URL을 찾을 수 없습니다: " + shortUrlId));

        String shortUrlKey = shortUrl.getShortUrl();
        shortUrlRepository.delete(shortUrl);

        notifyCacheEviction(shortUrlKey);
    }

    @Override
    public ShortUrlResponse getShortUrl(Long id) {
        return shortUrlRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));
    }

    @Override
    public ShortUrlResponse getShortUrlByKey(String shortUrl) {
        ShortUrl entity = shortUrlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));

        if (isExpired(entity)) {
            throw new IllegalStateException("단축 URL이 만료되었습니다.");
        }

        return toResponse(entity);
    }

    @Override
    public String resolveOriginalUrl(String shortUrl) {
        ShortUrl entity = shortUrlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new IllegalArgumentException("URL not found"));

        if (isExpired(entity)) {
            throw new IllegalStateException("단축 URL이 만료되었습니다.");
        }

        return entity.getLongUrl();
    }

    @Override
    public List<ShortUrlResponse> findAllForCaching() {
        return shortUrlRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResultList<ShortUrlResponse> listShortUrls(Pageable pageable) {
        Page<ShortUrl> page = shortUrlRepository.findAll(pageable);
        List<ShortUrlResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return new ResultList<>(page.getTotalElements(), responses);
    }

    @Override
    @Transactional
    public ShortUrlResponse updateShortUrlExpiration(Long id, ShortUrlUpdateRequest request) {
        ShortUrl shortUrl = shortUrlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL을 찾을 수 없습니다. ID: " + id));

        shortUrl.setExpiredAt(request.getExpiredAt());
        ShortUrl updated = shortUrlRepository.save(shortUrl);

        ShortUrlResponse response = toResponse(updated);
        notifyCacheUpdate(response);

        return response;
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

    /** 쓰기 노드의 로컬 캐시를 즉시 최신화한다. 타 노드는 refreshAfterWrite(60s)로 수렴. */
    private void notifyCacheUpdate(ShortUrlResponse response) {
        shortUrlCache.put(response.getShortKey(), response);
        shortUrlMissingCache.invalidate(response.getShortKey());
    }

    /** 쓰기 노드의 로컬 캐시에서 즉시 제거하고 short TTL negative cache 에 기록한다. */
    private void notifyCacheEviction(String shortUrlKey) {
        shortUrlCache.invalidate(shortUrlKey);
        shortUrlMissingCache.put(shortUrlKey, Boolean.TRUE);
    }
}
