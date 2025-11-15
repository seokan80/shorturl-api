package com.nh.shorturl;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Jasypt 암호화 테스트.
 *
 * 이 테스트는 실제 암호화가 필요할 때만 수동으로 실행합니다.
 * Spring Context가 필요없으므로 @SpringBootTest를 제거했습니다.
 */
@Disabled("Manual encryption test - enable only when needed")
public class JasyptEncryptionTest {

    @Test
    void encrypt_value() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("shortUrlApi"); // application.yml에 설정한 jasypt.encryptor.password
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256"); // application.yml에 설정한 algorithm
        encryptor.setIvGenerator(new RandomIvGenerator());

        String plainText = "mXuH+WprMme+/ankLaa+v0AskbYICwpCaimcaqE/z+kABiSQZxP9CyBTUf7IneqZK5t7tw5glKTO0iGi5gwR4Q=="; // 암호화할 값

        String encryptedText = encryptor.encrypt(plainText);

        System.out.println("Encrypted value: ENC(" + encryptedText + ")");
    }
}