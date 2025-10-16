package com.nh.shorturl.config;

import com.nh.shorturl.service.user.CustomUserDetailsService;
import com.nh.shorturl.util.JwtProvider; // 1. import 변경
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider; // 2. 필드 타입 변경
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // /api/auth/ 하위 모든 경로는 인증 없이 허용
                        .requestMatchers("/api/auth/**", "/r/**").permitAll()
                        .anyRequest().authenticated()
                )
                // 3. jwtProvider 주입
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}