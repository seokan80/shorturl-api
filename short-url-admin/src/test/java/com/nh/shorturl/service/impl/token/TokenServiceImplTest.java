package com.nh.shorturl.service.impl.token;

import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.admin.repository.UserRepository;
import com.nh.shorturl.admin.service.token.TokenServiceImpl;
import com.nh.shorturl.admin.util.JwtProvider;
import com.nh.shorturl.dto.response.auth.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private TokenServiceImpl tokenService;

    @Test
    @DisplayName("토큰 발급 시 Access/Refresh Token을 저장한다")
    void issueToken_updatesUserWithTokens() {
        User user = User.builder()
                .id(1L)
                .username("my-service")
                .build();

        given(userRepository.findByUsername("my-service"))
                .willReturn(Optional.of(user));
        given(jwtProvider.createToken("my-service")).willReturn("issued-token");

        TokenResponse response = tokenService.issueToken("my-service");

        assertThat(response.getToken()).isEqualTo("issued-token");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(user.getApiKey()).isEqualTo("issued-token");
        assertThat(user.getRefreshToken()).isEqualTo(response.getRefreshToken());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("등록되지 않은 사용자에게 토큰 발급을 요청하면 예외를 던진다")
    void issueToken_throwsWhenUserMissing() {
        given(userRepository.findByUsername("missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.issueToken("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Refresh Token을 검증하고 새로운 토큰을 재발급한다")
    void reissueToken_updatesTokensWhenRefreshValid() {
        User user = User.builder()
                .id(2L)
                .username("service")
                .apiKey("old-token")
                .refreshToken("refresh-token")
                .build();

        given(userRepository.findByUsernameAndRefreshToken("service", "refresh-token"))
                .willReturn(Optional.of(user));
        given(jwtProvider.createToken("service")).willReturn("new-token");

        TokenResponse response = tokenService.reissueToken("service", "refresh-token");

        assertThat(response.getToken()).isEqualTo("new-token");
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(user.getApiKey()).isEqualTo("new-token");
        assertThat(user.getRefreshToken()).isEqualTo(response.getRefreshToken());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Refresh Token 검증에 실패하면 예외를 던진다")
    void reissueToken_throwsWhenRefreshInvalid() {
        given(userRepository.findByUsernameAndRefreshToken("service", "invalid"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.reissueToken("service", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid user or refresh token");
    }
}
