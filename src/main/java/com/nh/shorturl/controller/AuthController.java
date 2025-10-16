package com.nh.shorturl.controller;

import com.nh.shorturl.config.RegistrationConfig;
import com.nh.shorturl.dto.request.auth.TokenRequest;
import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.dto.response.auth.TokenResponse;
import com.nh.shorturl.dto.response.auth.UserResponse;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.service.auth.AuthService;
import com.nh.shorturl.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UserResponse> register(@RequestHeader("X-REGISTRATION-KEY") String key,
                                                 @RequestBody UserRequest request) {
        if (!registrationConfig.getRegistrationKey().equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        User user = userService.createUser(request.getUsername());
        return ResponseEntity.ok(new UserResponse(user.getUsername(), user.getApiKey()));
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> getToken(@RequestHeader("X-REGISTRATION-KEY") String key,
                                                  @RequestBody TokenRequest request) {
        if (!registrationConfig.getRegistrationKey().equals(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String newApiKey = authService.reissueToken(request.getUsername(), request.getApiKey());
            return ResponseEntity.ok(new TokenResponse(newApiKey));
        } catch (IllegalArgumentException e) {
            // 사용자가 존재하지 않거나 기존 apiKey가 일치하지 않는 경우
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
