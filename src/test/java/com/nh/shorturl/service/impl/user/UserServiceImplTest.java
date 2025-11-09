package com.nh.shorturl.service.impl.user;

import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.service.user.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("사용자 생성 시 중복 검증 후 저장한다")
    void createUser_persistsNewUser() {
        UserRequest request = new UserRequest();
        request.setUsername("my-service");

        given(userRepository.findByUsername("my-service")).willReturn(Optional.empty());
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .willAnswer(invocation -> {
                    User entity = invocation.getArgument(0);
                    entity.setId(10L);
                    return entity;
                });

        User created = userService.createUser(request);

        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getUsername()).isEqualTo("my-service");
        assertThat(created.getApiKey()).isNull();
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

        UserRequest request = new UserRequest();
        request.setUsername("dup");

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exists");
    }

    @Test
    @DisplayName("등록된 사용자 목록을 조회한다")
    void getAllUsers_returnsRepositoryResult() {
        User first = User.builder().id(1L).username("a").apiKey("key-a").build();
        User second = User.builder().id(2L).username("b").apiKey("key-b").build();
        given(userRepository.findAllByDeletedFalseOrderByCreatedAtDesc()).willReturn(List.of(first, second));

        List<User> users = userService.getAllUsers();

        assertThat(users).containsExactly(first, second);
    }

    @Test
    @DisplayName("사용자 삭제는 username으로 조회 후 soft delete 한다")
    void deleteUser_removesExistingUser() {
        User user = User.builder().id(5L).username("target").apiKey("key").build();
        given(userRepository.findByUsernameAndDeletedFalse("target")).willReturn(Optional.of(user));

        userService.deleteUser("target");

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("username으로 사용자 상세 정보를 조회한다")
    void getUser_returnsUser() {
        User user = User.builder().id(3L).username("detail").apiKey("key").build();
        given(userRepository.findByUsernameAndDeletedFalse("detail")).willReturn(Optional.of(user));

        User found = userService.getUser("detail");

        assertThat(found).isEqualTo(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 상세 조회 시 예외를 던진다")
    void getUser_throwsWhenNotFound() {
        given(userRepository.findByUsernameAndDeletedFalse("missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("삭제 대상이 없으면 예외를 던진다")
    void deleteUser_throwsWhenMissing() {
        given(userRepository.findByUsernameAndDeletedFalse("missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }
}
