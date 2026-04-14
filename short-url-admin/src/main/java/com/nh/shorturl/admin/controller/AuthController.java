package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.admin.service.token.TokenService;
import com.nh.shorturl.dto.request.auth.TokenIssueRequest;
import com.nh.shorturl.dto.request.auth.TokenReissueRequest;
import com.nh.shorturl.dto.response.auth.TokenResponse;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.type.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 서비스 (JWT 토큰 발급 및 갱신)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/token")
public class AuthController {

    private final TokenService tokenService;
    private final ClientAccessKeyService clientAccessKeyService;

    @Operation(summary = "토큰 발급", description = "X-CLIENTACCESS-KEY와 사용자ID를 이용해 새로운 JWT 토큰을 발급합니다.")
    @PostMapping("/issue")
    public ResultEntity<?> issueToken(
            @Parameter(description = "클라이언트 접근 키", required = true) @RequestHeader("X-CLIENTACCESS-KEY") String key,
            @RequestBody TokenIssueRequest request) {
        if (!isAccessKeyValid(key)) {
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

    @Operation(summary = "토큰 갱신", description = "만료된 Access Token을 Refresh Token을 이용해 갱신합니다.")
    @PostMapping("/re-issue")
    public ResultEntity<?> reissueToken(
            @Parameter(description = "클라이언트 접근 키", required = true) @RequestHeader("X-CLIENTACCESS-KEY") String key,
            @RequestBody TokenReissueRequest request) {
        if (!isAccessKeyValid(key)) {
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

    private boolean isAccessKeyValid(String key) {
        try {
            clientAccessKeyService.validateActiveKey(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
