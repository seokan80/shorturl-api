export type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

export type SpecStatus = "작성 중" | "검토 대기" | "승인됨";

export interface ApiResponse {
  code: string;
  description: string;
  sample?: string;
}

export interface ApiHeader {
  name: string;
  required: boolean;
  value?: string;
  description: string;
}

export interface ApiBodyField {
  name: string;
  type: string;
  required: boolean;
  description: string;
}

export interface ApiSpec {
  id: string;
  category: "인증" | "단축 URL" | "리디렉션";
  name: string;
  method: HttpMethod;
  path: string;
  summary: string;
  description: string;
  tags: string[];
  authentication: "공개" | "등록 키" | "JWT";
  headers: ApiHeader[];
  requestBody?: ApiBodyField[];
  responses: ApiResponse[];
  lastUpdated: string;
  owner: string;
  version: string;
  status: SpecStatus;
}

export const apiSpecs: ApiSpec[] = [
  {
    id: "user-register",
    category: "인증",
    name: "사용자 등록",
    method: "POST",
    path: "/api/users",
    summary: "클라이언트 키로 신규 사용자 계정을 추가",
    description:
      "사전에 공유된 클라이언트 키를 검증한 뒤 신규 사용자 계정을 생성합니다. 토큰 발급은 별도의 엔드포인트에서 수행됩니다.",
    tags: ["사용자", "등록"],
    authentication: "등록 키",
    headers: [
      {
        name: "X-CLIENTACCESS-KEY",
        required: true,
        description: "Oracle 설정에 저장된 마스터 등록 키"
      },
      {
        name: "Content-Type",
        required: true,
        value: "application/json",
        description: "JSON 요청 본문 형식"
      }
    ],
    requestBody: [
      {
        name: "username",
        type: "string",
        required: true,
        description: "소비자 서비스에서 사용할 고유 아이디"
      }
    ],
    responses: [
      {
        code: "200",
        description: "성공 시 사용자명을 반환",
        sample: `{
  "code": "0000",
  "message": "Success",
  "data": {
    "username": "my-service"
  }
}`
      },
      {
        code: "1401",
        description: "등록 키가 없거나 잘못됨"
      },
      {
        code: "9999",
        description: "중복 사용자명 또는 서버 오류"
      }
    ],
    lastUpdated: "2025-01-12",
    owner: "플랫폼 스쿼드",
    version: "v1.3",
    status: "검토 대기"
  },
  {
    id: "auth-token",
    category: "인증",
    name: "토큰 발급",
    method: "POST",
    path: "/api/auth/token/issue",
    summary: "등록된 사용자명으로 Access/Refresh Token 발급",
    description:
      "등록 키 헤더를 검증한 뒤 지정된 사용자명으로 Access Token과 Refresh Token을 동시에 발급합니다.",
    tags: ["인증", "발급"],
    authentication: "등록 키",
    headers: [
      {
        name: "X-CLIENTACCESS-KEY",
        required: true,
        description: "Oracle 설정에 저장된 마스터 등록 키"
      }
    ],
    requestBody: [
      {
        name: "username",
        type: "string",
        required: true,
        description: "토큰을 발급받을 서비스 계정"
      }
    ],
    responses: [
      {
        code: "200",
        description: "Access/Refresh Token 쌍이 data에 포함",
        sample: `{
  "code": "0000",
  "message": "Success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "f26c0d12-..."
  }
}`
      },
      {
        code: "1403",
        description: "사용자를 찾지 못함"
      }
    ],
    lastUpdated: "2025-01-13",
    owner: "플랫폼 스쿼드",
    version: "v1.4",
    status: "승인됨"
  },
  {
    id: "auth-token-reissue",
    category: "인증",
    name: "토큰 재발급",
    method: "POST",
    path: "/api/auth/token/re-issue",
    summary: "Refresh Token 검증 후 Access/Refresh Token 재발급",
    description:
      "등록 키와 Refresh Token을 검증한 뒤 Access Token과 Refresh Token을 모두 회전시킵니다.",
    tags: ["인증", "회전"],
    authentication: "등록 키",
    headers: [
      {
        name: "X-CLIENTACCESS-KEY",
        required: true,
        description: "Oracle 설정에 저장된 마스터 등록 키"
      }
    ],
    requestBody: [
      {
        name: "username",
        type: "string",
        required: true,
        description: "재발급 받을 서비스 계정"
      },
      {
        name: "refreshToken",
        type: "string",
        required: true,
        description: "이전에 발급된 Refresh Token"
      }
    ],
    responses: [
      {
        code: "200",
        description: "새 Access/Refresh Token이 data에 포함",
        sample: `{
  "code": "0000",
  "message": "Success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "0d1ea972-..."
  }
}`
      },
      {
        code: "1403",
        description: "사용자/Refresh Token 불일치"
      }
    ],
    lastUpdated: "2025-01-11",
    owner: "플랫폼 스쿼드",
    version: "v1.0",
    status: "승인됨"
  },
  {
    id: "shorturl-create",
    category: "단축 URL",
    name: "단축 URL 생성",
    method: "POST",
    path: "/api/short-url",
    summary: "원본 URL을 단축 URL로 생성",
    description:
      "JWT 인증을 요구하며 만료 시간이 있는 단축 키를 생성합니다. 동일 키는 재사용하지 않습니다.",
    tags: ["단축", "쓰기"],
    authentication: "JWT",
    headers: [
      {
        name: "Authorization",
        value: "Bearer {API_KEY}",
        required: true,
        description: "인증 엔드포인트가 발급한 JWT"
      },
      {
        name: "Content-Type",
        value: "application/json",
        required: true,
        description: "JSON 요청 본문"
      }
    ],
    requestBody: [
      {
        name: "longUrl",
        type: "string",
        required: true,
        description: "원본 목적지 URL"
      },
      {
        name: "username",
        type: "string",
        required: true,
        description: "URL을 소유한 서비스 계정"
      }
    ],
    responses: [
      {
        code: "200",
        description: "ShortUrlResponse 형태로 반환",
        sample: `{
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
}`
      },
      {
        code: "9999",
        description: "존재하지 않는 계정이거나 서버 오류"
      }
    ],
    lastUpdated: "2025-01-10",
    owner: "그로스 스쿼드",
    version: "v1.0",
    status: "검토 대기"
  },
  {
    id: "shorturl-detail",
    category: "단축 URL",
    name: "단축 URL 조회",
    method: "GET",
    path: "/api/short-url/{id}",
    summary: "ID 기준 단축 URL 상세 조회",
    description:
      "리소스가 존재하고 만료되지 않았다면 전체 ShortUrlResponse 정보를 반환합니다.",
    tags: ["단축", "조회"],
    authentication: "JWT",
    headers: [
      {
        name: "Authorization",
        required: true,
        value: "Bearer {API_KEY}",
        description: "인증 엔드포인트가 발급한 JWT"
      }
    ],
    responses: [
      {
        code: "200",
        description: "ShortUrlResponse payload"
      },
      {
        code: "1404",
        description: "단축 URL이 없거나 만료됨"
      }
    ],
    lastUpdated: "2025-01-08",
    owner: "그로스 스쿼드",
    version: "v1.0",
    status: "승인됨"
  },
  {
    id: "redirection-redirect",
    category: "리디렉션",
    name: "단축 URL 리디렉션",
    method: "GET",
    path: "/r/{shortKey}",
    summary: "공개 리다이렉트 엔드포인트",
    description:
      "단축 키를 원본 URL로 변환해 HTTP 302 응답을 내보내며, 히스토리 메타데이터를 기록합니다.",
    tags: ["리디렉션", "공개"],
    authentication: "공개",
    headers: [],
    responses: [
      {
        code: "302",
        description: "원본 URL로 리디렉션"
      },
      {
        code: "302 (/error)",
        description: "잘못된 키일 때 오류 페이지로 리디렉션"
      }
    ],
    lastUpdated: "2025-01-05",
    owner: "코어 플랫폼",
    version: "v1.2",
    status: "승인됨"
  },
  {
    id: "redirection-stats",
    category: "리디렉션",
    name: "리디렉션 통계",
    method: "POST",
    path: "/r/history/{shortUrlId}/stats",
    summary: "리디렉션 집계 통계",
    description:
      "참조자, UA, 시간 구간 등으로 묶어 집계값을 반환합니다. JWT 인증이 필요합니다.",
    tags: ["통계", "리포트"],
    authentication: "JWT",
    headers: [
      {
        name: "Authorization",
        required: true,
        value: "Bearer {API_KEY}",
        description: "인증 엔드포인트가 발급한 JWT"
      },
      {
        name: "Content-Type",
        required: true,
        value: "application/json",
        description: "JSON 요청 본문"
      }
    ],
    requestBody: [
      {
        name: "groupBy",
        type: "GroupingType[]",
        required: true,
        description: "REFERER, YEAR, MONTH, USER_AGENT 등의 그룹 목록"
      }
    ],
    responses: [
      {
        code: "200",
        description: "집계 결과 리스트",
        sample: `{
  "code": "0000",
  "message": "Success",
  "data": [
    { "referer": "https://google.com", "year": 2025, "count": 5 },
    { "referer": "https://naver.com", "year": 2025, "count": 2 }
  ]
}`
      },
      {
        code: "9999",
        description: "존재하지 않는 shortUrlId 또는 지원하지 않는 그룹"
      }
    ],
    lastUpdated: "2025-01-09",
    owner: "애널리틱스 길드",
    version: "v1.2",
    status: "검토 대기"
  }
];

export const specCategories = ["인증", "단축 URL", "리디렉션"] as const;
