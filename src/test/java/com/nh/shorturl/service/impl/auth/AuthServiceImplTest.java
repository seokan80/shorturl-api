package com.nh.shorturl.service.impl.auth;

import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.service.auth.AuthServiceImpl;
import com.nh.shorturl.util.JwtProvider;
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
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("API Key 재발급 시 사용자 키를 업데이트하고 저장한다")
    void reissueToken_updatesApiKey() {
        User user = User.builder()
                .id(1L)
                .username("my-service")
                .apiKey("old-key")
                .build();

        given(userRepository.findByUsernameAndApiKey("my-service", "old-key"))
                .willReturn(Optional.of(user));
        given(jwtProvider.createToken("my-service")).willReturn("new-key");

        String newToken = authService.reissueToken("my-service", "old-key");

        assertThat(newToken).isEqualTo("new-key");
        assertThat(user.getApiKey()).isEqualTo("new-key");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("username/API Key 조합이 없으면 예외를 던진다")
    void reissueToken_throwsWhenUserNotFound() {
        given(userRepository.findByUsernameAndApiKey("missing", "bad-key"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reissueToken("missing", "bad-key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid");
    }
}
