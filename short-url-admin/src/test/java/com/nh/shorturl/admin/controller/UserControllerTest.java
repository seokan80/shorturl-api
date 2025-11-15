package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.admin.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.admin.service.user.UserService;
import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.dto.response.auth.UserDetailResponse;
import com.nh.shorturl.dto.response.auth.UserResponse;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.type.ApiResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserService userService;
    private ClientAccessKeyService clientAccessKeyService;
    private UserController controller;

    private static final String VALID_ACCESS_KEY = "valid-test-key";
    private static final String INVALID_ACCESS_KEY = "invalid-key";

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        clientAccessKeyService = mock(ClientAccessKeyService.class);
        controller = new UserController(userService, clientAccessKeyService);

        // ClientAccessKey 검증 설정
        ClientAccessKey validKey = ClientAccessKey.builder()
                .id(1L)
                .name("test-client")
                .keyValue(VALID_ACCESS_KEY)
                .active(true)
                .build();

        when(clientAccessKeyService.validateActiveKey(VALID_ACCESS_KEY)).thenReturn(validKey);
        when(clientAccessKeyService.validateActiveKey(INVALID_ACCESS_KEY))
                .thenThrow(new IllegalArgumentException("Invalid key"));
    }

    @Test
    @DisplayName("유효한 클라이언트 키로 사용자 목록을 조회한다")
    void getUsers() {
        // given
        User user1 = createUser(1L, "user1", "group1");
        User user2 = createUser(2L, "user2", "group2");

        when(userService.getAllUsers()).thenReturn(List.of(user1, user2));

        // when
        ResultEntity<?> result = controller.getUsers(VALID_ACCESS_KEY);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<UserResponse> users = (List<UserResponse>) result.getData();
        assertThat(users).hasSize(2);
        assertThat(users.get(0).getUsername()).isEqualTo("user1");
        assertThat(users.get(1).getUsername()).isEqualTo("user2");

        verify(userService).getAllUsers();
        verify(clientAccessKeyService).validateActiveKey(VALID_ACCESS_KEY);
    }

    @Test
    @DisplayName("잘못된 클라이언트 키로 사용자 목록 조회 시 UNAUTHORIZED를 반환한다")
    void getUsersUnauthorized() {
        // when
        ResultEntity<?> result = controller.getUsers(INVALID_ACCESS_KEY);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.UNAUTHORIZED.getCode());

        verify(userService, never()).getAllUsers();
        verify(clientAccessKeyService).validateActiveKey(INVALID_ACCESS_KEY);
    }

    @Test
    @DisplayName("새로운 사용자를 등록한다")
    void register() {
        // given
        UserRequest request = new UserRequest();
        request.setUsername("new-user");

        User createdUser = createUser(1L, "new-user", "test-client");
        when(userService.createUser(any(UserRequest.class), eq("test-client")))
                .thenReturn(createdUser);

        // when
        ResultEntity<?> result = controller.register(VALID_ACCESS_KEY, request);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isInstanceOf(UserResponse.class);

        UserResponse response = (UserResponse) result.getData();
        assertThat(response.getUsername()).isEqualTo("new-user");
        assertThat(response.getGroupName()).isEqualTo("test-client");

        verify(userService).createUser(request, "test-client");
        verify(clientAccessKeyService).validateActiveKey(VALID_ACCESS_KEY);
    }

    @Test
    @DisplayName("중복된 사용자명으로 등록 시 실패 메시지를 반환한다")
    void registerDuplicateUsername() {
        // given
        UserRequest request = new UserRequest();
        request.setUsername("duplicate-user");

        when(userService.createUser(any(UserRequest.class), eq("test-client")))
                .thenThrow(new IllegalArgumentException("Username already exists"));

        // when
        ResultEntity<?> result = controller.register(VALID_ACCESS_KEY, request);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.FAIL.getCode());
        assertThat(result.getMessage()).contains("Username already exists");

        verify(userService).createUser(request, "test-client");
    }

    @Test
    @DisplayName("잘못된 클라이언트 키로 사용자 등록 시 UNAUTHORIZED를 반환한다")
    void registerUnauthorized() {
        // given
        UserRequest request = new UserRequest();
        request.setUsername("test-user");

        // when
        ResultEntity<?> result = controller.register(INVALID_ACCESS_KEY, request);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.UNAUTHORIZED.getCode());

        verify(userService, never()).createUser(any(), any());
    }

    @Test
    @DisplayName("사용자 상세 정보를 조회한다")
    void getUser() {
        // given
        String username = "test-user";
        User user = createUser(1L, username, "test-group");

        when(userService.getUser(username)).thenReturn(user);

        // when
        ResultEntity<?> result = controller.getUser(VALID_ACCESS_KEY, username);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isInstanceOf(UserDetailResponse.class);

        UserDetailResponse response = (UserDetailResponse) result.getData();
        assertThat(response.getUsername()).isEqualTo(username);
        assertThat(response.getGroupName()).isEqualTo("test-group");

        verify(userService).getUser(username);
        verify(clientAccessKeyService).validateActiveKey(VALID_ACCESS_KEY);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 조회 시 USER_NOT_FOUND를 반환한다")
    void getUserNotFound() {
        // given
        String username = "non-existent-user";

        when(userService.getUser(username))
                .thenThrow(new IllegalArgumentException("User not found"));

        // when
        ResultEntity<?> result = controller.getUser(VALID_ACCESS_KEY, username);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.USER_NOT_FOUND.getCode());

        verify(userService).getUser(username);
    }

    @Test
    @DisplayName("사용자를 삭제한다")
    void deleteUser() {
        // given
        String username = "user-to-delete";
        doNothing().when(userService).deleteUser(username);

        // when
        ResultEntity<?> result = controller.deleteUser(VALID_ACCESS_KEY, username);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isNotNull();

        verify(userService).deleteUser(username);
        verify(clientAccessKeyService).validateActiveKey(VALID_ACCESS_KEY);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 삭제 시 USER_NOT_FOUND를 반환한다")
    void deleteUserNotFound() {
        // given
        String username = "non-existent-user";

        doThrow(new IllegalArgumentException("User not found"))
                .when(userService).deleteUser(username);

        // when
        ResultEntity<?> result = controller.deleteUser(VALID_ACCESS_KEY, username);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.USER_NOT_FOUND.getCode());

        verify(userService).deleteUser(username);
    }

    @Test
    @DisplayName("잘못된 클라이언트 키로 사용자 삭제 시 UNAUTHORIZED를 반환한다")
    void deleteUserUnauthorized() {
        // given
        String username = "test-user";

        // when
        ResultEntity<?> result = controller.deleteUser(INVALID_ACCESS_KEY, username);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.UNAUTHORIZED.getCode());

        verify(userService, never()).deleteUser(any());
    }

    private User createUser(Long id, String username, String groupName) {
        return User.builder()
                .id(id)
                .username(username)
                .groupName(groupName)
                .deleted(false)
                .build();
    }
}
