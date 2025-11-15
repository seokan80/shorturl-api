# UserController API 명세

기본 경로: `/api/users`

## 공통 규칙
- 모든 응답은 `ResultEntity` 포맷을 따르며 `code`, `message`, `data` 필드를 포함합니다.
- 모든 사용자 관리 엔드포인트는 클라이언트 키(`X-CLIENTACCESS-KEY`) 기반 검증을 수행합니다.
  - 발급된 클라이언트 키가 비활성 상태이거나 존재하지 않을 경우 `1401 (UNAUTHORIZED)`를 반환합니다.

## GET `/`
- **설명**: 등록된 모든 사용자 목록을 조회합니다.
- **요청 헤더**
  - `X-CLIENTACCESS-KEY: {access-key}`
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": [
      {
        "id": 1,
        "username": "my-service",
        "groupName": "default",
        "createdAt": "2025-01-13T12:00:00",
        "updatedAt": "2025-01-13T12:00:00"
      }
    ]
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401` (`UNAUTHORIZED`)

## POST `/`
- **설명**: 신규 사용자 정보를 저장합니다. 토큰 발급은 `/api/auth/token/issue`에서 별도로 수행됩니다.
- **요청 헤더**
  - `X-CLIENTACCESS-KEY: {access-key}`
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
      "id": 1,
      "username": "my-service",
      "groupName": "default",
      "createdAt": "2025-01-13T12:00:00",
      "updatedAt": "2025-01-13T12:00:00"
    }
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401` (`UNAUTHORIZED`)
  - 사용자명 중복 등 비즈니스 오류 → `9999` (`FAIL`)

## GET `/{username}`
- **설명**: 단일 사용자의 기본 정보를 조회합니다. 민감한 인증 정보(API Key, Refresh Token)는 포함되지 않습니다.
- **요청 헤더**
  - `X-CLIENTACCESS-KEY: {access-key}`
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": {
      "id": 1,
      "username": "my-service",
      "groupName": "default",
      "createdAt": "2025-01-13T12:00:00",
      "updatedAt": "2025-01-13T12:00:00"
    }
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401`
  - 사용자 미존재 → `1404` (`USER_NOT_FOUND`)

## DELETE `/{username}`
- **설명**: 사용자를 삭제합니다.
- **요청 헤더**
  - `X-CLIENTACCESS-KEY: {access-key}`
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": true
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401`
  - 사용자 미존재 → `1404` (`USER_NOT_FOUND`)

---

## AuthController (별도)

기본 경로: `/api/auth/token`

## POST `/issue`
- **설명**: 등록된 사용자 이름으로 Access/Refresh Token 쌍을 최초 발급합니다.
- **요청 헤더**
  - `X-CLIENTACCESS-KEY: {access-key}`
- **요청 본문**
  ```json
  {
    "username": "my-service"
  }
  ```
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": {
      "token": "eyJhbGciOiJIUzUxMiJ9...",
      "refreshToken": "f26c0d12-..."
    }
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401`
  - 사용자 미존재 → `1404` (`USER_NOT_FOUND`)

## POST `/re-issue`
- **설명**: 저장된 Refresh Token을 검증하고 Access/Refresh Token을 재발급합니다.
- **요청 헤더**
  - `X-CLIENTACCESS-KEY: {access-key}`
- **요청 본문**
  ```json
  {
    "username": "my-service",
    "refreshToken": "f26c0d12-..."
  }
  ```
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": {
      "token": "eyJhbGciOiJIUzUxMiJ9...",
      "refreshToken": "0d1ea972-..."
    }
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401`
  - 사용자/Refresh Token 불일치 → `1403` (`FORBIDDEN`)

---

## ClientAccessKeyController

기본 경로: `/api/client-keys`

- 모든 요청은 `X-CLIENTACCESS-KEY` 헤더에 유효한 클라이언트 키가 포함되어야 합니다.

### GET `/`
- **설명**: 발급된 클라이언트 키 목록을 최신순으로 반환합니다.
- **성공 응답 (`200`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": [
      {
        "id": 1,
        "name": "Payments",
        "keyValue": "59b4478b157c4e0f8c3a3a5c7c81d5f7",
        "issuedBy": "infra-team",
        "description": "결제 게이트웨이 연동",
        "expiresAt": "2025-12-31T15:00:00",
        "lastUsedAt": "2025-01-13T09:00:00",
        "active": true,
        "createdAt": "2025-01-10T08:00:00",
        "updatedAt": "2025-01-11T01:20:00"
      }
    ]
  }
  ```

### POST `/`
- **설명**: 클라이언트 키를 발급합니다. 이 엔드포인트는 추가 인증 없이 호출할 수 있습니다.
- **요청 본문**
  ```json
  {
    "name": "Payments",
    "issuedBy": "infra-team",
    "description": "결제 게이트웨이 연동",
    "expiresAt": "2025-12-31T15:00:00"
  }
  ```
- **성공 응답 (`200`)**: `ClientAccessKeyResponse`

### PUT `/{id}`
- **설명**: 이름/설명/만료일/활성 여부를 수정합니다.

### DELETE `/{id}`
- **설명**: 클라이언트 키를 삭제(소프트 삭제)합니다.
