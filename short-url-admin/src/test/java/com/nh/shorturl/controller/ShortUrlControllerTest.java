package com.nh.shorturl.controller;

import com.nh.shorturl.admin.controller.ShortUrlController;
import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.admin.service.shorturl.ShortUrlService;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.type.ApiResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ShortUrlControllerTest {

    private final ShortUrlService shortUrlService = mock(ShortUrlService.class);
    private final ClientAccessKeyService clientAccessKeyService = mock(ClientAccessKeyService.class);
    private final ShortUrlController controller = new ShortUrlController(shortUrlService, clientAccessKeyService);

    @Test
    @DisplayName("클라이언트 키 헤더만 있어도 ShortUrl 생성이 가능하다")
    void createWithClientAccessKeyHeader() throws Exception {
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://example.com");

        ClientAccessKey key = ClientAccessKey.builder().id(1L).keyValue("abc").name("client").build();
        when(clientAccessKeyService.validateActiveKey("abc")).thenReturn(key);

        ShortUrlResponse expectedResponse = ShortUrlResponse.builder()
                .id(10L)
                .shortKey("shortKey")
                .shortUrl("https://sho.rt/shortKey")
                .longUrl(request.getLongUrl())
                .createdBy("anonymous")
                .userId(99L)
                .createdAt(LocalDateTime.now())
                .build();
        when(shortUrlService.createShortUrlForClient(request, key)).thenReturn(expectedResponse);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("X-CLIENTACCESS-KEY", "abc");

        ResultEntity<?> result = controller.create(request, null, servletRequest);

        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isEqualTo(expectedResponse);

        verify(clientAccessKeyService).validateActiveKey("abc");
        verify(shortUrlService).createShortUrlForClient(request, key);
    }
}
