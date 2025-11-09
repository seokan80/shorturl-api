# RedirectionHistoryController API 명세

기본 경로: `/r/history`

## 공통 규칙
- JWT 인증 필수 (`Authorization: Bearer {API_KEY}`).
- 응답은 `ResultEntity` 포맷이며 `code`/`message`는 `ApiResult` 규칙을 따릅니다.

## GET `/{shortUrlId}/count`
- **설명**: 특정 단축 URL의 누적 리다이렉션 횟수를 반환합니다.
- **요청 파라미터**
  - `shortUrlId`: `ShortUrl` 엔티티 ID.
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": 15
  }
  ```
- **에러 시나리오**: 내부 조회 실패 시 `9999`.

## POST `/{shortUrlId}/stats`
- **설명**: 리다이렉션 이력을 지정한 그룹 기준으로 집계합니다.
- **요청 본문**
  ```json
  { "groupBy": ["REFERER", "YEAR"] }
  ```
  - `groupBy` 값은 `GroupingType` 열거형(`REFERER`, `USER_AGENT`, `YEAR`, `MONTH`, `DAY`, `HOUR`)만 허용됩니다.
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": [
      { "referer": "https://google.com", "year": 2025, "count": 5 },
      { "referer": "https://naver.com", "year": 2025, "count": 2 }
    ]
  }
  ```
- **에러 시나리오**
  - 미존재 ID 또는 지원하지 않는 그룹 필드 → `9999`
