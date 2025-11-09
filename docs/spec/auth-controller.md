# AuthController API 명세

기본 경로: `/api/auth`

## 공통 규칙
- 모든 응답은 `ResultEntity` 포맷을 따르며 `code`, `message`, `data` 필드를 포함합니다.
- 모든 인증 엔드포인트는 등록 키 기반 헤더 검증을 수행합니다.
  - `X-REGISTRATION-KEY` 값이 `registrationConfig.registrationKey`와 일치하지 않으면 `1401 (UNAUTHORIZED)`를 반환합니다.

## POST `/register`
- **설명**: 신규 사용자 정보를 저장합니다. 토큰 발급은 `/token/issue`에서 별도로 수행됩니다.
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
      "id": 1,
      "username": "my-service",
      "createdAt": "2025-01-13T12:00:00",
      "updatedAt": "2025-01-13T12:00:00"
    }
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401` (`UNAUTHORIZED`)
  - 사용자명 중복 등 비즈니스 오류 → `9999` (`FAIL`)

## GET `/users/{username}`
- **설명**: 단일 사용자의 기본 정보를 조회합니다. 민감한 인증 정보(API Key, Refresh Token)는 포함되지 않습니다.
- **요청 헤더**
  - `X-REGISTRATION-KEY: {registration-key}`
- **성공 응답 (`200`, code `0000`)**
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": {
      "id": 1,
      "username": "my-service",
      "createdAt": "2025-01-13T12:00:00",
      "updatedAt": "2025-01-13T12:00:00"
    }
  }
  ```
- **에러 시나리오**
  - 등록 키 불일치 → `1401`
  - 사용자 미존재 → `1404` (`USER_NOT_FOUND`)

## POST `/token/issue`
- **설명**: 등록된 사용자 이름으로 Access/Refresh Token 쌍을 최초 발급합니다.
- **요청 헤더**
  - `X-REGISTRATION-KEY: {registration-key}`
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

## POST `/token/re-issue`
- **설명**: 저장된 Refresh Token을 검증하고 Access/Refresh Token을 재발급합니다.
- **요청 헤더**
  - `X-REGISTRATION-KEY: {registration-key}`
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
