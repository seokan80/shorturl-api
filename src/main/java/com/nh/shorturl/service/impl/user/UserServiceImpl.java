package com.nh.shorturl.service.impl.user;

import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.service.user.UserService;
import com.nh.shorturl.util.Base62;
import com.nh.shorturl.util.JwtProvider;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    public UserServiceImpl(UserRepository userRepository, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.jwtProvider = jwtProvider;
    }

    public User createUser(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 랜덤 문자열 대신 JWT를 생성하여 API Key로 설정
        String apiKeyAsJwt = jwtProvider.createToken(username);

        User user = new User(username, apiKeyAsJwt);
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByUsernameAndApiKey(String username, String apiKey) {
        return userRepository.findByUsernameAndApiKey(username, apiKey);
    }
}
