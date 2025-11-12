package com.nh.shorturl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.service.user.CustomUserDetailsService;
import com.nh.shorturl.util.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity // Spring Security를 활성화하고 웹 보안 설정을 구성함을 명시
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final ClientAccessKeyService clientAccessKeyService;
    private final ObjectMapper objectMapper;

    @Bean
    public ClientAccessKeyValidationFilter clientAccessKeyValidationFilter() {
        return new ClientAccessKeyValidationFilter(clientAccessKeyService, objectMapper);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                // JWT 기반의 무상태(Stateless) API로 운영하기 위해 세션 정책을 STATELESS로 설정합니다.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /api/client-keys/, /api/auth/, /r/ 하위 경로 및 /error 경로는 인증 없이 허용
                        .requestMatchers("/api/client-keys/**", "/api/auth/**", "/r/**", "/error").permitAll()
                        // /api/short-url POST는 X-access-key로 검증되므로 permitAll
                        .requestMatchers(HttpMethod.POST, "/api/short-url").permitAll()
                        // 그 외 나머지 모든 요청은 반드시 인증을 거쳐야 함
                        .anyRequest().authenticated()
                )
                // ClientAccessKeyValidationFilter를 JwtAuthenticationFilter 앞에 추가
                .addFilterBefore(clientAccessKeyValidationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider, customUserDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
