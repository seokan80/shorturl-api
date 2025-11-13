package com.nh.shorturl.service.shorturl;

import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.entity.ClientAccessKey;
import com.nh.shorturl.entity.ShortUrl;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.ShortUrlRepository;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.util.Base62;
import lombok.RequiredArgsConstructor;
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
                .createBy(user.getUsername())
                .user(user)
                .expiredAt(LocalDateTime.now().plusDays(1L))
                .build();

        shortUrlRepository.save(entity);

        return toResponse(entity);
    }

    @Override
    @Transactional
    public ShortUrlResponse createShortUrlForClient(ShortUrlRequest request, ClientAccessKey clientAccessKey) {
        // 중복 방지를 위해 UUID → Base62 8자리 인코딩
        String shortUrl;
        do {
            shortUrl = Base62.encodeUUID(UUID.randomUUID());
        } while (shortUrlRepository.existsByShortUrl(shortUrl));

        // anonymous 사용자 조회 또는 생성
        User anonymousUser = userRepository.findByUsername("anonymous")
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .username("anonymous")
                            .groupName("anonymous")
                            .build();
                    return userRepository.save(newUser);
                });

        ShortUrl entity = ShortUrl.builder()
                .shortUrl(shortUrl)
                .longUrl(request.getLongUrl())
                .createBy("anonymous")
                .user(anonymousUser)
                .clientAccessKey(clientAccessKey)
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
    public void deleteShortUrl(Long id, String username) {
        ShortUrl shortUrl = shortUrlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 URL을 찾을 수 없습니다. ID: " + id));

        if (username == null || !username.equals(shortUrl.getCreateBy())) {
            throw new IllegalStateException("자신이 생성한 URL만 삭제할 수 있습니다.");
        }

        shortUrlRepository.delete(shortUrl);
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
    public ResultList<ShortUrlResponse> listShortUrls(Pageable pageable, String username) {
        Page<ShortUrl> page;

        if (username != null) {
            // 로그인한 사용자의 경우 자신이 생성한 URL만 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 사용자"));
            page = shortUrlRepository.findByUser(user, pageable);
        } else {
            // 비로그인 또는 전체 조회
            page = shortUrlRepository.findAll(pageable);
        }

        List<ShortUrlResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new ResultList<>(page.getTotalElements(), responses);
    }

    @Override
    @Transactional
    public ShortUrlResponse updateShortUrlExpiration(Long id, ShortUrlUpdateRequest request, String username) {
        ShortUrl shortUrl = shortUrlRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("URL을 찾을 수 없습니다. ID: " + id));

        // 권한 확인: 자신이 생성한 URL만 수정 가능
        if (username != null && !username.equals(shortUrl.getCreateBy())) {
            throw new IllegalStateException("자신이 생성한 URL만 수정할 수 있습니다.");
        }

        // 만료일 수정
        shortUrl.setExpiredAt(request.getExpiredAt());
        ShortUrl updated = shortUrlRepository.save(shortUrl);

        return toResponse(updated);
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
