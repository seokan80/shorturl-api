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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * X-access-key 헤더를 검증하는 필터.
 * 비회원 shortUrl 생성 요청 시 ClientAccessKey를 검증합니다.
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

        // /api/short-url POST 요청만 처리
        if (!"/api/short-url".equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod())) {
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

        // X-access-key / X-CLIENTACCESS-KEY 헤더 검증 (대소문자 구분 없이 허용)
        String accessKey = resolveAccessKey(request);
        if (accessKey == null || accessKey.trim().isEmpty()) {
            sendErrorResponse(response, ApiResult.UNAUTHORIZED, "X-access-key header is required");
            return;
        }

        try {
            // ClientAccessKey 검증
            ClientAccessKey validatedKey = clientAccessKeyService.validateActiveKey(accessKey);
            // request attribute에 저장하여 컨트롤러에서 사용
            request.setAttribute(CLIENT_ACCESS_KEY_ATTRIBUTE, validatedKey);
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            sendErrorResponse(response, ApiResult.UNAUTHORIZED, e.getMessage());
        }
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
