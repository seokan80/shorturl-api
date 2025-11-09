package com.nh.shorturl.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long validityInSeconds;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = resolveKeyBytes(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] resolveKeyBytes(String candidate) {
        try {
            return Decoders.BASE64.decode(candidate);
        } catch (IllegalArgumentException | DecodingException ex) {
            log.warn("JWT secret is not valid Base64; deriving a SHA-512 key from the provided text. " +
                    "Set a Base64-encoded value in `jwt.secret` to silence this warning.");
            return sha512(candidate.getBytes(StandardCharsets.UTF_8));
        }
    }

    private byte[] sha512(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-512").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 algorithm not available", e);
        }
    }

    /**
     * 사용자 이름을 기반으로 JWT(API Key)를 생성합니다. (0.11.5 호환)
     * @param username 사용자 이름
     */
    public String createToken(String username) {
        long validityInMilliseconds = validityInSeconds * 1000;
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 토큰의 유효성을 검증합니다. (0.11.5 호환)
     * @param token 검증할 토큰
     * @return 유효하면 true
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 사용자 이름을 추출합니다. (0.11.5 호환)
     * @param token 파싱할 토큰
     * @return 사용자 이름
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
    
    /**
     * 토큰 만료 여부를 확인합니다.
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 토큰에서 만료 시간을 추출합니다.
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getExpiration();
    }
}
