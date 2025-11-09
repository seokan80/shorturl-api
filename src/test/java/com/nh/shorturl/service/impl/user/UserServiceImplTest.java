package com.nh.shorturl.service.impl.user;

import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.service.user.UserServiceImpl;
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
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("사용자 생성 시 JWT 기반 API Key를 저장한다")
    void createUser_generatesJwtApiKey() {
        given(userRepository.findByUsername("my-service")).willReturn(Optional.empty());
        given(jwtProvider.createToken("my-service")).willReturn("jwt-token");
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willAnswer(invocation -> {
                    User entity = invocation.getArgument(0);
                    entity.setId(10L);
                    return entity;
                });

        User created = userService.createUser("my-service");

        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getUsername()).isEqualTo("my-service");
        assertThat(created.getApiKey()).isEqualTo("jwt-token");
        verify(userRepository).save(created);
    }

    @Test
    @DisplayName("이미 존재하는 사용자명을 등록하면 예외를 던진다")
    void createUser_throwsWhenDuplicate() {
        User existing = User.builder()
                .id(1L)
                .username("dup")
                .apiKey("old")
                .build();

        given(userRepository.findByUsername("dup")).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.createUser("dup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exists");
    }

    @Test
    @DisplayName("username/API Key 조합 조회는 Repository 결과를 그대로 반환한다")
    void findByUsernameAndApiKey_delegatesToRepository() {
        User user = User.builder()
                .id(2L)
                .username("service")
                .apiKey("key")
                .build();

        given(userRepository.findByUsernameAndApiKey("service", "key"))
                .willReturn(Optional.of(user));

        Optional<User> result = userService.findByUsernameAndApiKey("service", "key");

        assertThat(result).contains(user);
    }
}
