package com.nh.shorturl.redirect.service;

import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;

public interface ShortUrlService {
    ShortUrlResponse getShortUrlByKey(String shortUrlKey);
}
