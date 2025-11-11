// 이 파일은 이전 답변의 내용과 동일하므로 생략합니다.
// (saveRedirectionHistory 메서드가 포함된 인터페이스)
package com.nh.shorturl.redirect.service;

import jakarta.servlet.http.HttpServletRequest;

public interface RedirectionHistoryService {
    void saveRedirectionHistory(String shortUrlKey, HttpServletRequest request);
}
