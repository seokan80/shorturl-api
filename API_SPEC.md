# API Endpoint Specification

Base URL: `https://{host}` (local development defaults to `http://localhost:8080`). All JSON bodies use UTF-8 encoding.

## Common Response Envelope
- Every REST endpoint returns JSON in the shape:
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": { ... } // or primitive/array/null depending on the endpoint
  }
  ```
- `code`/`message` follow `ApiResult`; notable values: `0000` success, `1401` unauthorized, `1403` forbidden, `1404` not found, `9999` generic failure.
- Authenticated endpoints require `Authorization: Bearer {API_KEY}`.

## Authentication APIs (`/api/auth`)

### POST `/api/auth/register`
- **Auth**: Registration key header (no JWT)
- **Headers**: `X-REGISTRATION-KEY: {registration-key}`
- **Request Body**:
  ```json
  { "username": "my-service" }
  ```
- **Success 200**:
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
- **Failure**: wrong key → `1401`; duplicate username → `9999`.

### POST `/api/auth/token`
- **Auth**: Registration key header (no JWT)
- **Headers**: `X-REGISTRATION-KEY: {registration-key}`
- **Request Body**:
  ```json
  {
    "username": "my-service",
    "apiKey": "expired-jwt"
  }
  ```
- **Success 200**:
  ```json
  {
    "code": "0000",
    "message": "Success",
    "data": { "token": "eyJhbGciOiJIUzUxMiJ9..." }
  }
  ```
- **Failure**: unknown user or mismatched key → `1403`.

## Short URL Management (`/api/short-url`)

### POST `/api/short-url`
- **Auth**: JWT required
- **Request Body**:
  ```json
  {
    "longUrl": "https://example.com/page",
    "username": "my-service"
  }
  ```
- **Success 200** (`ShortUrlResponse`):
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
      "createdAt": "2025-01-18T12:34:56.000",
      "expiredAt": "2025-01-19T12:34:56.000"
    }
  }
  ```
- **Failure**: invalid user or server error → `9999`.

### GET `/api/short-url/{id}`
- **Auth**: JWT required.
- **Path**: `id` numeric ShortUrl identifier.
- **Success 200**: same payload as create.
- **Failure**: unknown/expired URL → `1404`.

### GET `/api/short-url/key/{shortKey}`
- **Auth**: JWT required.
- **Path**: `shortKey` assigned slug string.
- **Success 200**: same payload as create.
- **Failure**: unknown/expired key → `1404`.

### DELETE `/api/short-url/{id}`
- **Auth**: JWT required.
- **Action**: deletes the short URL; no response body.
- **Success 204**.
- **Failure**: repository layer throws if ID missing; expect `404` propagated by Spring.

## Redirection & Analytics (`/r`)

### GET `/r/{shortKey}`
- **Auth**: public.
- **Behavior**: Redirects (302) to the stored long URL; records referer, user-agent, client IP.
- **Failure**: invalid key → redirects to `/error`.

### GET `/r/history/{shortUrlId}/count`
- **Auth**: JWT required.
- **Success 200**:
  ```json
  { "code": "0000", "message": "Success", "data": 15 }
  ```
- **Failure**: unknown ID → `9999`.

### POST `/r/history/{shortUrlId}/stats`
- **Auth**: JWT required.
- **Request Body**:
  ```json
  { "groupBy": ["REFERER", "YEAR"] }
  ```
  Supported groupings: `REFERER`, `USER_AGENT`, `YEAR`, `MONTH`, `DAY`, `HOUR`.
- **Success 200** (example):
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
- **Failure**: invalid ID or grouping → `9999`.
