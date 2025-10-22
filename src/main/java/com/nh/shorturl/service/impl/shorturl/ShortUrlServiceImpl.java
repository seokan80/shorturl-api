package com.nh.shorturl.service.impl.shorturl;

import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.entity.ShortUrl;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.ShortUrlRepository;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.service.shorturl.ShortUrlService;
import com.nh.shorturl.util.Base62;
import com.nh.shorturl.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortUrlServiceImpl implements ShortUrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final UserRepository userRepository;

    @Value("${base-url}")
    String baseUrl;

    @Override
    public ShortUrlResponse createShortUrl(ShortUrlRequest request, String username) {
        // 중복 방지를 위해 UUID → Base62 8자리 인코딩
        String shortUrl;
        do {
            shortUrl = Base62.encodeUUID(UUID.randomUUID());
        } while (shortUrlRepository.existsByShortUrl(shortUrl));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자"));

        ShortUrl entity = ShortUrl.builder()
                .shortUrl(shortUrl)
                .longUrl(request.getLongUrl())
                .createdAt(LocalDateTime.now())
                .createBy(user.getUsername())
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(1L))
                .build();

        shortUrlRepository.save(entity);

        return toResponse(entity);
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
    public void deleteShortUrl(Long id) {
        shortUrlRepository.deleteById(id);
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