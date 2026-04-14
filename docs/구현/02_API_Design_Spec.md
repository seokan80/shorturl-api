# Short URL - REST API 설계서

## 1. 개요
본 문서는 `shorturl-api` 시스템의 백엔드 API 명세를 정의합니다. 외부 시스템 및 Admin UI와의 연동을 위해 RESTful 원칙을 준수하며, JSON 형식을 기본 응답 포맷으로 사용합니다.

---

## 2. 공통 사항
- **Base URL**: `http://{domain}/api`
- **Content-Type**: `application/json; charset=utf-8`
- **인증 방식**: 
  - `X-CLIENTACCESS-KEY`: 비회원용 API 키 인증 (헤더 사용)
  - `Authorization: Bearer {JWT}`: 관리자 API 인증
- **공통 응답 규격**: 
  ```json
  {
    "code": "0000",
    "message": "성공",
    "data": { ... }
  }
  ```

---

## 3. API 목록 (Summary)

### 3.1 단축 URL 관리 (Short URL)
| 기능 | 메서드 | 경로 | 설명 |
| :--- | :--- | :--- | :--- |
| 생성 | POST | `/short-url` | 새로운 단축 URL 생성 |
| 목록 | GET | `/short-url` | 생성된 단축 URL 목록 조회 (페이징) |
| 상세 | GET | `/short-url/{id}` | ID 기반 단건 상세 조회 |
| 키 조회 | GET | `/short-url/key/{key}` | 단축 키 기반 상세 조회 |
| 만료수정 | PUT | `/short-url/{id}/expiration` | 단축 URL 만료 일시 수정 |
| 삭제 | DELETE | `/short-url/{id}` | 단축 URL 삭제 |

### 3.2 인증 및 보안 (Auth & Key)
| 기능 | 메서드 | 경로 | 설명 |
| :--- | :--- | :--- | :--- |
| 토큰 발급 | POST | `/auth/token/issue` | API Key 기반 JWT 토큰 발급 |
| 토큰 갱신 | POST | `/auth/token/re-issue` | Refresh Token 기반 JWT 갱신 |
| 클라이언트 키 목록 | GET | `/client-keys` | 발급된 클라이언트 접근 키 목록 |
| 클라이언트 키 생성 | POST | `/client-keys` | 신규 클라이언트 접근 키 발급 |
| 클라이언트 키 수정 | PUT | `/client-keys/{id}` | 키 정보(이름, 만료일 등) 수정 |
| 클라이언트 키 삭제 | DELETE | `/client-keys/{id}` | 클라이언트 접근 키 폐기 |

### 3.3 사용자 관리 (User)
| 기능 | 메서드 | 경로 | 설명 |
| :--- | :--- | :--- | :--- |
| 목록 | GET | `/users` | 전체 사용자 목록 조회 |
| 등록 | POST | `/users` | 신규 서비스 사용자 등록 |
| 정보 수정 | PUT | `/users/{username}` | 사용자 정보(고객사명 등) 수정 |
| 상세 | GET | `/users/{username}` | 사용자 상세 정보 조회 |
| 삭제 | DELETE | `/users/{username}` | 사용자 삭제 |

### 3.4 통계 및 이력 (Analytics)
| 기능 | 메서드 | 경로 | 설명 |
| :--- | :--- | :--- | :--- |
| 리다이렉션 이력 | GET | `/redirections/history` | 전체 리다이렉션 실행 이력 조회 |
| 이력 상세 | GET | `/redirections/history/{id}` | 특정 리다이렉션 로그 상세 조회 |

### 3.5 내부 연동 API (Internal API)
| 기능 | 메서드 | 경로 | 설명 |
| :--- | :--- | :--- | :--- |
| 키 조회 (내부) | GET | `/internal/short-urls/{shortUrlKey}` | 단축 키 기반 원본 정보 조회 |
| 전체 목록 (캐시) | GET | `/internal/short-urls/all` | 캐싱용 전체 단축 URL 목록 반환 |
| 공통 설정 조회 | GET | `/internal/redirection-config` | 리다이렉트 서버 운영 환경 설정 조회 |
| 이력 저장 (내부) | POST | `/internal/redirections/history` | 리다이렉트 발생 이력 서버 전달 저장 |

---

## 4. API 상세 명세

### 4.1 단축 URL 생성 ([POST] /short-url)
- **설명**: 원본 URL을 전달받아 단축 키를 포함한 정보를 생성합니다.
- **Request Body**:

| 필드명 | 타입 | 필수 | 설명 |
| :--- | :--- | :--- | :--- |
| longUrl | String | Y | 원래의 긴 URL 주소 |
| botType | String | N | 연동 봇 구분 (CALLBOT, CHATBOT) |
| botServiceKey | String | N | 봇 서비스 식별 키 |
| surveyId | String | N | 관련 설문 ID |
| surveyVer | String | N | 관련 설문 버전 |

- **Sample Response**:
```json
{
  "code": "0000",
  "message": "성공",
  "data": {
    "id": 12,
    "shortKey": "nh82k3p",
    "shortUrl": "http://n.nh/nh82k3p",
    "longUrl": "https://survey.nh.com/...",
    "createdBy": "admin",
    "createdAt": "2025-12-23T22:00:00"
  }
}
```

### 4.2 단축 URL 목록 ([GET] /short-url)
- **설명**: 생성된 단축 URL 목록을 페이징하여 조회합니다.
- **Request Parameters**:

| 필드명 | 타입 | 필수 | 설명 |
| :--- | :--- | :--- | :--- |
| page | Integer | N | 페이지 번호 (Default: 0) |
| size | Integer | N | 페이지 크기 (Default: 20) |
| sort | String | N | 정렬 조건 (예: createdAt,desc) |

### 4.3 리다이렉션 실행 이력 상세 ([GET] /redirections/history/{id})
- **설명**: 특정 리다이렉션 이벤트의 상세 정보(IP, UserAgent 등)를 조회합니다.
- **Response Data**:

| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| id | Long | 이력 고유 ID |
| ip | String | 접속자 IP |
| os | String | 운영체제 |
| browser | String | 브라우저 명칭 |
| deviceType | String | 기기 구분 |
| redirectAt | LocalDateTime | 리다이렉션 발생 시점 |

### 4.4 내부 연동 API 상세

#### 4.4.1 단축 URL 키 조회 ([GET] /internal/short-urls/{shortUrlKey})
- **설명**: 리다이렉트 서버에서 특정 단축 키의 원본 정보를 조회할 때 사용합니다.
- **Response Data**: `ShortUrlResponse` 규격과 동일

#### 4.4.2 리다이렉션 설정 조회 ([GET] /internal/redirection-config)
- **설명**: 시스템 전역 리다이렉션 정책(Fallback URL, 호스트 정보 등)을 조회합니다.
- **Response Data**:

| 필드명 | 타입 | 설명 |
| :--- | :--- | :--- |
| fallbackUrl | String | 매칭되는 URL이 없을 경우 이동할 기본 주소 |
| defaultHost | String | 단축 URL 생성 시 사용되는 기본 도메인 |
| showErrorPage | Boolean | 오류 발생 시 안내 페이지 노출 여부 |
| trackingFields | String | 분석을 위해 보존할 쿼리 파라미터 목록 |

---

## 5. 공통 오류 코드
| 코드 | 메시지 | 설명 |
| :--- | :--- | :--- |
| 0000 | 성공 | 정상 처리 |
| E401 | 인증에 실패하였습니다. | 토큰 만료 또는 유효하지 않은 키 |
| E403 | 권한이 없습니다. | 접근 불가능한 자원 요청 |
| E404 | 리소스를 찾을 수 없습니다. | 존재하지 않는 ID 또는 URL 키 |
| E500 | 서버 내부 오류가 발생했습니다. | 시스템 내부 예외 발생 |
