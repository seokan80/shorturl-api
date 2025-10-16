package com.nh.shorturl.service.impl.auth;

import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.service.auth.AuthService;
import com.nh.shorturl.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public String reissueToken(String username, String oldApiKey) {
        // 1. username과 기존 apiKey로 사용자를 찾습니다.
        User user = userRepository.findByUsernameAndApiKey(username, oldApiKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user or API Key"));

        // 2. 새로운 apiKey(JWT)를 생성합니다.
        String newApiKey = jwtProvider.createToken(user.getUsername());

        // 3. 사용자의 apiKey를 새로운 키로 업데이트합니다.
        user.setApiKey(newApiKey);
        userRepository.save(user);

        // 4. 새로운 apiKey를 반환합니다.
        return newApiKey;
    }
}