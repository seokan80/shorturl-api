package com.nh.shorturl.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.admin.entity.RedirectionHistory;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.admin.repository.RedirectionHistoryRepository;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.admin.repository.UserRepository;
import com.nh.shorturl.dto.request.history.RedirectionStatsRequest;
import com.nh.shorturl.type.GroupingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RedirectionHistoryController 통합 테스트.
 *
 * 이 컨트롤러는 단축 URL의 리디렉션 이력 조회 및 통계를 제공합니다.
 * SecurityConfig에서 /r/** 경로는 permitAll()로 설정되어 있어 별도 인증이 필요하지 않습니다.
 *
 * 테스트 데이터:
 * - data.sql에서 로드되는 User:
 *   - username: "test-user", groupName: "test-group"
 *
 * - @BeforeEach에서 생성하는 데이터:
 *   - ShortUrl: "test-key-history"
 *   - RedirectionHistory: 3개의 리디렉션 이력
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RedirectionHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedirectionHistoryRepository redirectionHistoryRepository;

    // data.sql에 정의된 테스트용 User
    private static final String TEST_USERNAME = "test-user";

    private ShortUrl testShortUrl;
    private Long testShortUrlId;

    @BeforeEach
    void setUp() {
        // data.sql에서 로드된 test-user 사용
        User testUser = userRepository.findByUsername(TEST_USERNAME)
                .orElseThrow(() -> new IllegalStateException("test-user not found in database"));

        // 테스트용 ShortUrl 생성
        testShortUrl = ShortUrl.builder()
                .shortUrl("test-key-history")
                .longUrl("https://example.com/history-test")
                .user(testUser)
                .createBy(TEST_USERNAME)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .deleted(false)
                .build();
        shortUrlRepository.save(testShortUrl);
        testShortUrlId = testShortUrl.getId();

        // 테스트용 RedirectionHistory 생성 (3개)
        LocalDateTime now = LocalDateTime.now();

        RedirectionHistory history1 = RedirectionHistory.builder()
                .shortUrl(testShortUrl)
                .ip("192.168.1.1")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .referer("https://google.com")
                .redirectAt(now.minusHours(3))
                .build();

        RedirectionHistory history2 = RedirectionHistory.builder()
                .shortUrl(testShortUrl)
                .ip("192.168.1.2")
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                .referer("https://naver.com")
                .redirectAt(now.minusHours(2))
                .build();

        RedirectionHistory history3 = RedirectionHistory.builder()
                .shortUrl(testShortUrl)
                .ip("192.168.1.3")
                .userAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)")
                .referer("https://daum.net")
                .redirectAt(now.minusHours(1))
                .build();

        redirectionHistoryRepository.save(history1);
        redirectionHistoryRepository.save(history2);
        redirectionHistoryRepository.save(history3);
    }

    // =========================
    // GET /r/history/{shortUrlId}/count
    // =========================

    @Test
    @DisplayName("[History] 존재하는 ShortUrl의 리디렉션 횟수를 조회한다")
    void shouldReturnRedirectCount_whenShortUrlExists() throws Exception {
        // given: setUp()에서 생성한 testShortUrl (리디렉션 3회)

        // when & then
        mockMvc.perform(get("/r/history/{shortUrlId}/count", testShortUrlId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value(3)); // 3개의 히스토리
    }

    @Test
    @DisplayName("[History] 리디렉션 이력이 없는 ShortUrl의 count는 0이다")
    void shouldReturnZeroCount_whenNoHistoryExists() throws Exception {
        // given: 새로운 ShortUrl 생성 (히스토리 없음)
        User testUser = userRepository.findByUsername(TEST_USERNAME).orElseThrow();

        ShortUrl newShortUrl = ShortUrl.builder()
                .shortUrl("no-history-key")
                .longUrl("https://example.com/no-history")
                .user(testUser)
                .createBy(TEST_USERNAME)
                .deleted(false)
                .build();
        shortUrlRepository.save(newShortUrl);

        // when & then
        mockMvc.perform(get("/r/history/{shortUrlId}/count", newShortUrl.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").value(0)); // 히스토리 없음
    }

    @Test
    @DisplayName("[History] 존재하지 않는 ShortUrlId로 count 조회 시 0을 반환한다")
    void shouldReturnZeroCount_whenShortUrlIdNotFound() throws Exception {
        // given
        Long nonExistentId = 99999L;

        // when & then: 존재하지 않는 ID는 count = 0 반환 (또는 404)
        mockMvc.perform(get("/r/history/{shortUrlId}/count", nonExistentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0)); // 또는 에러 처리
    }

    @Test
    @DisplayName("[History] count 조회 시 인증이 필요하지 않다 (permitAll)")
    void shouldAllowCountAccessWithoutAuthentication() throws Exception {
        // given: 인증 헤더 없음
        // /r/** 경로는 SecurityConfig에서 permitAll()

        // when & then
        mockMvc.perform(get("/r/history/{shortUrlId}/count", testShortUrlId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    // =========================
    // POST /r/history/{shortUrlId}/stats
    // =========================

    @Test
    @DisplayName("[Stats] 그룹핑 없이 통계를 조회한다")
    void shouldReturnStats_withoutGrouping() throws Exception {
        // given: 그룹핑 없는 요청
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        // groupBy를 설정하지 않음

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", testShortUrlId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[Stats] REFERER 기준으로 그룹핑한 통계를 조회한다")
    void shouldReturnStatsGroupedByReferer() throws Exception {
        // given: REFERER 기준 그룹핑
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(Arrays.asList(GroupingType.REFERER));

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", testShortUrlId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[Stats] USER_AGENT 기준으로 그룹핑한 통계를 조회한다")
    void shouldReturnStatsGroupedByUserAgent() throws Exception {
        // given: USER_AGENT 기준 그룹핑
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(Arrays.asList(GroupingType.USER_AGENT));

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", testShortUrlId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[Stats] DAY 기준으로 그룹핑한 통계를 조회한다")
    void shouldReturnStatsGroupedByDay() throws Exception {
        // given: DAY 기준 그룹핑
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(Arrays.asList(GroupingType.DAY));

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", testShortUrlId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[Stats] 여러 기준으로 그룹핑한 통계를 조회한다")
    void shouldReturnStatsWithMultipleGroupings() throws Exception {
        // given: REFERER와 USER_AGENT 기준 그룹핑
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(Arrays.asList(GroupingType.REFERER, GroupingType.USER_AGENT));

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", testShortUrlId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[Stats] 이력이 없는 ShortUrl의 통계는 빈 배열을 반환한다")
    void shouldReturnEmptyStats_whenNoHistoryExists() throws Exception {
        // given: 히스토리 없는 새 ShortUrl
        User testUser = userRepository.findByUsername(TEST_USERNAME).orElseThrow();

        ShortUrl newShortUrl = ShortUrl.builder()
                .shortUrl("no-stats-key")
                .longUrl("https://example.com/no-stats")
                .user(testUser)
                .createBy(TEST_USERNAME)
                .deleted(false)
                .build();
        shortUrlRepository.save(newShortUrl);

        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(Arrays.asList(GroupingType.REFERER));

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", newShortUrl.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0)); // 빈 배열
    }

    @Test
    @DisplayName("[Stats] 존재하지 않는 ShortUrlId로 통계 조회 시 빈 배열을 반환한다")
    void shouldReturnEmptyStats_whenShortUrlIdNotFound() throws Exception {
        // given
        Long nonExistentId = 99999L;
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(Arrays.asList(GroupingType.REFERER));

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0)); // 또는 404
    }

    @Test
    @DisplayName("[Stats] 통계 조회 시 인증이 필요하지 않다 (permitAll)")
    void shouldAllowStatsAccessWithoutAuthentication() throws Exception {
        // given: 인증 헤더 없음
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        request.setGroupBy(Arrays.asList(GroupingType.DAY));

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", testShortUrlId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("[Stats] Request body가 비어있어도 통계를 조회한다")
    void shouldReturnStats_whenRequestBodyIsEmpty() throws Exception {
        // given: 빈 요청 객체
        RedirectionStatsRequest request = new RedirectionStatsRequest();
        // groupBy를 설정하지 않음

        // when & then
        mockMvc.perform(post("/r/history/{shortUrlId}/stats", testShortUrlId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").isArray());
    }
}
