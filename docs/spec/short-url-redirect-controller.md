# ShortUrlRedirectController API 명세

기본 경로: `/r`

## 공통 규칙
- 공개 엔드포인트로 JWT가 필요 없습니다.
- 단축 키 조회에 실패하거나 만료된 경우 `404 Not Found` 응답을 전송합니다.
- 성공 시 `RedirectionHistoryService`를 통해 referer/user-agent/IP 등 접근 이력을 비동기적으로 저장하며, 기록 실패 여부와 관계없이 리다이렉션은 반드시 수행됩니다.

## GET `/{shortUrl}`
- **설명**: 단축 키에 매핑된 원본 URL로 302 리다이렉트합니다.
- **요청 파라미터**
  - `shortUrl`: Base62 키.
- **성공 플로우**
  1. `ShortUrlService.getShortUrlByKey`로 유효성 검사 및 URL 조회.
  2. 리다이렉션 이력 저장 (예외 무시).
  3. `HttpServletResponse#sendRedirect(longUrl)` 호출 → 302 응답.
- **에러 플로우**
  - 키 미존재 또는 만료 → `HttpServletResponse#sendError(404, message)`로 에러 페이지 반환.
