package com.nh.shorturl.service.token;

import com.nh.shorturl.dto.response.auth.TokenResponse;

public interface TokenService {

    /**
     * 등록된 사용자에게 Access/Refresh Token 쌍을 발급합니다.
     *
     * @param username 토큰을 발급할 사용자명
     * @return 새로 발급된 토큰 쌍
     */
    TokenResponse issueToken(String username);

    /**
     * Refresh Token을 검증하고 새로운 토큰 쌍을 재발급합니다.
     *
     * @param username 사용자명
     * @param refreshToken 기존 Refresh Token
     * @return 새로 발급된 토큰 쌍
     */
    TokenResponse reissueToken(String username, String refreshToken);
}
