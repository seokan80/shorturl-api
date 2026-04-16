package com.nh.shorturl.admin.service.history;

import com.nh.shorturl.admin.entity.RedirectionHistory;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.repository.RedirectionHistoryRepository;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.type.GroupingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedirectionHistoryServiceImplTest {

    @Mock
    private RedirectionHistoryRepository redirectionHistoryRepository;

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @InjectMocks
    private RedirectionHistoryServiceImpl service;

    private ShortUrl shortUrlEntity;

    @BeforeEach
    void setUp() {
        shortUrlEntity = ShortUrl.builder()
                .shortUrl("HIST0001")
                .longUrl("https://example.com")
                .build();
        ReflectionTestUtils.setField(shortUrlEntity, "id", 3L);
    }

    @Test
    void getRedirectCount_returnsCount_whenShortUrlExists() {
        when(shortUrlRepository.findById(3L)).thenReturn(Optional.of(shortUrlEntity));
        when(redirectionHistoryRepository.countByShortUrlId(3L)).thenReturn(42L);

        int count = service.getRedirectCount(3L);

        assertThat(count).isEqualTo(42);
    }

    @Test
    void getRedirectCount_throws_whenShortUrlMissing() {
        when(shortUrlRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRedirectCount(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getStats_returnsEmpty_whenGroupByIsEmpty() {
        when(shortUrlRepository.findById(3L)).thenReturn(Optional.of(shortUrlEntity));
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(List.of());

        List<Map<String, Object>> stats = service.getStats(3L, request);

        assertThat(stats).isEmpty();
    }

    @Test
    void getStats_mapsRowsWithLowerCaseKeys() {
        when(shortUrlRepository.findById(3L)).thenReturn(Optional.of(shortUrlEntity));
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(List.of(GroupingType.REFERER));

        List<Object[]> rows = List.of(
                new Object[]{"https://naver.com", 10L},
                new Object[]{"https://google.com", 5L}
        );
        when(redirectionHistoryRepository.getStatsByShortUrlId(3L, List.of(GroupingType.REFERER)))
                .thenReturn(rows);

        List<Map<String, Object>> stats = service.getStats(3L, request);

        assertThat(stats).hasSize(2);
        assertThat(stats.get(0)).containsEntry("referer", "https://naver.com").containsEntry("count", 10L);
        assertThat(stats.get(1)).containsEntry("referer", "https://google.com").containsEntry("count", 5L);
    }

    @Test
    void saveRedirectionHistoryRequest_persists_whenKeyResolved() {
        RedirectionHistoryRequest request = RedirectionHistoryRequest.builder()
                .shortUrlKey("HIST0001")
                .ip("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .referer("https://naver.com")
                .build();

        when(shortUrlRepository.findByShortUrl("HIST0001")).thenReturn(Optional.of(shortUrlEntity));

        service.saveRedirectionHistory(request);

        ArgumentCaptor<RedirectionHistory> captor = ArgumentCaptor.forClass(RedirectionHistory.class);
        verify(redirectionHistoryRepository).save(captor.capture());
        RedirectionHistory saved = captor.getValue();
        assertThat(saved.getShortUrl()).isSameAs(shortUrlEntity);
        assertThat(saved.getIp()).isEqualTo("127.0.0.1");
        assertThat(saved.getReferer()).isEqualTo("https://naver.com");
        assertThat(saved.getRedirectAt()).isNotNull();
    }

    @Test
    void saveRedirectionHistoryRequest_throws_whenKeyUnknown() {
        RedirectionHistoryRequest request = RedirectionHistoryRequest.builder()
                .shortUrlKey("MISSING0")
                .userAgent("Mozilla/5.0")
                .build();

        when(shortUrlRepository.findByShortUrl("MISSING0")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveRedirectionHistory(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
