package com.nh.shorturl.admin.redirect.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlLookupServiceTest {

    @Mock
    private LoadingCache<String, ShortUrlResponse> shortUrlCache;

    @Mock
    private Cache<String, Boolean> shortUrlMissingCache;

    private ShortUrlLookupService service;

    @BeforeEach
    void setUp() {
        service = new ShortUrlLookupService(shortUrlCache, shortUrlMissingCache);
    }

    @Test
    void findByKey_returnsNullImmediately_whenNegativeCacheHit() {
        when(shortUrlMissingCache.getIfPresent("MISS0001")).thenReturn(Boolean.TRUE);

        ShortUrlResponse result = service.findByKey("MISS0001");

        assertThat(result).isNull();
        verify(shortUrlCache, never()).get("MISS0001");
    }

    @Test
    void findByKey_returnsCachedValue_whenPositiveCacheHit() {
        ShortUrlResponse cached = ShortUrlResponse.builder()
                .shortKey("HIT00001")
                .longUrl("https://example.com")
                .build();
        when(shortUrlMissingCache.getIfPresent("HIT00001")).thenReturn(null);
        when(shortUrlCache.get("HIT00001")).thenReturn(cached);

        ShortUrlResponse result = service.findByKey("HIT00001");

        assertThat(result).isSameAs(cached);
    }

    @Test
    void findByKey_returnsNull_whenLoaderThrowsNotFoundDirectly() {
        when(shortUrlMissingCache.getIfPresent("GONE0001")).thenReturn(null);
        when(shortUrlCache.get("GONE0001"))
                .thenThrow(new ShortUrlLookupLoader.ShortUrlNotFoundException("GONE0001"));

        ShortUrlResponse result = service.findByKey("GONE0001");

        assertThat(result).isNull();
    }

    @Test
    void findByKey_returnsNull_whenNotFoundWrappedInCompletionException() {
        when(shortUrlMissingCache.getIfPresent("GONE0002")).thenReturn(null);
        when(shortUrlCache.get("GONE0002"))
                .thenThrow(new CompletionException(new ShortUrlLookupLoader.ShortUrlNotFoundException("GONE0002")));

        ShortUrlResponse result = service.findByKey("GONE0002");

        assertThat(result).isNull();
    }

    @Test
    void findByKey_rethrows_whenCompletionExceptionWrapsUnknownCause() {
        when(shortUrlMissingCache.getIfPresent("BOOM0001")).thenReturn(null);
        CompletionException ex = new CompletionException(new IllegalStateException("db down"));
        when(shortUrlCache.get("BOOM0001")).thenThrow(ex);

        assertThatThrownBy(() -> service.findByKey("BOOM0001"))
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
