package com.nh.shorturl.service.impl.history;

import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.entity.RedirectionHistory;
import com.nh.shorturl.entity.ShortUrl;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.ShortUrlRepository;
import com.nh.shorturl.repository.history.RedirectionHistoryRepository;
import com.nh.shorturl.service.history.RedirectionHistoryServiceImpl;
import com.nh.shorturl.type.GroupingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedirectionHistoryServiceImplTest {

    @Mock
    private RedirectionHistoryRepository redirectionHistoryRepository;

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @InjectMocks
    private RedirectionHistoryServiceImpl redirectionHistoryService;

    @Test
    @DisplayName("리다이렉션 카운트를 반환하기 전에 ShortUrl 존재 여부를 검증한다")
    void getRedirectCount_returnsSum() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(1L)
                .shortUrl("a1")
                .longUrl("https://example.com")
                .createBy("tester")
                .user(User.builder().id(10L).username("tester").apiKey("api").build())
                .build();

        given(shortUrlRepository.findById(1L)).willReturn(Optional.of(shortUrl));
        given(redirectionHistoryRepository.countByShortUrlId(1L)).willReturn(15L);

        int count = redirectionHistoryService.getRedirectCount(1L);

        assertThat(count).isEqualTo(15);
        verify(redirectionHistoryRepository).countByShortUrlId(1L);
    }

    @Test
    @DisplayName("그룹 기준에 맞춰 통계 데이터를 매핑한다")
    void getStats_formatsResults() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(2L)
                .shortUrl("a2")
                .longUrl("https://example.com")
                .createBy("tester")
                .user(User.builder().id(11L).username("tester").apiKey("api").build())
                .build();

        List<GroupingType> groupBy = List.of(GroupingType.REFERER, GroupingType.YEAR);
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(groupBy);

        List<Object[]> queryResults = List.<Object[]>of(new Object[]{"https://google.com", 2025, 5L});

        given(shortUrlRepository.findById(2L)).willReturn(Optional.of(shortUrl));
        given(redirectionHistoryRepository.getStatsByShortUrlId(2L, groupBy))
                .willReturn(queryResults);

        List<Map<String, Object>> stats = redirectionHistoryService.getStats(2L, request);

        assertThat(stats).hasSize(1);
        Map<String, Object> row = stats.get(0);
        assertThat(row.get("referer")).isEqualTo("https://google.com");
        assertThat(row.get("year")).isEqualTo(2025);
        assertThat(row.get("count")).isEqualTo(5L);
    }

    @Test
    @DisplayName("리다이렉션 이력을 저장할 때 요청 메타데이터를 캡처한다")
    void saveRedirectionHistory_persistsRequestInfo() {
        ShortUrl shortUrl = ShortUrl.builder()
                .id(3L)
                .shortUrl("abc12345")
                .longUrl("https://example.com")
                .createBy("tester")
                .user(User.builder().id(12L).username("tester").apiKey("api").build())
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "10.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        request.addHeader("Referer", "https://google.com");
        request.setRemoteAddr("127.0.0.1");

        given(shortUrlRepository.findByShortUrl("abc12345")).willReturn(Optional.of(shortUrl));

        redirectionHistoryService.saveRedirectionHistory("abc12345", request);

        ArgumentCaptor<RedirectionHistory> captor = ArgumentCaptor.forClass(RedirectionHistory.class);
        verify(redirectionHistoryRepository).save(captor.capture());
        RedirectionHistory history = captor.getValue();
        assertThat(history.getShortUrl()).isEqualTo(shortUrl);
        assertThat(history.getIp()).isEqualTo("10.0.0.1");
        assertThat(history.getReferer()).isEqualTo("https://google.com");
        assertThat(history.getUserAgent()).isEqualTo("JUnit");
        assertThat(history.getRedirectAt()).isNotNull();
    }

    @Test
    @DisplayName("저장 중 예외가 발생해도 호출자에게 전파하지 않는다")
    void saveRedirectionHistory_swallowExceptions() {
        given(shortUrlRepository.findByShortUrl("missing"))
                .willThrow(new RuntimeException("boom"));

        assertThatCode(() -> redirectionHistoryService.saveRedirectionHistory("missing", new MockHttpServletRequest()))
                .doesNotThrowAnyException();

        verify(redirectionHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
