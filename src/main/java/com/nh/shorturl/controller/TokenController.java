package com.nh.shorturl.controller;

import com.nh.shorturl.config.RegistrationConfig;
import com.nh.shorturl.dto.request.auth.TokenIssueRequest;
import com.nh.shorturl.dto.request.auth.TokenReissueRequest;
import com.nh.shorturl.dto.response.auth.TokenResponse;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.service.token.TokenService;
import com.nh.shorturl.type.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/token")
public class TokenController {

    private final TokenService tokenService;
    private final RegistrationConfig registrationConfig;

    @PostMapping("/issue")
    public ResultEntity<?> issueToken(@RequestHeader("X-REGISTRATION-KEY") String key,
                                      @RequestBody TokenIssueRequest request) {
        if (!isRegistrationKeyValid(key)) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            TokenResponse response = tokenService.issueToken(request.getUsername());
            return new ResultEntity<>(response);
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.USER_NOT_FOUND);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    @PostMapping("/re-issue")
    public ResultEntity<?> reissueToken(@RequestHeader("X-REGISTRATION-KEY") String key,
                                        @RequestBody TokenReissueRequest request) {
        if (!isRegistrationKeyValid(key)) {
            return ResultEntity.of(ApiResult.UNAUTHORIZED);
        }

        try {
            TokenResponse response = tokenService.reissueToken(request.getUsername(), request.getRefreshToken());
            return new ResultEntity<>(response);
        } catch (IllegalArgumentException e) {
            return ResultEntity.of(ApiResult.FORBIDDEN);
        } catch (Exception e) {
            return ResultEntity.of(ApiResult.FAIL);
        }
    }

    private boolean isRegistrationKeyValid(String key) {
        return registrationConfig.getRegistrationKey().equals(key);
    }
}
