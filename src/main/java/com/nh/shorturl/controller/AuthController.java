package com.nh.shorturl.controller;

import com.nh.shorturl.config.RegistrationConfig;
import com.nh.shorturl.dto.request.auth.TokenRequest;
import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.dto.response.auth.TokenResponse;
import com.nh.shorturl.dto.response.auth.UserResponse;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.service.auth.AuthService;
import com.nh.shorturl.service.user.UserService;
import com.nh.shorturl.type.ApiResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final RegistrationConfig registrationConfig;

    public AuthController(AuthService authService, UserService userService, RegistrationConfig registrationConfig) {
        this.authService = authService;
        this.userService = userService;
        this.registrationConfig = registrationConfig;
    }

    @PostMapping("/register")
    public ResultEntity<?> register(@RequestHeader("X-REGISTRATION-KEY") String key,
                                               @RequestBody UserRequest request) {
        if (!registrationConfig.getRegistrationKey().equals(key)) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            User user = userService.createUser(request.getUsername());
            return new ResultEntity<>(new UserResponse(user.getUsername(), user.getApiKey()));
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    @PostMapping("/token")
    public ResultEntity<?> getToken(@RequestHeader("X-REGISTRATION-KEY") String key,
                                                  @RequestBody TokenRequest request) {
        if (!registrationConfig.getRegistrationKey().equals(key)) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            String newApiKey = authService.reissueToken(request.getUsername(), request.getApiKey());
            return new ResultEntity<>(new TokenResponse(newApiKey));
        } catch (IllegalArgumentException e) {
            // 사용자가 존재하지 않거나 기존 apiKey가 일치하지 않는 경우
            return ResultEntity.of(ApiResult.FORBIDDEN);
        }
    }
}
