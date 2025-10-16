package com.nh.shorturl.service.auth;

public interface AuthService {

    /**
     * 기존 apiKey를 검증하고 새로운 apiKey(JWT)를 재발급합니다.
     *
     * @param username 사용자 이름
     * @param oldApiKey 만료되었거나 기존의 apiKey
     * @return 새로 발급된 apiKey (JWT)
     * @throws IllegalArgumentException 사용자가 존재하지 않거나 apiKey가 일치하지 않을 경우
     */
    String reissueToken(String username, String oldApiKey);
}