package com.nh.shorturl.controller;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.service.shorturl.ShortUrlService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/r")
public class ShortUrlRedirectController {

    private final ShortUrlService shortUrlService;

    public ShortUrlRedirectController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    /**
     * 단축 URL 키로 요청이 들어왔을 때 원본 URL로 리디렉션
     *
     * @param shortUrl 단축 URL 키
     * @param response HttpServletResponse
     */
    @GetMapping("/{shortUrl}")
    public void redirectToOriginal(
            @PathVariable String shortUrl,
            HttpServletResponse response) throws IOException {

        ShortUrlResponse shortUrlResponse = shortUrlService.getShortUrlByKey(shortUrl);
        response.sendRedirect(shortUrlResponse.getLongUrl());
    }
}