# AuthController API 명세

기본 경로: `/api/auth`

## 공통 규칙
- 모든 응답은 `ResultEntity` 포맷을 따르며 `code`, `message`, `data` 필드를 포함합니다.
- 두 엔드포인트 모두 JWT가 아닌 등록 키 기반 헤더 검증을 수행합니다.
  - `X-REGISTRATION-KEY` 값은 `registrationConfig.registrationKey`와 일치해야 하며, 불일치 시 `1401 (UNAUTHORIZED)`를 반환합니다.

## POST `/register`
- **설명**: 신규 사용자에게 API Key를 발급합니다.
- **요청 헤더**
  - `X-REGISTRATION-KEY: {registration-key}`
- **요청 본문**
  ```json
  { "username": "my-service" }
  ```
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": {
      "username": "my-service",
      "apiKey": "eyJhbGciOiJIUzUxMiJ9..."
    }
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401` (`UNAUTHORIZED`)
  - 사용자명 중복 등 비즈니스 오류 → `9999` (`FAIL`)

## POST `/token`
- **설명**: 기존 사용자/키 조합을 검증하고 새 JWT(API Key)를 재발급합니다.
- **요청 헤더**
  - `X-REGISTRATION-KEY: {registration-key}`
- **요청 본문**
  ```json
  {
    "username": "my-service",
    "apiKey": "expired-or-current-jwt"
  }
  ```
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": { "token": "eyJhbGciOiJIUzUxMiJ9..." }
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401`
  - 사용자 미존재, username/apiKey 불일치 → `1403` (`FORBIDDEN`)
