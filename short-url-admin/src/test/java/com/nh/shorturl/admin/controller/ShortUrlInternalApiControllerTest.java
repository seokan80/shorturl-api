package com.nh.shorturl.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.admin.entity.ShortUrl;
import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.admin.repository.ShortUrlRepository;
import com.nh.shorturl.admin.repository.UserRepository;
import com.nh.shorturl.dto.request.history.RedirectionHistoryRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ShortUrlInternalApiController 통합 테스트.
 *
 * 이 컨트롤러는 short-url-redirect 모듈과의 내부 통신을 위한 API를 제공합니다.
 * SecurityConfig에서 /api/internal/** 경로는 permitAll()로 설정되어 있어 별도 인증이 필요하지 않습니다.
 *
 * 테스트 데이터:
 * - data.sql에서 로드되는 User:
 *   - username: "test-user", groupName: "test-group"
 *   - username: "admin-user", groupName: "admin-group"
 *
 * - @BeforeEach에서 생성하는 ShortUrl:
 *   - shortUrl: "test-key-123"
 *   - longUrl: "https://example.com/original"
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShortUrlInternalApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortUrlRepository shortUrlRepository;

    @Autowired
    private UserRepository userRepository;

    // data.sql에 정의된 테스트용 User
    private static final String TEST_USERNAME = "test-user";
    private static final String TEST_SHORT_URL_KEY = "test-key-123";

    private ShortUrl testShortUrl;

    @BeforeEach
    void setUp() {
        // data.sql에서 로드된 test-user 사용
        User testUser = userRepository.findByUsername(TEST_USERNAME)
                .orElseThrow(() -> new IllegalStateException("test-user not found in database"));

        // 테스트용 ShortUrl 생성 및 저장
        testShortUrl = ShortUrl.builder()
                .shortUrl(TEST_SHORT_URL_KEY)
                .longUrl("https://example.com/original")
                .user(testUser)
                .createBy(TEST_USERNAME)
                .expiredAt(LocalDateTime.now().plusDays(30))
                .deleted(false)
                .build();
        shortUrlRepository.save(testShortUrl);
    }

    // =========================
    // GET /api/internal/short-urls/{shortUrlKey}
    // =========================

    @Test
    @DisplayName("[Internal API] 존재하는 단축키로 ShortUrl을 조회한다")
    void shouldReturnShortUrl_whenValidShortUrlKeyProvided() throws Exception {
        // given: setUp()에서 생성한 testShortUrl 사용
        // shortUrlKey = "test-key-123"

        // when & then
        mockMvc.perform(get("/api/internal/short-urls/{shortUrlKey}", TEST_SHORT_URL_KEY))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testShortUrl.getId()))
                .andExpect(jsonPath("$.shortUrl").value(TEST_SHORT_URL_KEY))
                .andExpect(jsonPath("$.longUrl").value("https://example.com/original"))
                .andExpect(jsonPath("$.username").value(TEST_USERNAME));
    }

    @Test
    @DisplayName("[Internal API] 존재하지 않는 단축키로 조회 시 예외를 반환한다")
    void shouldReturnError_whenShortUrlKeyNotFound() throws Exception {
        // given
        String nonExistentKey = "non-existent-key-xxx";

        // when & then
        // ShortUrlService.getShortUrlByKey()가 예외를 던지면 에러 응답
        mockMvc.perform(get("/api/internal/short-urls/{shortUrlKey}", nonExistentKey))
                .andDo(print())
                .andExpect(status().is4xxClientError()); // 404 또는 400 등 에러 상태
    }

    @Test
    @DisplayName("[Internal API] 삭제된 ShortUrl 조회 시 조회되지 않는다")
    void shouldNotReturnDeletedShortUrl() throws Exception {
        // given: ShortUrl을 삭제 상태로 변경
        testShortUrl.setDeleted(true);
        testShortUrl.setDeletedAt(LocalDateTime.now());
        shortUrlRepository.save(testShortUrl);

        // when & then: 삭제된 URL은 조회 불가 (SQLRestriction으로 인해)
        mockMvc.perform(get("/api/internal/short-urls/{shortUrlKey}", TEST_SHORT_URL_KEY))
                .andDo(print())
                .andExpect(status().is4xxClientError()); // NOT_FOUND 또는 에러
    }

    @Test
    @DisplayName("[Internal API] 인증 없이 internal API 호출이 가능하다 (permitAll)")
    void shouldAllowAccessWithoutAuthentication() throws Exception {
        // given: 인증 헤더 없음
        // /api/internal/** 경로는 SecurityConfig에서 permitAll()

        // when & then
        mockMvc.perform(get("/api/internal/short-urls/{shortUrlKey}", TEST_SHORT_URL_KEY))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value(TEST_SHORT_URL_KEY));
    }

    // =========================
    // POST /api/internal/redirection-histories
    // =========================

    @Test
    @DisplayName("[Internal API] 유효한 리디렉션 히스토리 요청을 저장한다")
    void shouldSaveRedirectionHistory_whenValidRequest() throws Exception {
        // given: RedirectionHistoryRequest 생성
        RedirectionHistoryRequest request = RedirectionHistoryRequest.builder()
                .shortUrlKey(TEST_SHORT_URL_KEY)
                .ip("192.168.1.100")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .referer("https://google.com")
                .build();

        // when & then: 히스토리 저장 성공
        mockMvc.perform(post("/api/internal/redirection-histories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk()); // ResponseEntity.ok().build() → 200 OK
    }

    @Test
    @DisplayName("[Internal API] shortUrlKey 없이 히스토리 저장 시 에러를 반환한다")
    void shouldReturnError_whenShortUrlKeyMissing() throws Exception {
        // given: shortUrlKey 없는 요청
        RedirectionHistoryRequest request = RedirectionHistoryRequest.builder()
                .ip("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .referer("https://google.com")
                .build();

        // when & then: Service에서 예외 발생 예상
        mockMvc.perform(post("/api/internal/redirection-histories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError()); // 400 또는 500
    }

    @Test
    @DisplayName("[Internal API] 존재하지 않는 shortUrlKey로 히스토리 저장 시 에러를 반환한다")
    void shouldReturnError_whenSavingHistoryForNonExistentShortUrl() throws Exception {
        // given: 존재하지 않는 shortUrlKey
        RedirectionHistoryRequest request = RedirectionHistoryRequest.builder()
                .shortUrlKey("non-existent-key")
                .ip("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .referer("https://google.com")
                .build();

        // when & then
        mockMvc.perform(post("/api/internal/redirection-histories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError()); // NOT_FOUND 또는 에러
    }

    @Test
    @DisplayName("[Internal API] 필수 필드가 모두 포함된 히스토리를 저장한다")
    void shouldSaveHistoryWithAllFields() throws Exception {
        // given: 모든 필드가 포함된 요청
        RedirectionHistoryRequest request = RedirectionHistoryRequest.builder()
                .shortUrlKey(TEST_SHORT_URL_KEY)
                .ip("127.0.0.1")
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                .referer("https://naver.com")
                .build();

        // when & then
        mockMvc.perform(post("/api/internal/redirection-histories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // =========================
    // GET /api/internal/short-urls/all
    // =========================

    @Test
    @DisplayName("[Internal API] 캐싱용 전체 활성 ShortUrl 목록을 조회한다")
    void shouldReturnAllActiveShortUrls_forCaching() throws Exception {
        // given: setUp()에서 생성한 testShortUrl + 추가 URL 생성
        User testUser = userRepository.findByUsername(TEST_USERNAME).orElseThrow();

        ShortUrl additionalUrl = ShortUrl.builder()
                .shortUrl("test-key-456")
                .longUrl("https://example.com/another")
                .user(testUser)
                .createBy(TEST_USERNAME)
                .expiredAt(LocalDateTime.now().plusDays(60))
                .deleted(false)
                .build();
        shortUrlRepository.save(additionalUrl);

        // when & then: 모든 활성 URL 조회
        mockMvc.perform(get("/api/internal/short-urls/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)) // testShortUrl + additionalUrl
                .andExpect(jsonPath("$[0].shortUrl").exists())
                .andExpect(jsonPath("$[0].longUrl").exists())
                .andExpect(jsonPath("$[1].shortUrl").exists());
    }

    @Test
    @DisplayName("[Internal API] 삭제된 ShortUrl은 전체 목록에서 제외된다")
    void shouldExcludeDeletedShortUrls_inCachingList() throws Exception {
        // given: testShortUrl을 삭제 상태로 변경
        testShortUrl.setDeleted(true);
        testShortUrl.setDeletedAt(LocalDateTime.now());
        shortUrlRepository.save(testShortUrl);

        // when & then: 삭제된 URL은 목록에서 제외 (SQLRestriction)
        mockMvc.perform(get("/api/internal/short-urls/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0)); // 삭제되어 빈 목록
    }

    @Test
    @DisplayName("[Internal API] ShortUrl이 없을 때 빈 배열을 반환한다")
    void shouldReturnEmptyArray_whenNoShortUrlsExist() throws Exception {
        // given: 모든 ShortUrl 삭제
        shortUrlRepository.deleteAll();

        // when & then
        mockMvc.perform(get("/api/internal/short-urls/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("[Internal API] 캐싱 목록 조회 시 인증이 필요하지 않다")
    void shouldAllowCachingListAccessWithoutAuthentication() throws Exception {
        // given: 인증 헤더 없음 (permitAll)

        // when & then
        mockMvc.perform(get("/api/internal/short-urls/all"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
