package com.nh.shorturl.controller;

import com.nh.shorturl.config.RegistrationConfig;
import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.dto.response.auth.UserDetailResponse;
import com.nh.shorturl.dto.response.auth.UserResponse;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.service.user.UserService;
import com.nh.shorturl.type.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final RegistrationConfig registrationConfig;

    @GetMapping("/users")
    public ResultEntity<?> getUsers(@RequestHeader("X-REGISTRATION-KEY") String key) {
        if (!isRegistrationKeyValid(key)) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            List<UserResponse> users = userService.getAllUsers()
                    .stream()
                    .map(user -> new UserResponse(user.getUsername()))
                    .toList();
            return ResultEntity.ok(users);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }
    
    @PostMapping("/users")
    public ResultEntity<?> register(@RequestHeader("X-REGISTRATION-KEY") String key,
                                    @RequestBody UserRequest request) {
        if (!isRegistrationKeyValid(key)) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            User user = userService.createUser(request);
            return new ResultEntity<>(new UserResponse(user.getUsername()));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    @DeleteMapping("/users/{username}")
    public ResultEntity<?> deleteUser(@RequestHeader("X-REGISTRATION-KEY") String key,
                                      @PathVariable String username) {
        if (!isRegistrationKeyValid(key)) {
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

    @GetMapping("/users/{username}")
    public ResultEntity<?> getUser(@RequestHeader("X-REGISTRATION-KEY") String key,
                                   @PathVariable String username) {
        if (!isRegistrationKeyValid(key)) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            User user = userService.getUser(username);
            UserDetailResponse response = new UserDetailResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
            return new ResultEntity<>(response);
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.USER_NOT_FOUND);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    

    private boolean isRegistrationKeyValid(String key) {
        return registrationConfig.getRegistrationKey().equals(key);
    }
}
