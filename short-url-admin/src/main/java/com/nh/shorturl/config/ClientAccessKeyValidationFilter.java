package com.nh.shorturl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.type.ApiResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * X-access-key 헤더를 검증하는 필터.
 * 비회원 요청 시 ClientAccessKey를 검증하고 보안 컨텍스트에 인증 정보를 설정합니다.
 */
@RequiredArgsConstructor
public class ClientAccessKeyValidationFilter extends OncePerRequestFilter {

    private final ClientAccessKeyService clientAccessKeyService;
    private final ObjectMapper objectMapper;

    public static final String CLIENT_ACCESS_KEY_ATTRIBUTE = "validatedClientAccessKey";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // /api/short-url 로 시작하는 요청에 대해 검증 시도
        if (!request.getRequestURI().startsWith("/api/short-url")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 이미 인증된 사용자는 스킵 (JWT 로그인 사용자)
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()
                && !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        // X-CLIENTACCESS-KEY 헤더 확인
        String accessKey = resolveAccessKey(request);

        if (accessKey != null && !accessKey.trim().isEmpty()) {
            try {
                // ClientAccessKey 검증
                ClientAccessKey validatedKey = clientAccessKeyService.validateActiveKey(accessKey);

                // request attribute에 저장하여 컨트롤러에서 사용
                request.setAttribute(CLIENT_ACCESS_KEY_ATTRIBUTE, validatedKey);

                // Spring Security 보안 컨텍스트에 인증 정보 설정
                // principal은 "anonymous"로 설정하여 기존 User 엔티티와의 호환성 유지
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "anonymous",
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT")));
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (IllegalArgumentException e) {
                // 유효하지 않은 키인 경우 즉시 에러 응답
                sendErrorResponse(response, ApiResult.UNAUTHORIZED, e.getMessage());
                return;
            }
        }

        // 키가 없으면 그냥 통과 (이후 Spring Security 설정에 따라 인가 여부 결정)
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, ApiResult apiResult, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ResultEntity<?> errorResponse = ResultEntity.of(apiResult);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String resolveAccessKey(HttpServletRequest request) {
        String header = request.getHeader("X-CLIENTACCESS-KEY");
        if (header != null && !header.isBlank()) {
            return header;
        }
        return null;
    }
}
