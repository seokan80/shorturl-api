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
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final ClientAccessKeyService clientAccessKeyService;

    @GetMapping("/users")
    public ResultEntity<?> getUsers(@RequestHeader("X-CLIENTACCESS-KEY") String key) {
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
                            user.getUpdatedAt()
                    ))
                    .toList();
            return ResultEntity.ok(users);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }
    
    @PostMapping("/users")
    public ResultEntity<?> register(@RequestHeader("X-CLIENTACCESS-KEY") String key,
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
                    user.getUpdatedAt()
            ));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    @DeleteMapping("/users/{username}")
    public ResultEntity<?> deleteUser(@RequestHeader("X-CLIENTACCESS-KEY") String key,
                                      @PathVariable String username) {
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

    @GetMapping("/users/{username}")
    public ResultEntity<?> getUser(@RequestHeader("X-CLIENTACCESS-KEY") String key,
                                   @PathVariable String username) {
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
                    user.getUpdatedAt()
            );
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
