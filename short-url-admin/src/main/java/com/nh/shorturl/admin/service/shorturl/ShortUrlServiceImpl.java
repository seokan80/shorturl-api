package com.nh.shorturl.admin.service.shorturl;

import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.admin.util.Base62;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 단축 URL CRUD 서비스.
 *
 * <p>캐시는 redirect 서버(short-url-redirect)가 독립적으로 관리한다.
 * admin 은 생성·수정·삭제 후 redirect 서버의 /api/internal/cache/short-urls 를 호출해
 * 캐시를 동기화해야 한다(현재는 redirect 서버가 폴링으로 수렴).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShortUrlServiceImpl implements ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;

    @Value("${short-url.redirect.public-url}")
    private String publicUrl;

    @Value("${short-url.default-expiration-days:1}")
    private long defaultExpirationDays;

    @Override
    @Transactional
    public ShortUrlResponse createShortUrl(ShortUrlRequest request) {
        String shortUrl;
        do {
            shortUrl = Base62.encodeUUID(UUID.randomUUID());
        } while (shortUrlRepository.existsByShortUrl(shortUrl));

        log.info("Creating Short URL, request: {}", request);

        LocalDateTime expiredAt = resolveExpiration(request);

        ShortUrl entity = ShortUrl.builder()
                .shortUrl(shortUrl)
                .longUrl(request.getLongUrl())
                .expiredAt(expiredAt)
                .build();

        ShortUrl saved = shortUrlRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteShortUrl(Long shortUrlId) {
        ShortUrl shortUrl = shortUrlRepository.findById(shortUrlId)
                .orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 단축 URL을 찾을 수 없습니다: " + shortUrlId));
        shortUrlRepository.delete(shortUrl);
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
                .filter(e -> !isExpired(e))
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
        return toResponse(updated);
    }

    /**
     * 만료 시각 결정 우선순위:
     * 1) validDays 지정 → 오늘 기준 N 일 뒤 23:59:59
     * 2) expireDate 지정 → 절대 시각 그대로
     * 3) 미지정 → default-expiration-days 적용
     */
    private LocalDateTime resolveExpiration(ShortUrlRequest request) {
        if (request.getValidDays() != null) {
            if (request.getValidDays() <= 0) {
                throw new IllegalArgumentException("validDays 는 1 이상이어야 합니다.");
            }
            return endOfDayAfter(request.getValidDays());
        }
        if (request.getExpireDate() != null) {
            return request.getExpireDate();
        }
        return endOfDayAfter(defaultExpirationDays);
    }

    private LocalDateTime endOfDayAfter(long days) {
        return LocalDate.now().plusDays(days).atTime(LocalTime.MAX);
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
                .build();
    }
}
