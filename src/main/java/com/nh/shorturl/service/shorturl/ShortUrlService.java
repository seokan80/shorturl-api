package com.nh.shorturl.service.shorturl;

import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import com.nh.shorturl.entity.ClientAccessKey;

public interface ShortUrlService {

    /**
     * 단축 URL 생성 (로그인 사용자).
     */
    ShortUrlResponse createShortUrl(ShortUrlRequest request, String username) throws Exception;

    /**
     * 단축 URL 생성 (비회원, ClientAccessKey 인증).
     */
    ShortUrlResponse createShortUrlForClient(ShortUrlRequest request, ClientAccessKey clientAccessKey) throws Exception;

    /**
     * ID 기반 단축 URL 조회.
     */
    ShortUrlResponse getShortUrl(Long id);

    /**
     * 단축 키 기반 단축 URL 조회.
     */
    ShortUrlResponse getShortUrlByKey(String shortUrl);

    /**
     * 단축 URL 삭제.
     */
    void deleteShortUrl(Long id);

    /**
     * 단축 URL을 통해 원본 URL로 리다이렉션할 때 원본 URL 반환.
     */
    String resolveOriginalUrl(String shortUrl);
}
