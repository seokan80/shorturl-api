package com.nh.shorturl.admin.service.shorturl;

import com.nh.shorturl.dto.request.shorturl.ShortUrlRequest;
import com.nh.shorturl.dto.request.shorturl.ShortUrlUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultList;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ShortUrlService {

    /** 단축 URL 생성 (인증 없음, 폐쇄망 전제). */
    ShortUrlResponse createShortUrl(ShortUrlRequest request);

    /** ID 기반 단축 URL 조회. */
    ShortUrlResponse getShortUrl(Long id);

    /** 단축 키 기반 단축 URL 조회. */
    ShortUrlResponse getShortUrlByKey(String shortUrl);

    /** 단축 URL 삭제. */
    void deleteShortUrl(Long id);

    /** 단축 URL 을 원본 URL 로 해석. */
    String resolveOriginalUrl(String shortUrl);

    List<ShortUrlResponse> findAllForCaching();

    /** 단축 URL 목록 조회 (페이징). */
    ResultList<ShortUrlResponse> listShortUrls(Pageable pageable);

    /** 단축 URL 만료일 수정. */
    ShortUrlResponse updateShortUrlExpiration(Long id, ShortUrlUpdateRequest request);

    /**
     * 지정 시각 이후 생성·수정·삭제된 단축 URL 목록 (증분 동기화용).
     * 소프트 삭제된 항목도 포함하여 redirect 서버가 캐시에서 evict 할 수 있도록 한다.
     */
    List<ShortUrlResponse> findChangedSince(LocalDateTime since);
}
