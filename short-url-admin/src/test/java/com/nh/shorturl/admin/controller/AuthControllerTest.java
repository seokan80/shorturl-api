package com.nh.shorturl.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.admin.repository.ClientAccessKeyRepository;
import com.nh.shorturl.admin.repository.UserRepository;
import com.nh.shorturl.dto.request.auth.TokenIssueRequest;
import com.nh.shorturl.dto.request.auth.TokenReissueRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트.
 *
 * 테스트 데이터:
 * - @BeforeEach에서 생성하는 ClientAccessKey:
 *   - key: "dev-test-key-12345"
 *   - name: "Development Test Key"
 *
 * - @BeforeEach에서 생성하는 User:
 *   - username: "test-user", groupName: "test-group"
 *   - username: "admin-user", groupName: "admin-group"
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClientAccessKeyRepository clientAccessKeyRepository;

    // 테스트용 ClientAccessKey
    private static final String VALID_ACCESS_KEY = "dev-test-key-12345";
    private static final String INVALID_ACCESS_KEY = "invalid-key-xxx";

    // 테스트용 User
    private static final String TEST_USERNAME = "test-user";
    private static final String ADMIN_USERNAME = "admin-user";
    private static final String NON_EXISTENT_USERNAME = "non-existent-user";

    @BeforeEach
    void setUp() {
        // ClientAccessKey 생성
        ClientAccessKey clientAccessKey = ClientAccessKey.builder()
                .name("Development Test Key")
                .keyValue(VALID_ACCESS_KEY)
                .issuedBy("System")
                .description("Default client access key for development and testing purposes")
                .active(true)
                .deleted(false)
                .build();
        clientAccessKeyRepository.save(clientAccessKey);

        // 테스트용 User 생성
        User testUser = User.builder()
                .username(TEST_USERNAME)
                .groupName("test-group")
                .deleted(false)
                .build();
        userRepository.save(testUser);

        // 관리자용 User 생성
        User adminUser = User.builder()
                .username(ADMIN_USERNAME)
                .groupName("admin-group")
                .deleted(false)
                .build();
        userRepository.save(adminUser);
    }

    @Test
    @DisplayName("유효한 access-key로 JWT 토큰을 발급한다")
    void shouldIssueToken_whenValidAccessKey() throws Exception {
        // given: data.sql의 test-user 사용
        TokenIssueRequest request = new TokenIssueRequest();
        request.setUsername(TEST_USERNAME);

        // when & then
        mockMvc.perform(post("/api/auth/token/issue")
                        .header("X-CLIENTACCESS-KEY", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("access-key 헤더가 없으면 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorized_whenAccessKeyMissing() throws Exception {
        // given
        TokenIssueRequest request = new TokenIssueRequest();
        request.setUsername(TEST_USERNAME);

        // when & then
        mockMvc.perform(post("/api/auth/token/issue")
                        // X-CLIENTACCESS-KEY 헤더 없음
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // 헤더 누락 시 400
    }

    @Test
    @DisplayName("잘못된 access-key로 요청 시 UNAUTHORIZED를 반환한다")
    void shouldReturnUnauthorized_whenInvalidAccessKey() throws Exception {
        // given
        TokenIssueRequest request = new TokenIssueRequest();
        request.setUsername(TEST_USERNAME);

        // when & then
        mockMvc.perform(post("/api/auth/token/issue")
                        .header("X-CLIENTACCESS-KEY", INVALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9998")) // UNAUTHORIZED
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 토큰 발급 시 USER_NOT_FOUND를 반환한다")
    void shouldReturnUserNotFound_whenUserDoesNotExist() throws Exception {
        // given
        TokenIssueRequest request = new TokenIssueRequest();
        request.setUsername(NON_EXISTENT_USERNAME);

        // when & then
        mockMvc.perform(post("/api/auth/token/issue")
                        .header("X-CLIENTACCESS-KEY", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1001")) // USER_NOT_FOUND
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @DisplayName("유효한 refresh token으로 JWT 토큰을 재발급한다")
    void shouldReissueToken_whenValidRefreshToken() throws Exception {
        // given: 먼저 토큰 발급
        TokenIssueRequest issueRequest = new TokenIssueRequest();
        issueRequest.setUsername(TEST_USERNAME);

        String issueResponse = mockMvc.perform(post("/api/auth/token/issue")
                        .header("X-CLIENTACCESS-KEY", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 발급받은 refresh token 추출
        String refreshToken = objectMapper.readTree(issueResponse)
                .path("data")
                .path("refreshToken")
                .asText();

        // when: refresh token으로 재발급
        TokenReissueRequest reissueRequest = new TokenReissueRequest();
        reissueRequest.setUsername(TEST_USERNAME);
        reissueRequest.setRefreshToken(refreshToken);

        // then
        mockMvc.perform(post("/api/auth/token/re-issue")
                        .header("X-CLIENTACCESS-KEY", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("잘못된 refresh token으로 재발급 시 FORBIDDEN을 반환한다")
    void shouldReturnForbidden_whenInvalidRefreshToken() throws Exception {
        // given
        TokenReissueRequest request = new TokenReissueRequest();
        request.setUsername(TEST_USERNAME);
        request.setRefreshToken("invalid-refresh-token-xxx");

        // when & then
        mockMvc.perform(post("/api/auth/token/re-issue")
                        .header("X-CLIENTACCESS-KEY", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9997")) // FORBIDDEN
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    @DisplayName("username과 refresh token이 일치하지 않으면 FORBIDDEN을 반환한다")
    void shouldReturnForbidden_whenUsernameMismatch() throws Exception {
        // given: test-user로 토큰 발급
        TokenIssueRequest issueRequest = new TokenIssueRequest();
        issueRequest.setUsername(TEST_USERNAME);

        String issueResponse = mockMvc.perform(post("/api/auth/token/issue")
                        .header("X-CLIENTACCESS-KEY", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(issueResponse)
                .path("data")
                .path("refreshToken")
                .asText();

        // when: admin-user로 재발급 시도 (refresh token은 test-user의 것)
        TokenReissueRequest reissueRequest = new TokenReissueRequest();
        reissueRequest.setUsername(ADMIN_USERNAME);
        reissueRequest.setRefreshToken(refreshToken);

        // then
        mockMvc.perform(post("/api/auth/token/re-issue")
                        .header("X-CLIENTACCESS-KEY", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reissueRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9997")) // FORBIDDEN
                .andExpect(jsonPath("$.message").value("Forbidden"));
    }

    @Test
    @DisplayName("admin-user로 JWT 토큰을 발급한다")
    void shouldIssueToken_forAdminUser() throws Exception {
        // given: data.sql의 admin-user 사용
        TokenIssueRequest request = new TokenIssueRequest();
        request.setUsername(ADMIN_USERNAME);

        // when & then
        mockMvc.perform(post("/api/auth/token/issue")
                        .header("X-CLIENTACCESS-KEY", VALID_ACCESS_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }
}
