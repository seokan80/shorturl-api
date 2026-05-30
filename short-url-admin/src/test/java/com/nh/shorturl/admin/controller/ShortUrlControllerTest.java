package com.nh.shorturl.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.admin.exception.GlobalExceptionHandler;
import com.nh.shorturl.admin.service.shorturl.ShortUrlService;
import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ShortUrlController 의 요청/응답 계약을 잠그는 골든 테스트.
 * GlobalExceptionHandler 도입 이후에도 이 테스트가 녹색으로 유지되어야 한다.
 * MockMvc standalone 설정을 사용하여 Spring 컨텍스트·Jasypt 로딩 비용을 피한다.
 */
class ShortUrlControllerTest {

    private ShortUrlService shortUrlService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @BeforeEach
    void setUp() {
        shortUrlService = mock(ShortUrlService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ShortUrlController(shortUrlService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_returnsSuccessEnvelope() throws Exception {
        ShortUrlRequest request = new ShortUrlRequest();
        request.setLongUrl("https://example.com");

        ShortUrlResponse response = ShortUrlResponse.builder()
                .id(1L)
                .shortKey("ABCDE123")
                .shortUrl("http://localhost:18081/s/ABCDE123")
                .longUrl("https://example.com")
                .createdAt(LocalDateTime.now())
                .build();
        when(shortUrlService.createShortUrl(any(ShortUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/short-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.shortKey").value("ABCDE123"));

        verify(shortUrlService).createShortUrl(any(ShortUrlRequest.class));
    }

    @Test
    void create_returnsFailEnvelopeOnServiceException() throws Exception {
        when(shortUrlService.createShortUrl(any(ShortUrlRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/short-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"longUrl\":\"https://example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("9999"));
    }

    @Test
    void getById_returnsSuccessEnvelope() throws Exception {
        ShortUrlResponse response = ShortUrlResponse.builder()
                .id(1L).shortKey("ABCDE123").longUrl("https://example.com").build();
        when(shortUrlService.getShortUrl(1L)).thenReturn(response);

        mockMvc.perform(get("/api/short-url/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.shortKey").value("ABCDE123"));
    }

    @Test
    void getById_returnsNotFoundEnvelopeWhenServiceThrows() throws Exception {
        when(shortUrlService.getShortUrl(99L)).thenThrow(new IllegalArgumentException("URL not found"));

        mockMvc.perform(get("/api/short-url/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1404"));
    }

    @Test
    void delete_returnsTrueOnSuccess() throws Exception {
        mockMvc.perform(delete("/api/short-url/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"));

        verify(shortUrlService).deleteShortUrl(1L);
    }

    @Test
    void delete_returnsNotFoundEnvelopeWhenEntityMissing() throws Exception {
        doThrow(new IllegalArgumentException("not found")).when(shortUrlService).deleteShortUrl(99L);

        mockMvc.perform(delete("/api/short-url/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1404"));
    }

    @Test
    void updateExpiration_returnsUpdatedResponse() throws Exception {
        ShortUrlResponse response = ShortUrlResponse.builder()
                .id(1L).shortKey("ABCDE123")
                .expiredAt(LocalDateTime.now().plusDays(7).toString())
                .build();
        when(shortUrlService.updateShortUrlExpiration(eq(1L), any(ShortUrlUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/short-url/1/expiration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiredAt\":\"2030-01-01T00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0000"))
                .andExpect(jsonPath("$.data.shortKey").value("ABCDE123"));
    }
}
