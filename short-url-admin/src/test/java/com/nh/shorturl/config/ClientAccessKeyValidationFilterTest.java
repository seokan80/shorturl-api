package com.nh.shorturl.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.entity.ClientAccessKey;
import com.nh.shorturl.service.clientaccess.ClientAccessKeyService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientAccessKeyValidationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("대문자 X-CLIENTACCESS-KEY 헤더를 사용해도 인증이 통과한다")
    void acceptsUppercaseClientAccessKeyHeader() throws Exception {
        ClientAccessKeyService clientAccessKeyService = mock(ClientAccessKeyService.class);
        ClientAccessKeyValidationFilter filter = new ClientAccessKeyValidationFilter(clientAccessKeyService, new ObjectMapper());
        ClientAccessKey key = ClientAccessKey.builder().id(1L).keyValue("abc").name("test").build();
        when(clientAccessKeyService.validateActiveKey("abc")).thenReturn(key);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/short-url");
        request.setMethod("POST");
        request.addHeader("X-CLIENTACCESS-KEY", "abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(request.getAttribute(ClientAccessKeyValidationFilter.CLIENT_ACCESS_KEY_ATTRIBUTE)).isEqualTo(key);
    }
}
