package com.nh.shorturl.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.admin.util.JwtTestHelper;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ShortUrlController 통합 테스트.
 *
 * 테스트 데이터:
 * - data.sql에서 로드되는 User:
 *   - username: "test-user", groupName: "test-group"
 *   - username: "admin-user", groupName: "admin-group"
 *
 * - data.sql에서 로드되는 ClientAccessKey:
 *   - key: "dev-test-key-12345"
 *
 * JWT 토큰 생성:
 * - JwtTestHelper를 사용하여 테스트용 JWT 토큰 생성
 * - Secret: application.yml의 jwt.secret 사용
 * - Algorithm: HS512
 * - Expiration: 1시간 (3600000ms)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // data.sql에 정의된 테스트용 User
    private static final String TEST_USERNAME = "test-user";
    private static final String ADMIN_USERNAME = "admin-user";

    // data.sql에 정의된 테스트용 ClientAccessKey
    private static final String VALID_ACCESS_KEY = "dev-test-key-12345";
    private static final String INVALID_ACCESS_KEY = "invalid-access-key";

    // =========================
    // JWT 인증 기반 테스트
    // =========================

    @Test
    @DisplayName("[JWT] 유효한 JWT 토큰으로 단축 URL을 생성한다")
    void shouldCreateShortUrl_whenValidJwtToken() throws Exception {
        // given: JWT 토큰 생성 (test-user)
        String authHeader = JwtTestHelper.createAuthorizationHeader(TEST_USERNAME);

        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/very/long/url/path");
        request.setExpireDate(LocalDateTime.now().plusDays(7));

        // when & then
        mockMvc.perform(post("/api/short-url")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.shortUrl").exists())
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com/very/long/url/path"))
                .andExpect(jsonPath("$.data.username").value(TEST_USERNAME));
    }

    @Test
    @DisplayName("[JWT] JWT 토큰이 없으면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorized_whenJwtMissing() throws Exception {
        // given
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test");

        // when & then
        mockMvc.perform(post("/api/short-url")
                        // Authorization 헤더 없음
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9998")); // UNAUTHORIZED
    }

    @Test
    @DisplayName("[JWT] 잘못된 서명의 JWT 토큰으로 요청 시 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorized_whenInvalidSignatureToken() throws Exception {
        // given: 잘못된 서명의 토큰
        String invalidAuthHeader = JwtTestHelper.createInvalidSignatureAuthorizationHeader(TEST_USERNAME);

        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test");

        // when & then
        mockMvc.perform(post("/api/short-url")
                        .header("Authorization", invalidAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9998")); // UNAUTHORIZED
    }

    @Test
    @DisplayName("[JWT] 만료된 JWT 토큰으로 요청 시 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorized_whenExpiredToken() throws Exception {
        // given: 만료된 토큰
        String expiredAuthHeader = JwtTestHelper.createExpiredAuthorizationHeader(TEST_USERNAME);

        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test");

        // when & then
        mockMvc.perform(post("/api/short-url")
                        .header("Authorization", expiredAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9998")); // UNAUTHORIZED
    }

    @Test
    @DisplayName("[JWT] 생성한 단축 URL을 ID로 조회한다")
    void shouldGetShortUrl_byId() throws Exception {
        // given: 먼저 단축 URL 생성
        String authHeader = JwtTestHelper.createAuthorizationHeader(TEST_USERNAME);

        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test-get-by-id");

        MvcResult createResult = mockMvc.perform(post("/api/short-url")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // 생성된 ID 추출
        Long shortUrlId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        // when & then: ID로 조회
        mockMvc.perform(get("/api/short-url/{id}", shortUrlId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.id").value(shortUrlId))
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com/test-get-by-id"));
    }

    @Test
    @DisplayName("[JWT] 생성한 단축 URL을 단축키로 조회한다")
    void shouldGetShortUrl_byKey() throws Exception {
        // given: 먼저 단축 URL 생성
        String authHeader = JwtTestHelper.createAuthorizationHeader(TEST_USERNAME);

        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test-get-by-key");

        MvcResult createResult = mockMvc.perform(post("/api/short-url")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // 생성된 단축키 추출
        String shortUrlKey = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("shortUrl")
                .asText();

        // when & then: 단축키로 조회
        mockMvc.perform(get("/api/short-url/key/{shortUrl}", shortUrlKey))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.shortUrl").value(shortUrlKey))
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com/test-get-by-key"));
    }

    @Test
    @DisplayName("[JWT] 존재하지 않는 ID로 조회 시 NOT_FOUND를 반환한다")
    void shouldReturnNotFound_whenInvalidId() throws Exception {
        // given
        Long nonExistentId = 99999L;

        // when & then
        mockMvc.perform(get("/api/short-url/{id}", nonExistentId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9996")); // NOT_FOUND
    }

    @Test
    @DisplayName("[JWT] 자신이 생성한 단축 URL을 삭제한다")
    void shouldDeleteShortUrl_whenOwner() throws Exception {
        // given: 먼저 단축 URL 생성
        String authHeader = JwtTestHelper.createAuthorizationHeader(TEST_USERNAME);

        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test-delete");

        MvcResult createResult = mockMvc.perform(post("/api/short-url")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Long shortUrlId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        // when & then: 삭제
        mockMvc.perform(post("/api/short-url/delete/{id}", shortUrlId)
                        .header("Authorization", authHeader))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));
    }

    @Test
    @DisplayName("[JWT] JWT 토큰 없이 삭제 시도 시 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorized_whenDeleteWithoutJwt() throws Exception {
        // given
        Long anyId = 1L;

        // when & then
        mockMvc.perform(post("/api/short-url/delete/{id}", anyId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9998")); // UNAUTHORIZED
    }

    @Test
    @DisplayName("[JWT] 단축 URL 목록을 페이징하여 조회한다")
    void shouldListShortUrls_withPaging() throws Exception {
        // given: 여러 개의 단축 URL 생성
        String authHeader = JwtTestHelper.createAuthorizationHeader(TEST_USERNAME);

        for (int i = 1; i <= 3; i++) {
            ShortUrlRequest request = new ShortUrlRequest();
            request.setLongUrl("https://www.example.com/test-" + i);

            mockMvc.perform(post("/api/short-url")
                            .header("Authorization", authHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        // when & then: 목록 조회 (page=0, size=10)
        mockMvc.perform(get("/api/short-url")
                        .header("Authorization", authHeader)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

//     @Test
//     @DisplayName("[JWT] 단축 URL의 만료일을 수정한다")
//     void shouldUpdateExpiration_whenOwner() throws Exception {
//         // given: 단축 URL 생성
//         String authHeader = JwtTestHelper.createAuthorizationHeader(TEST_USERNAME);
// 
//         ShortUrlRequest request = new ShortUrlRequest();
//         request.setLongUrl("https://www.example.com/test-update");
// 
//         MvcResult createResult = mockMvc.perform(post("/api/short-url")
//                         .header("Authorization", authHeader)
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(request)))
//                 .andExpect(status().isOk())
//                 .andReturn();
// 
//         Long shortUrlId = objectMapper.readTree(createResult.getResponse().getContentAsString())
//                 .path("data")
//                 .path("id")
//                 .asLong();
// 
//         // when: 만료일 수정
//         ShortUrlUpdateRequest updateRequest = new ShortUrlUpdateRequest();
//         updateRequest.setExpireDate(LocalDateTime.now().plusDays(30));
// 
//         // then
//         mockMvc.perform(put("/api/short-url/{id}/expiration", shortUrlId)
//                         .header("Authorization", authHeader)
//                         .contentType(MediaType.APPLICATION_JSON)
//                         .content(objectMapper.writeValueAsString(updateRequest)))
//                 .andDo(print())
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.code").value("0000"))
//                 .andExpect(jsonPath("$.data.id").value(shortUrlId));
//     }
// 
    // =========================
    // ClientAccessKey (x-access-key) 인증 기반 테스트
    // =========================

    @Test
    @DisplayName("[AccessKey] 유효한 x-access-key로 단축 URL을 생성한다")
    void shouldCreateShortUrl_whenValidAccessKey() throws Exception {
        // given
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test-access-key");

        // when & then
        // ClientAccessKeyValidationFilter가 x-access-key 헤더를 검증
        mockMvc.perform(post("/api/short-url")
                        .header("x-access-key", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.shortUrl").exists())
                .andExpect(jsonPath("$.data.originalUrl").value("https://www.example.com/test-access-key"));
    }

    @Test
    @DisplayName("[AccessKey] 잘못된 x-access-key로 요청 시 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorized_whenInvalidAccessKey() throws Exception {
        // given
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test");

        // when & then
        mockMvc.perform(post("/api/short-url")
                        .header("x-access-key", INVALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9998")); // UNAUTHORIZED
    }

    @Test
    @DisplayName("[AccessKey] x-access-key 헤더 없이 요청 시 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorized_whenAccessKeyMissing() throws Exception {
        // given
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test");

        // when & then
        mockMvc.perform(post("/api/short-url")
                        // x-access-key 헤더 없음, Authorization 헤더도 없음
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9998")); // UNAUTHORIZED
    }

    // =========================
    // Validation 및 예외 케이스 테스트
    // =========================

    @Test
    @DisplayName("[Validation] originalUrl이 없으면 Bad Request를 반환한다")
    void shouldReturnBadRequest_whenOriginalUrlMissing() throws Exception {
        // given: originalUrl 없는 요청
        String authHeader = JwtTestHelper.createAuthorizationHeader(TEST_USERNAME);

        ShortUrlRequest request = new ShortUrlRequest();
        // originalUrl을 설정하지 않음

        // when & then
        mockMvc.perform(post("/api/short-url")
                        .header("Authorization", authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9999")); // FAIL 또는 validation error
    }

    @Test
    @DisplayName("[Permission] 다른 사용자가 생성한 URL은 삭제할 수 없다")
    void shouldReturnForbidden_whenDeletingOthersUrl() throws Exception {
        // given: test-user가 URL 생성
        String testUserAuthHeader = JwtTestHelper.createAuthorizationHeader(TEST_USERNAME);

        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://www.example.com/test-permission");

        MvcResult createResult = mockMvc.perform(post("/api/short-url")
                        .header("Authorization", testUserAuthHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Long shortUrlId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        // when & then: admin-user가 삭제 시도
        String adminAuthHeader = JwtTestHelper.createAuthorizationHeader(ADMIN_USERNAME);

        mockMvc.perform(post("/api/short-url/delete/{id}", shortUrlId)
                        .header("Authorization", adminAuthHeader))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9997")); // FORBIDDEN
    }
}
