package com.nh.shorturl.admin.service.shorturl;

import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.admin.repository.UserRepository;
import com.nh.shorturl.admin.util.Base62;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShortUrlServiceImpl implements ShortUrlService {
    private static final Logger log = LoggerFactory.getLogger(ShortUrlServiceImpl.class);

    private final ShortUrlRepository shortUrlRepository;
    private final WebClient redirectApiClient; // redirect 모듈 호출용 WebClient 주입
    private final UserRepository userRepository;

    @Value("${base-url}")
    private String baseUrl;

    public ShortUrlServiceImpl(ShortUrlRepository shortUrlRepository, WebClient redirectApiClient, UserRepository userRepository) {
        this.shortUrlRepository = shortUrlRepository;
        this.redirectApiClient = redirectApiClient;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ShortUrlResponse createShortUrl(ShortUrlRequest request, String username) {
        // 중복 방지를 위해 UUID → Base62 8자리 인코딩
        String shortUrl;
        do {
            shortUrl = Base62.encodeUUID(UUID.randomUUID());
        } while (shortUrlRepository.existsByShortUrl(shortUrl));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자"));

        ShortUrl newShortUrl = ShortUrl.builder()
                .shortUrl(shortUrl)
                .longUrl(request.getLongUrl())
                .createBy(user.getUsername())
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(1L))
                .build();

        ShortUrl savedShortUrl = shortUrlRepository.save(newShortUrl);

        ShortUrlResponse response = ShortUrlResponse.builder()
                .longUrl(savedShortUrl.getLongUrl())
                .shortUrl(savedShortUrl.getShortUrl())
                .build();

        // redirect 모듈에 캐시 업데이트 요청
        notifyCacheUpdate(response);
        
        return response;
    }

    // (updateShortUrl 메소드가 있다면 동일하게 notifyCacheUpdate(response) 호출)

    @Override
    @Transactional
    public void deleteShortUrl(Long shortUrlId) {
        ShortUrl shortUrl = shortUrlRepository.findById(shortUrlId)
                .orElseThrow(() -> new IllegalArgumentException("ID에 해당하는 단축 URL을 찾을 수 없습니다: " + shortUrlId));
        
        String shortUrlKey = shortUrl.getShortUrl();
        shortUrlRepository.delete(shortUrl);

        // redirect 모듈에 캐시 삭제 요청
        notifyCacheEviction(shortUrlKey);
    }
    
    private void notifyCacheUpdate(ShortUrlResponse response) {
        redirectApiClient.put()
                .uri("/api/internal/cache/short-urls")
                .body(Mono.just(response), ShortUrlResponse.class)
                .retrieve()
                .toBodilessEntity()
                .doOnError(e -> log.error("Failed to notify cache update for {}", response.getShortUrl(), e))
                .subscribe();
    }
    
    private void notifyCacheEviction(String shortUrlKey) {
        redirectApiClient.delete()
                .uri("/api/internal/cache/short-urls/{shortUrlKey}", shortUrlKey)
                .retrieve()
                .toBodilessEntity()
                .doOnError(e -> log.error("Failed to notify cache eviction for {}", shortUrlKey, e))
                .subscribe();
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

//    @Override
//    public void deleteShortUrl(Long id) {
//        ShortUrl shortUrl = shortUrlRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("삭제할 URL을 찾을 수 없습니다. ID: " + id));
//
//        shortUrlRepository.delete(shortUrl);
//    }

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
                .map(shortUrl -> ShortUrlResponse.builder()
                        .longUrl(shortUrl.getLongUrl())
                        .shortUrl(shortUrl.getShortUrl())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 만료 여부 확인.
     */
    private boolean isExpired(ShortUrl entity) {
        return entity.getExpiredAt() != null && entity.getExpiredAt().isBefore(LocalDateTime.now());
    }

    /**
     * Entity → DTO 변환.
     */
    private ShortUrlResponse toResponse(ShortUrl entity) {
        return ShortUrlResponse.builder()
                .id(entity.getId())
                .shortKey(entity.getShortUrl())
                .shortUrl(baseUrl + entity.getShortUrl())
                .longUrl(entity.getLongUrl())
                .createdBy(entity.getCreateBy())
                .userId(entity.getUser().getId())
                .createdAt(entity.getCreatedAt())
                .expiredAt(entity.getExpiredAt() != null ? entity.getExpiredAt().toString() : null)
                .build();
    }
}