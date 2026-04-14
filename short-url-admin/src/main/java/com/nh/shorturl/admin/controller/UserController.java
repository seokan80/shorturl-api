package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.admin.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.admin.service.user.UserService;
import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.dto.request.auth.UserUpdateRequest;
import com.nh.shorturl.dto.response.auth.UserDetailResponse;
import com.nh.shorturl.dto.response.auth.UserResponse;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.type.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User", description = "관리자 사용자 계정 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final ClientAccessKeyService clientAccessKeyService;

    @Operation(summary = "사용자 목록 조회", description = "시스템에 등록된 모든 사용자 계정 목록을 조회합니다.")
    @GetMapping("")
    public ResultEntity<?> getUsers(
            @Parameter(description = "클라이언트 접근 키", required = true) @RequestHeader("X-CLIENTACCESS-KEY") String key) {
        if (getValidatedAccessKey(key) == null) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            List<UserResponse> users = userService.getAllUsers()
                    .stream()
                    .map(user -> new UserResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getGroupName(),
                            user.getCreatedAt(),
                            user.getUpdatedAt()))
                    .toList();
            return ResultEntity.ok(users);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    @Operation(summary = "사용자 등록", description = "신규 관리자 사용자 계정을 생성합니다.")
    @PostMapping("")
    public ResultEntity<?> register(
            @Parameter(description = "클라이언트 접근 키", required = true) @RequestHeader("X-CLIENTACCESS-KEY") String key,
            @RequestBody UserRequest request) {
        ClientAccessKey clientAccessKey = getValidatedAccessKey(key);
        if (clientAccessKey == null) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            User user = userService.createUser(request, clientAccessKey.getName());
            return new ResultEntity<>(new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getGroupName(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "사용자 정보 수정", description = "사용자의 고객사명(그룹명) 등 정보를 수정합니다.")
    @PutMapping("/{username}")
    public ResultEntity<?> updateUser(
            @Parameter(description = "클라이언트 접근 키", required = true) @RequestHeader("X-CLIENTACCESS-KEY") String key,
            @Parameter(description = "사용자 계정ID(username)") @PathVariable String username,
            @RequestBody UserUpdateRequest request) {
        if (getValidatedAccessKey(key) == null) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            User user = userService.updateUser(username, request);
            return new ResultEntity<>(new UserResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getGroupName(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.USER_NOT_FOUND);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    @Operation(summary = "사용자 삭제", description = "지정된 사용자 계정을 삭제(비활성화) 처리합니다.")
    @DeleteMapping("/{username}")
    public ResultEntity<?> deleteUser(
            @Parameter(description = "클라이언트 접근 키", required = true) @RequestHeader("X-CLIENTACCESS-KEY") String key,
            @Parameter(description = "사용자 계정ID(username)") @PathVariable String username) {
        if (getValidatedAccessKey(key) == null) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            userService.deleteUser(username);
            return ResultEntity.True();
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.USER_NOT_FOUND);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    @Operation(summary = "사용자 상세 조회", description = "특정 사용자 계정의 상세 정보를 조회합니다.")
    @GetMapping("/{username}")
    public ResultEntity<?> getUser(
            @Parameter(description = "클라이언트 접근 키", required = true) @RequestHeader("X-CLIENTACCESS-KEY") String key,
            @Parameter(description = "사용자 계정ID(username)") @PathVariable String username) {
        if (getValidatedAccessKey(key) == null) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            User user = userService.getUser(username);
            UserDetailResponse response = new UserDetailResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getGroupName(),
                    user.getCreatedAt(),
                    user.getUpdatedAt());
            return new ResultEntity<>(response);
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.USER_NOT_FOUND);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    private ClientAccessKey getValidatedAccessKey(String key) {
        try {
            return clientAccessKeyService.validateActiveKey(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
