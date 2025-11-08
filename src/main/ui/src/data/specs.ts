export type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

export type SpecStatus = "Draft" | "Pending Review" | "Approved";

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
  category: "Authentication" | "Short URL" | "Redirection";
  name: string;
  method: HttpMethod;
  path: string;
  summary: string;
  description: string;
  tags: string[];
  authentication: "Public" | "Registration Key" | "JWT";
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
    id: "auth-register",
    category: "Authentication",
    name: "Register API Key",
    method: "POST",
    path: "/api/auth/register",
    summary: "Issue initial API key using registration key",
    description:
      "Registers a new service account and returns the bootstrap API key. Requires the configured registration key header.",
    tags: ["auth", "bootstrap"],
    authentication: "Registration Key",
    headers: [
      {
        name: "X-REGISTRATION-KEY",
        required: true,
        description: "Registration master key configured in Oracle settings"
      },
      {
        name: "Content-Type",
        required: true,
        value: "application/json",
        description: "Payload encoding"
      }
    ],
    requestBody: [
      {
        name: "username",
        type: "string",
        required: true,
        description: "Unique identifier for the consumer service"
      }
    ],
    responses: [
      {
        code: "200",
        description: "Success with username and apiKey payload",
        sample: `{
  "code": "0000",
  "message": "Success",
  "data": {
    "username": "my-service",
    "apiKey": "eyJhbGciOiJIUzUxMiJ9..."
  }
}`
      },
      {
        code: "1401",
        description: "Registration key invalid or missing"
      },
      {
        code: "9999",
        description: "Duplicate username or server failure"
      }
    ],
    lastUpdated: "2025-01-12",
    owner: "Platform Squad",
    version: "v1.3",
    status: "Pending Review"
  },
  {
    id: "auth-token",
    category: "Authentication",
    name: "Reissue API Key",
    method: "POST",
    path: "/api/auth/token",
    summary: "Reissue JWT when existing token expires",
    description:
      "Reissues an API key for an existing service account after validating the registration key and previous token.",
    tags: ["auth", "rotation"],
    authentication: "Registration Key",
    headers: [
      {
        name: "X-REGISTRATION-KEY",
        required: true,
        description: "Registration master key configured in Oracle settings"
      }
    ],
    requestBody: [
      {
        name: "username",
        type: "string",
        required: true,
        description: "Existing service account"
      },
      {
        name: "apiKey",
        type: "string",
        required: true,
        description: "Previously issued API key to refresh"
      }
    ],
    responses: [
      {
        code: "200",
        description: "Success with new token field",
        sample: `{
  "code": "0000",
  "message": "Success",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9..."
  }
}`
      },
      {
        code: "1403",
        description: "User not found or apiKey mismatch"
      }
    ],
    lastUpdated: "2025-01-11",
    owner: "Platform Squad",
    version: "v1.1",
    status: "Approved"
  },
  {
    id: "shorturl-create",
    category: "Short URL",
    name: "Create Short URL",
    method: "POST",
    path: "/api/short-url",
    summary: "Create a new short URL mapping",
    description:
      "Generates an expiring short URL for an existing account. Requires JWT authentication and enforces unique short key generation.",
    tags: ["short-url", "mutation"],
    authentication: "JWT",
    headers: [
      {
        name: "Authorization",
        value: "Bearer {API_KEY}",
        required: true,
        description: "JWT issued by auth endpoints"
      },
      {
        name: "Content-Type",
        value: "application/json",
        required: true,
        description: "Payload encoding"
      }
    ],
    requestBody: [
      {
        name: "longUrl",
        type: "string",
        required: true,
        description: "Original destination URL"
      },
      {
        name: "username",
        type: "string",
        required: true,
        description: "Owning service account"
      }
    ],
    responses: [
      {
        code: "200",
        description: "Success with ShortUrlResponse payload",
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
        description: "Invalid account or unexpected error"
      }
    ],
    lastUpdated: "2025-01-10",
    owner: "Growth Squad",
    version: "v1.0",
    status: "Pending Review"
  },
  {
    id: "shorturl-detail",
    category: "Short URL",
    name: "Get Short URL",
    method: "GET",
    path: "/api/short-url/{id}",
    summary: "Retrieve short URL by numeric identifier",
    description:
      "Returns the full ShortUrlResponse payload when the resource exists and is not expired. Requires JWT authentication.",
    tags: ["short-url", "read"],
    authentication: "JWT",
    headers: [
      {
        name: "Authorization",
        required: true,
        value: "Bearer {API_KEY}",
        description: "JWT issued by auth endpoints"
      }
    ],
    responses: [
      {
        code: "200",
        description: "Success with ShortUrlResponse payload"
      },
      {
        code: "1404",
        description: "Short URL not found or expired"
      }
    ],
    lastUpdated: "2025-01-08",
    owner: "Growth Squad",
    version: "v1.0",
    status: "Approved"
  },
  {
    id: "redirection-redirect",
    category: "Redirection",
    name: "Redirect to Long URL",
    method: "GET",
    path: "/r/{shortKey}",
    summary: "Public redirect entry point",
    description:
      "Resolves the short key and issues an HTTP 302 redirect to the long URL while tracking history metadata.",
    tags: ["redirection", "public"],
    authentication: "Public",
    headers: [],
    responses: [
      {
        code: "302",
        description: "Redirect to long URL"
      },
      {
        code: "302 (/error)",
        description: "Fallback redirect when the key is invalid"
      }
    ],
    lastUpdated: "2025-01-05",
    owner: "Core Platform",
    version: "v1.2",
    status: "Approved"
  },
  {
    id: "redirection-stats",
    category: "Redirection",
    name: "Redirection Stats",
    method: "POST",
    path: "/r/history/{shortUrlId}/stats",
    summary: "Aggregated redirection metrics",
    description:
      "Returns aggregated metrics grouped by referer, user agent, and temporal dimensions. Requires JWT authentication.",
    tags: ["analytics", "reporting"],
    authentication: "JWT",
    headers: [
      {
        name: "Authorization",
        required: true,
        value: "Bearer {API_KEY}",
        description: "JWT issued by auth endpoints"
      },
      {
        name: "Content-Type",
        required: true,
        value: "application/json",
        description: "Payload encoding"
      }
    ],
    requestBody: [
      {
        name: "groupBy",
        type: "GroupingType[]",
        required: true,
        description: "List of groupings like REFERER, YEAR, MONTH, USER_AGENT"
      }
    ],
    responses: [
      {
        code: "200",
        description: "List of aggregated metrics",
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
        description: "Invalid shortUrlId or unsupported grouping"
      }
    ],
    lastUpdated: "2025-01-09",
    owner: "Analytics Guild",
    version: "v1.2",
    status: "Pending Review"
  }
];

export const specCategories = ["Authentication", "Short URL", "Redirection"] as const;
