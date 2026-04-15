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
  category: "단축 URL" | "리디렉션";
  name: string;
  method: HttpMethod;
  path: string;
  summary: string;
  description: string;
  tags: string[];
  authentication: "공개";
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
    id: "shorturl-create",
    category: "단축 URL",
    name: "단축 URL 생성",
    method: "POST",
    path: "/api/short-url",
    summary: "원본 URL을 단축 URL로 생성",
    description:
      "폐쇄망 내부 도구 전제로 인증 없이 호출합니다. 만료 시간이 있는 단축 키를 생성하며 동일 키는 재사용하지 않습니다.",
    tags: ["단축", "쓰기"],
    authentication: "공개",
    headers: [
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
    "shortUrl": "http://localhost:8080/r/a1B2c3D4",
    "longUrl": "https://example.com/page",
    "createdAt": "2025-01-18T12:34:56",
    "expiredAt": "2025-01-19T12:34:56"
  }
}`
      },
      {
        code: "9999",
        description: "서버 오류"
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
    authentication: "공개",
    headers: [],
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
      "참조자, UA, 시간 구간 등으로 묶어 집계값을 반환합니다. 폐쇄망 내부 도구 전제로 공개 호출입니다.",
    tags: ["통계", "리포트"],
    authentication: "공개",
    headers: [
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
        description: "REFERER, USER_AGENT, DEVICE_TYPE, OS, BROWSER, COUNTRY, CITY, YEAR, MONTH, DAY, HOUR 등의 그룹 목록"
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

export const specCategories = ["단축 URL", "리디렉션"] as const;
