# ShortUrlController API 명세

기본 경로: `/api/short-url`

## 공통 규칙
- 모든 요청은 `Authorization: Bearer {API_KEY}` 헤더로 인증합니다.
- 응답은 `ResultEntity` 포맷을 사용하며, `code`/`message` 값은 `ApiResult` 열거형을 따릅니다.
- JWT에서 추출한 `Principal#getName()` 값이 서비스 계층으로 전달되어 생성자/소유자 정보로 사용됩니다.

## POST `/`
- **설명**: 신규 단축 URL을 생성합니다.
- **요청 본문**
  ```json
  { "longUrl": "https://example.com/page" }
  ```
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": {
      "id": 42,
      "shortKey": "a1B2c3D4",
      "shortUrl": "https://sho.rt/a1B2c3D4",
      "longUrl": "https://example.com/page",
      "createdBy": "my-service",
      "userId": 7,
      "createdAt": "2025-01-18T12:34:56",
      "expiredAt": "2025-01-19T12:34:56"
    }
  }
  ```
- **에러 시나리오**
  - 사용자 정보 불일치, 저장 실패 등 → `9999` (`FAIL`)

## GET `/{id}`
- **설명**: ID로 단축 URL을 조회합니다.
- **성공 응답 (`200`, code `0000`)**: `POST /`와 동일한 `ShortUrlResponse` 구조
- **에러 시나리오**: 리소스 없음 또는 만료 → `1404` (`NOT_FOUND`)

## GET `/key/{shortUrl}`
- **설명**: 단축 키(slug)로 단축 URL을 조회합니다.
- **성공 응답**: `ShortUrlResponse`
- **에러 시나리오**
  - 키 미존재 → `1404`
  - 만료된 키 → `9999` (`IllegalStateException`을 `ResultEntity.of(ApiResult.NOT_FOUND)`로 매핑)

## POST `/delete/{id}`
- **설명**: 단축 URL을 삭제합니다. (HTTP `POST` 사용)
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": { "result": true }
  }
  ```
- **에러 시나리오**: 대상 없음/삭제 실패 → `9999`
