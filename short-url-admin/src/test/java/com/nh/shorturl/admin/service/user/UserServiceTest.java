package com.nh.shorturl.admin.service.user;

import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.admin.repository.UserRepository;
import com.nh.shorturl.dto.request.auth.UserRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserServiceImpl(userRepository);

    @Test
    @DisplayName("사용자를 생성한다")
    void createUser() {
        // given
        UserRequest request = new UserRequest();
        request.setUsername("test-user");
        String groupName = "test-group";

        User savedUser = User.builder()
                .id(1L)
                .username("test-user")
                .groupName(groupName)
                .build();

        when(userRepository.findByUsername("test-user")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        User result = userService.createUser(request, groupName);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("test-user");
        assertThat(result.getGroupName()).isEqualTo(groupName);

        verify(userRepository).findByUsername("test-user");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("중복된 사용자명으로 생성 시 예외가 발생한다")
    void createUserDuplicateUsername() {
        // given
        UserRequest request = new UserRequest();
        request.setUsername("duplicate-user");

        User existingUser = User.builder()
                .id(1L)
                .username("duplicate-user")
                .build();

        when(userRepository.findByUsername("duplicate-user")).thenReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> userService.createUser(request, "test-group"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository).findByUsername("duplicate-user");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("모든 삭제되지 않은 사용자 목록을 조회한다")
    void getAllUsers() {
        // given
        User user1 = User.builder()
                .id(1L)
                .username("user1")
                .groupName("group1")
                .deleted(false)
                .build();

        User user2 = User.builder()
                .id(2L)
                .username("user2")
                .groupName("group2")
                .deleted(false)
                .build();

        when(userRepository.findAllByDeletedFalseOrderByCreatedAtDesc())
                .thenReturn(List.of(user1, user2));

        // when
        List<User> result = userService.getAllUsers();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUsername()).isEqualTo("user1");
        assertThat(result.get(1).getUsername()).isEqualTo("user2");

        verify(userRepository).findAllByDeletedFalseOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("사용자명으로 사용자를 조회한다")
    void getUser() {
        // given
        String username = "test-user";
        User user = User.builder()
                .id(1L)
                .username(username)
                .groupName("test-group")
                .deleted(false)
                .build();

        when(userRepository.findByUsernameAndDeletedFalse(username))
                .thenReturn(Optional.of(user));

        // when
        User result = userService.getUser(username);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getGroupName()).isEqualTo("test-group");

        verify(userRepository).findByUsernameAndDeletedFalse(username);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 예외가 발생한다")
    void getUserNotFound() {
        // given
        String username = "non-existent-user";

        when(userRepository.findByUsernameAndDeletedFalse(username))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUser(username))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByUsernameAndDeletedFalse(username);
    }

    @Test
    @DisplayName("사용자를 삭제한다")
    void deleteUser() {
        // given
        String username = "user-to-delete";
        User user = User.builder()
                .id(1L)
                .username(username)
                .groupName("test-group")
                .deleted(false)
                .build();

        when(userRepository.findByUsernameAndDeletedFalse(username))
                .thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        // when
        userService.deleteUser(username);

        // then
        verify(userRepository).findByUsernameAndDeletedFalse(username);
        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 삭제 시 예외가 발생한다")
    void deleteUserNotFound() {
        // given
        String username = "non-existent-user";

        when(userRepository.findByUsernameAndDeletedFalse(username))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(username))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findByUsernameAndDeletedFalse(username);
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("groupName이 null인 사용자도 생성할 수 있다")
    void createUserWithoutGroupName() {
        // given
        UserRequest request = new UserRequest();
        request.setUsername("user-no-group");

        User savedUser = User.builder()
                .id(1L)
                .username("user-no-group")
                .groupName(null)
                .build();

        when(userRepository.findByUsername("user-no-group")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // when
        User result = userService.createUser(request, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("user-no-group");
        assertThat(result.getGroupName()).isNull();

        verify(userRepository).save(any(User.class));
    }
}
