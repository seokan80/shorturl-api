package com.nh.shorturl;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class JasyptEncryptionTest {

//    @Value("${jasypt.encryptor.password:shortUrl}")
//    String password;

    @Test
    void encrypt_value() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
//        encryptor.setPassword(password); // application.yml에 설정한 jasypt.encryptor.password
        encryptor.setPassword("shortUrlApi"); // application.yml에 설정한 jasypt.encryptor.password
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256"); // application.yml에 설정한 algorithm
        encryptor.setIvGenerator(new RandomIvGenerator());

//        String plainText = "mXuH+WprMme+/ankLaa+v0AskbYICwpCaimcaqE/z+kABiSQZxP9CyBTUf7IneqZK5t7tw5glKTO0iGi5gwR4Q=="; // 암호화할 값
//        String plainText = "password"; // 암호화할 값
        String plainText = "mXuH+WprMme+/ankLaa+v0AskbYICwpCaimcaqE/z+kABiSQZxP9CyBTUf7IneqZK5t7tw5glKTO0iGi5gwR4Q=="; // 암호화할 값

        String encryptedText = encryptor.encrypt(plainText);

        System.out.println("Encrypted value: ENC(" + encryptedText + ")");
    }
}