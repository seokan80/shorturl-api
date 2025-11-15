package com.nh.shorturl.admin.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

/**
 * JWT 토큰 생성을 위한 테스트 헬퍼 클래스.
 * 실제 JwtProvider와 동일한 방식으로 토큰을 생성합니다.
 */
public class JwtTestHelper {

    // 테스트용 Base64 인코딩된 시크릿 (test/resources/application.yml과 동일)
    // HS512 알고리즘 요구사항: 최소 512비트 (64바이트)
    private static final String TEST_SECRET = "dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItdW5pdC10ZXN0aW5nLXB1cnBvc2Utb25seS1kb3Qtbm90LXVzZS1pbi1wcm9kdWN0aW9uLWVudmlyb25tZW50LXRoaXMtaXMtYS12ZXJ5LWxvbmctc2VjcmV0LWtleQ==";
    private static final long DEFAULT_VALIDITY_IN_MS = 3600000L; // 1 hour
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));

    /**
     * 유효한 JWT 토큰을 생성합니다.
     *
     * @param username 사용자명 (subject로 사용됨)
     * @return 생성된 JWT 토큰
     */
    public static String createValidToken(String username) {
        return createToken(username, DEFAULT_VALIDITY_IN_MS);
    }

    /**
     * 지정된 유효기간을 가진 JWT 토큰을 생성합니다.
     *
     * @param username            사용자명
     * @param validityInMilliseconds 유효기간 (밀리초)
     * @return 생성된 JWT 토큰
     */
    public static String createToken(String username, long validityInMilliseconds) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 만료된 JWT 토큰을 생성합니다.
     *
     * @param username 사용자명
     * @return 만료된 JWT 토큰
     */
    public static String createExpiredToken(String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() - 1000); // 1초 전에 만료됨

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now.getTime() - 2000)) // 2초 전 발급
                .setExpiration(expiration)
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 잘못된 서명을 가진 JWT 토큰을 생성합니다.
     *
     * @param username 사용자명
     * @return 잘못된 서명의 JWT 토큰
     */
    public static String createInvalidSignatureToken(String username) {
        // 다른 시크릿 키로 서명 (HS512 요구사항 충족: 512비트)
        String wrongSecret = "d3Jvbmctc2VjcmV0LWtleS1mb3ItdGVzdGluZy1pbnZhbGlkLXNpZ25hdHVyZS10aGlzLWlzLWEtdmVyeS1sb25nLXdyb25nLXNlY3JldC1rZXktZm9yLXRlc3Rpbmc=";
        Key wrongKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(wrongSecret));

        Date now = new Date();
        Date validity = new Date(now.getTime() + DEFAULT_VALIDITY_IN_MS);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(wrongKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Authorization 헤더 값을 생성합니다 (Bearer 접두사 포함).
     *
     * @param username 사용자명
     * @return "Bearer {token}" 형식의 헤더 값
     */
    public static String createAuthorizationHeader(String username) {
        return "Bearer " + createValidToken(username);
    }

    /**
     * 만료된 토큰의 Authorization 헤더 값을 생성합니다.
     *
     * @param username 사용자명
     * @return "Bearer {expired_token}" 형식의 헤더 값
     */
    public static String createExpiredAuthorizationHeader(String username) {
        return "Bearer " + createExpiredToken(username);
    }

    /**
     * 잘못된 서명 토큰의 Authorization 헤더 값을 생성합니다.
     *
     * @param username 사용자명
     * @return "Bearer {invalid_token}" 형식의 헤더 값
     */
    public static String createInvalidSignatureAuthorizationHeader(String username) {
        return "Bearer " + createInvalidSignatureToken(username);
    }
}
