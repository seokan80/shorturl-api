package com.nh.shorturl.admin.service.shorturl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceImplTest {

    private static final String PUBLIC_URL = "http://localhost:8080/r/";

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private LoadingCache<String, ShortUrlResponse> shortUrlCache;

    @Mock
    private Cache<String, Boolean> shortUrlMissingCache;

    private ShortUrlServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ShortUrlServiceImpl(shortUrlRepository, shortUrlCache, shortUrlMissingCache);
        ReflectionTestUtils.setField(service, "publicUrl", PUBLIC_URL);
        ReflectionTestUtils.setField(service, "defaultExpirationDays", 1L);
    }

    @Test
    void createShortUrl_persistsEntityAndPrimesCache() {
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://example.com/some/very/long/path");

        when(shortUrlRepository.existsByShortUrl(anyString())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 42L);
            return entity;
        });

        ShortUrlResponse response = service.createShortUrl(request);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getShortKey()).isNotBlank();
        assertThat(response.getShortUrl()).isEqualTo(PUBLIC_URL + response.getShortKey());
        assertThat(response.getLongUrl()).isEqualTo(request.getLongUrl());

        verify(shortUrlRepository).save(any(ShortUrl.class));
        verify(shortUrlCache).put(response.getShortKey(), response);
        verify(shortUrlMissingCache).invalidate(response.getShortKey());
    }

    @Test
    void createShortUrl_usesValidDaysWhenProvided() {
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://example.com");
        request.setValidDays(7);

        when(shortUrlRepository.existsByShortUrl(anyString())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate today = LocalDate.now();
        service.createShortUrl(request);

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(captor.capture());
        LocalDateTime expiredAt = captor.getValue().getExpiredAt();
        LocalDateTime expected = today.plusDays(7).atTime(LocalTime.MAX);
        LocalDateTime expectedIfMidnightCrossed = today.plusDays(8).atTime(LocalTime.MAX);
        assertThat(expiredAt).isIn(expected, expectedIfMidnightCrossed);
    }

    @Test
    void createShortUrl_validDaysTakesPrecedenceOverExpireDate() {
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://example.com");
        request.setValidDays(3);
        request.setExpireDate(LocalDateTime.now().plusYears(10));

        when(shortUrlRepository.existsByShortUrl(anyString())).thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createShortUrl(request);

        ArgumentCaptor<ShortUrl> captor = ArgumentCaptor.forClass(ShortUrl.class);
        verify(shortUrlRepository).save(captor.capture());
        LocalDateTime expiredAt = captor.getValue().getExpiredAt();
        assertThat(expiredAt).isBefore(LocalDate.now().plusDays(5).atTime(LocalTime.MAX));
        assertThat(expiredAt).isAfter(LocalDateTime.now().plusDays(2));
    }

    @Test
    void createShortUrl_rejectsNonPositiveValidDays() {
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://example.com");
        request.setValidDays(0);

        when(shortUrlRepository.existsByShortUrl(anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.createShortUrl(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validDays");
    }

    @Test
    void createShortUrl_retriesOnCollision() {
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://example.com");

        when(shortUrlRepository.existsByShortUrl(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(shortUrlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShortUrlResponse response = service.createShortUrl(request);

        assertThat(response.getShortKey()).isNotBlank();
        verify(shortUrlRepository, times(2)).existsByShortUrl(anyString());
    }

    @Test
    void getShortUrlByKey_returnsResponseForActiveEntity() {
        ShortUrl entity = ShortUrl.builder()
                .shortUrl("ABC12345")
                .longUrl("https://example.com")
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(entity, "id", 1L);

        when(shortUrlRepository.findByShortUrl("ABC12345")).thenReturn(Optional.of(entity));

        ShortUrlResponse response = service.getShortUrlByKey("ABC12345");

        assertThat(response.getShortKey()).isEqualTo("ABC12345");
        assertThat(response.getLongUrl()).isEqualTo("https://example.com");
    }

    @Test
    void getShortUrlByKey_throwsWhenExpired() {
        ShortUrl entity = ShortUrl.builder()
                .shortUrl("EXPIRED0")
                .longUrl("https://example.com")
                .expiredAt(LocalDateTime.now().minusSeconds(1))
                .build();
        ReflectionTestUtils.setField(entity, "id", 2L);

        when(shortUrlRepository.findByShortUrl("EXPIRED0")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.getShortUrlByKey("EXPIRED0"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("만료");
    }

    @Test
    void getShortUrlByKey_throwsWhenMissing() {
        when(shortUrlRepository.findByShortUrl("MISSING0")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getShortUrlByKey("MISSING0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteShortUrl_evictsPositiveCacheAndMarksNegative() {
        ShortUrl entity = ShortUrl.builder()
                .shortUrl("DEL12345")
                .longUrl("https://example.com")
                .build();
        ReflectionTestUtils.setField(entity, "id", 7L);

        when(shortUrlRepository.findById(7L)).thenReturn(Optional.of(entity));

        service.deleteShortUrl(7L);

        verify(shortUrlRepository).delete(entity);
        verify(shortUrlCache).invalidate("DEL12345");
        verify(shortUrlMissingCache).put("DEL12345", Boolean.TRUE);
    }

    @Test
    void deleteShortUrl_throwsWhenEntityMissing() {
        when(shortUrlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteShortUrl(99L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(shortUrlCache, never()).invalidate(anyString());
        verify(shortUrlMissingCache, never()).put(anyString(), any());
    }

    @Test
    void updateShortUrlExpiration_updatesEntityAndRefreshesCache() {
        LocalDateTime newExpiry = LocalDateTime.now().plusDays(7);
        ShortUrl entity = ShortUrl.builder()
                .shortUrl("UPD12345")
                .longUrl("https://example.com")
                .expiredAt(LocalDateTime.now().plusDays(1))
                .build();
        ReflectionTestUtils.setField(entity, "id", 5L);

        ShortUrlUpdateRequest request = new ShortUrlUpdateRequest();
        request.setExpiredAt(newExpiry);

        when(shortUrlRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(shortUrlRepository.save(entity)).thenReturn(entity);

        ShortUrlResponse response = service.updateShortUrlExpiration(5L, request);

        assertThat(entity.getExpiredAt()).isEqualTo(newExpiry);
        assertThat(response.getShortKey()).isEqualTo("UPD12345");
        verify(shortUrlCache).put("UPD12345", response);
        verify(shortUrlMissingCache).invalidate("UPD12345");
    }
}
