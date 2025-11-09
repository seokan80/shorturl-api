package com.nh.shorturl.service.token;

import com.nh.shorturl.dto.response.auth.TokenResponse;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.UserRepository;
import com.nh.shorturl.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public TokenResponse issueToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
        return updateTokens(user);
    }

    @Override
    @Transactional
    public TokenResponse reissueToken(String username, String refreshToken) {
        User user = userRepository.findByUsernameAndRefreshToken(username, refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user or refresh token"));
        return updateTokens(user);
    }

    private TokenResponse updateTokens(User user) {
        String newApiKey = jwtProvider.createToken(user.getUsername());
        String newRefreshToken = UUID.randomUUID().toString();

        user.setApiKey(newApiKey);
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new TokenResponse(newApiKey, newRefreshToken);
    }
}
