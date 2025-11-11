package com.nh.shorturl;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.util.Base64;

public class KeyGeneratorTest {

    @Test
    void keyEncode() {
        // HS512 에 맞는 안전한 키 생성
        javax.crypto.SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        // 생성된 키를 Base64 문자열로 인코딩
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
        System.out.println("Generated Base64 Key: " + base64Key);
    }
}