package com.nh.shorturl.admin.redirect.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlLookupLoaderTest {

    private static final String PUBLIC_URL = "http://localhost:8080/r/";

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private Cache<String, Boolean> shortUrlMissingCache;

    private ShortUrlLookupLoader loader;

    @BeforeEach
    void setUp() {
        loader = new ShortUrlLookupLoader(shortUrlRepository, shortUrlMissingCache);
        ReflectionTestUtils.setField(loader, "publicUrl", PUBLIC_URL);
    }

    @Test
    void load_returnsResponse_forActiveEntity() {
        ShortUrl entity = ShortUrl.builder()
                .shortUrl("HIT00001")
                .longUrl("https://example.com")
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(entity, "id", 10L);
        when(shortUrlRepository.findByShortUrl("HIT00001")).thenReturn(Optional.of(entity));

        ShortUrlResponse response = loader.load("HIT00001");

        assertThat(response.getShortKey()).isEqualTo("HIT00001");
        assertThat(response.getShortUrl()).isEqualTo(PUBLIC_URL + "HIT00001");
        assertThat(response.getLongUrl()).isEqualTo("https://example.com");
        verify(shortUrlMissingCache, never()).put(anyString(), any(Boolean.class));
    }

    @Test
    void load_marksMissingAndThrows_whenNotFound() {
        when(shortUrlRepository.findByShortUrl("GONE0001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loader.load("GONE0001"))
                .isInstanceOf(ShortUrlLookupLoader.ShortUrlNotFoundException.class);

        verify(shortUrlMissingCache).put("GONE0001", Boolean.TRUE);
    }

    @Test
    void load_marksMissingAndThrows_whenExpired() {
        ShortUrl entity = ShortUrl.builder()
                .shortUrl("EXP00001")
                .longUrl("https://example.com")
                .expiredAt(LocalDateTime.now().minusSeconds(1))
                .build();
        ReflectionTestUtils.setField(entity, "id", 11L);
        when(shortUrlRepository.findByShortUrl("EXP00001")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> loader.load("EXP00001"))
                .isInstanceOf(ShortUrlLookupLoader.ShortUrlNotFoundException.class);

        verify(shortUrlMissingCache).put("EXP00001", Boolean.TRUE);
    }

}
