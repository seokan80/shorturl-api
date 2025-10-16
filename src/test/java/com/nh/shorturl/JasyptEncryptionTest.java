package com.nh.shorturl;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.junit.jupiter.api.Test;

public class JasyptEncryptionTest {

    @Test
    void encrypt_value() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("aaa"); // application.yml에 설정한 jasypt.encryptor.password
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256"); // application.yml에 설정한 algorithm
        encryptor.setIvGenerator(new RandomIvGenerator());

        String plainText = "mXuH+WprMme+/ankLaa+v0AskbYICwpCaimcaqE/z+kABiSQZxP9CyBTUf7IneqZK5t7tw5glKTO0iGi5gwR4Q=="; // 암호화할 값
        String encryptedText = encryptor.encrypt(plainText);

        System.out.println("Encrypted value: ENC(" + encryptedText + ")");
    }
}