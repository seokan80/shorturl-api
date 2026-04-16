# 09. Admin / Redirect 서버 분리 설계서

## Executive Summary

| 항목 | 내용 |
|---|---|
| 문서 번호 | 09 |
| 작성일 | 2026-04-16 |
| 브랜치 | `feature/unified-app-merge` |
| 선행 문서 | [07_Unified_App_Merge_Design.md](07_Unified_App_Merge_Design.md) (단일앱 통합 → **본 문서에서 역전**) |

### Value Delivered

| 관점 | 내용 |
|---|---|
| Problem | 리다이렉트 서비스가 외부 Tomcat(WAR)에 배포되어야 하나 07 설계는 단일 JAR로 통합했음. 운영 환경의 Tomcat 인프라를 활용할 수 없고, 트래픽 격리가 불가능 |
| Solution | admin=JAR(내장 Tomcat), redirect=WAR(외부 Tomcat) 분리. WebClient 기반 내부 API 통신. Caffeine per-entry 만료 + 5분 증분 폴링 |
| Function UX | 리다이렉트 응답 지연 최소화(캐시 100% hit), 관리 API 독립 운영, 기존 Tomcat 인프라 재활용 |
| Core Value | 장애 격리(관리 API 장애 → 리다이렉트 영향 없음), 독립 스케일링, 운영 유연성 |

---

## 1. 배경 및 설계 변경 이유

### 1.1 07 설계(단일앱 통합)의 한계

07 문서는 admin + redirect를 하나의 Spring Boot JAR로 통합했다. 이 설계는 다음 운영 제약과 충돌했다:

1. **기존 Tomcat 인프라 활용 불가** — 폐쇄망 내 리다이렉트 서비스는 기존 외부 Tomcat(WAS)에 WAR 배포해야 하는 운영 정책이 있음
2. **장애 격리 불가** — 관리 API의 JPA 세션풀 고갈·배포 재기동이 리다이렉트 서비스에 직접 영향
3. **독립 스케일링 불가** — 리다이렉트 트래픽 급증 시 admin까지 함께 증설해야 하는 비효율
4. **컨텍스트 패스 분리** — 리다이렉트 서버는 `/s/` 컨텍스트, admin은 `/` (또는 별도 도메인)

### 1.2 결정

> **admin = JAR (내장 Tomcat)**, **redirect = WAR (외부 Tomcat)** 로 분리.
> 리다이렉트 서버는 DB에 직접 접근하지 않으며, admin의 내부 API를 통해 데이터를 폴링한다.

---

## 2. TO-BE 아키텍처

### 2.1 모듈 구성

```
settings.gradle
├── common              # 공유 DTO, 응답 래퍼, OpenAPI 정의
├── short-url-admin     # JAR (내장 Tomcat) — 관리 API + Admin UI
├── short-url-admin-ui  # React 18 + Vite (Gradle 빌드 시 admin JAR에 포함)
└── short-url-redirect  # WAR (외부 Tomcat) — 리다이렉트 전용
```

### 2.2 런타임 아키텍처

```
                   Internet / 내부망
                       │
               ┌───────┴───────┐
               │  L4/L7 LB     │
               └───┬───────┬───┘
                   │       │
        ┌──────────┴──┐  ┌─┴──────────────┐
        │  Tomcat A   │  │  Tomcat B       │
        │  /s/ (WAR)  │  │  /s/ (WAR)     │
        │  redirect   │  │  redirect      │
        │  Caffeine   │  │  Caffeine      │
        └──────┬──────┘  └──────┬─────────┘
               │                │
               │  WebClient     │  WebClient
               │  (HTTP)        │  (HTTP)
               │                │
        ┌──────┴────────────────┴─────────┐
        │        Admin Server (JAR)        │
        │  port 8080                       │
        │  /api/** (관리 API)              │
        │  /api/internal/** (내부 API)     │
        │  / (Admin UI - React SPA)        │
        └──────────────┬──────────────────┘
                       │
                   ┌───┴───┐
                   │ Oracle │
                   │  (DB)  │
                   └───────┘
```

### 2.3 통신 패턴

| 방향 | 프로토콜 | 엔드포인트 | 주기 | 목적 |
|---|---|---|---|---|
| redirect → admin | HTTP GET | `/api/internal/short-urls/all` | 기동 시 1회 | 캐시 워밍 (full load) |
| redirect → admin | HTTP GET | `/api/internal/short-urls/changes?since=` | 5분 | 증분 캐시 동기화 |
| redirect → admin | HTTP GET | `/api/internal/redirection-config` | 60초 | 리다이렉트 설정 폴링 |
| redirect → admin | HTTP POST | `/api/internal/redirections/history` | 리다이렉트 발생 시 | 이력 비동기 저장 |

---

## 3. 모듈별 상세 설계

### 3.1 `short-url-admin` (JAR)

#### 패키지 구조

```
com.nh.shorturl.admin
├── ShortUrlAdminApplication.java      # @SpringBootApplication, @EnableJpaAuditing
├── config/
│   └── SpaFallbackController.java     # React BrowserRouter deep-link 지원
├── constants/
│   └── SchemaConstants.java
├── controller/
│   ├── ShortUrlController.java        # /api/short-url (공개 CRUD)
│   ├── RedirectionHistoryController.java  # /api/redirection-history
│   └── InternalApiController.java     # /api/internal/** (redirect 서버 전용)
├── entity/
│   ├── common/BaseEntity.java         # @CreatedDate, @LastModifiedDate
│   ├── ShortUrl.java                  # key→URL 매핑, soft-delete
│   └── RedirectionHistory.java        # 리다이렉트 이력
├── exception/
│   └── GlobalExceptionHandler.java    # @RestControllerAdvice
├── repository/
│   ├── ShortUrlRepository.java        # JPA + 네이티브 쿼리(findChangedSince)
│   ├── RedirectionHistoryRepository.java
│   └── RedirectionHistoryRepositoryCustom(Impl).java  # QueryDSL
├── service/
│   ├── shorturl/
│   │   ├── ShortUrlService.java       # 인터페이스
│   │   └── ShortUrlServiceImpl.java   # CRUD + findAllForCaching + findChangedSince
│   └── history/
│       ├── RedirectionHistoryService.java
│       └── RedirectionHistoryServiceImpl.java
├── converter/
│   └── BooleanToYnConverter.java
└── util/
    ├── Base62.java                    # UUID → 단축 키 인코딩
    ├── DateUtils.java
    ├── RequestInfoUtils.java
    └── UserAgentParser.java
```

#### 내부 API 상세 (InternalApiController)

| 메서드 | 경로 | 파라미터 | 응답 | 설명 |
|---|---|---|---|---|
| GET | `/api/internal/short-urls/all` | — | `List<ShortUrlResponse>` | 만료되지 않은 전체 목록 (캐시 워밍) |
| GET | `/api/internal/short-urls/changes` | `since: LocalDateTime` | `List<ShortUrlResponse>` | 증분 변경분 (삭제 포함) |
| GET | `/api/internal/redirection-config` | — | `RedirectionConfigResponse` | 리다이렉트 설정 |
| POST | `/api/internal/redirections/history` | `RedirectionHistoryRequest` | 200 OK | 이력 저장 |

#### 증분 동기화 쿼리

```sql
-- @SQLRestriction("IS_DEL = 'N'") 을 우회하기 위해 네이티브 쿼리 사용
SELECT * FROM TBL_SHORT_URL
WHERE UPDATED_AT >= :since OR DELETED_AT >= :since
```

- soft-delete 된 항목도 반환 → `ShortUrlResponse.deleted=true` 로 표시
- redirect 서버는 `deleted=true` 또는 `expiredAt < now` 인 항목을 캐시에서 evict

#### 빌드

```gradle
// JAR 빌드 — admin-ui 정적 자산 포함
bootJar { enabled = true }

processResources {
    dependsOn ':short-url-admin-ui:yarnBuild'
    from(project(':short-url-admin-ui').layout.buildDirectory.dir('dist')) {
        into 'static'
    }
}
```

- **출력**: `short-url-admin-0.0.1-SNAPSHOT.jar`
- **실행**: `java -jar short-url-admin-*.jar --spring.profiles.active=dev`
- **포트**: 8080

---

### 3.2 `short-url-redirect` (WAR)

#### 패키지 구조

```
com.nh.shorturl.redirect
├── ShortUrlRedirectApplication.java   # SpringBootServletInitializer (WAR)
├── config/
│   └── AppConfig.java                 # WebClient + Caffeine Cache 빈
├── controller/
│   └── ShortUrlRedirectController.java  # /{shortKey} (리다이렉트) + /verify
└── service/
    ├── ShortUrlCacheService.java      # Caffeine Cache 래퍼 (put/get/evict)
    ├── ShortUrlCacheSyncer.java       # 기동 시 full load + 5분 증분 폴링
    ├── RedirectionConfigStore.java    # 60초 설정 폴링
    └── RedirectionHistoryService.java # 비동기 이력 POST
```

#### 핵심 설계: 캐시 전략

**per-entry Caffeine 만료**:

```java
Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfter(new Expiry<String, ShortUrlResponse>() {
        // 생성/갱신 시: expiredAt 까지 남은 시간을 TTL 로 설정
        // expiredAt 이 null 이면 24시간 기본값
        // 읽기 시: 기존 TTL 유지 (소비 만료 아님)
    })
    .build();
```

| 항목 | 값 | 근거 |
|---|---|---|
| `maximumSize` | 10,000 | 예상 활성 URL 수 대비 충분한 여유 |
| per-entry TTL | `expiredAt - now` | URL별 만료 시각에 정확히 맞춤 |
| null expiredAt 기본 TTL | 24시간 | 만료 미지정 URL도 무한 캐시 방지 |

**캐시 동기화 흐름**:

```
[기동] ──► fullLoad()
             GET /api/internal/short-urls/all
             → 전체 put() (per-entry TTL 자동 설정)

[5분 주기] ──► incrementalSync()
                GET /api/internal/short-urls/changes?since={lastSyncTime}
                → deleted=true || expired → evict()
                → 그 외 → put() (TTL 자동 갱신)
```

#### 리다이렉트 흐름

```
GET /{shortKey}
  │
  ├─ cacheService.get(shortKey)
  │   ├─ 존재 → targetUrl 확정
  │   └─ null  → handleFailure()
  │
  ├─ trackingFields 전파 (utm_source, utm_medium, utm_campaign)
  │
  ├─ historyService.save(shortKey, request)  ← 비동기 (fire-and-forget)
  │
  └─ response.sendRedirect(targetUrl)  ← 302 Redirect
```

**실패 처리 전략**:

| 조건 | 동작 |
|---|---|
| `config.fallbackUrl` 존재 | 302 → fallback URL |
| `config.showErrorPage = true` | 인라인 HTML 에러 페이지 |
| 그 외 | 404 응답 |

#### 빌드

```gradle
apply plugin: 'war'
bootJar { enabled = false }
bootWar { enabled = true }
war   { enabled = false }
```

- **출력**: `short-url-redirect-0.0.1-SNAPSHOT.war`
- **배포**: 외부 Tomcat의 `/s/` 컨텍스트
- **실행**: Tomcat `webapps/` 디렉터리에 WAR 배치

---

### 3.3 `common` (공유 라이브러리)

admin과 redirect 양쪽에서 사용하는 DTO, 응답 래퍼, 열거형을 담는다.

| 패키지 | 주요 클래스 | 용도 |
|---|---|---|
| `dto.request.shorturl` | `ShortUrlRequest`, `ShortUrlUpdateRequest` | 생성/수정 요청 |
| `dto.request.history` | `RedirectionHistoryRequest` | 이력 저장 요청 |
| `dto.response.shorturl` | `ShortUrlResponse` | URL 조회 응답 (deleted 플래그 포함) |
| `dto.response.control` | `RedirectionConfigResponse` | 리다이렉트 설정 |
| `dto.response.common` | `ResultEntity`, `ResultList`, `Result` | 공통 응답 래퍼 |
| `type` | `ApiResult`, `GroupingType` | 결과 코드, 그룹핑 유형 열거형 |

---

### 3.4 `short-url-admin-ui` (React SPA)

- Vite + React 18 + TypeScript + Tailwind CSS
- Gradle node 플러그인으로 빌드 → admin JAR의 `static/` 에 포함
- 개발 시 `yarn dev` (Vite HMR, `/api/**` 프록시 → admin 8080)

---

## 4. 데이터 모델

### 4.1 ShortUrl 엔티티

```java
@Entity
@Table(name = "TBL_SHORT_URL")
@SQLDelete(sql = "UPDATE TBL_SHORT_URL SET IS_DEL = 'Y', DELETED_AT = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("IS_DEL = 'N'")
public class ShortUrl extends BaseEntity {
    Long id;              // PK
    String longUrl;       // 원본 URL (2000자)
    String shortUrl;      // 단축 키 (100자, UNIQUE)
    LocalDateTime expiredAt;  // 만료 일시
    Boolean deleted;      // IS_DEL ('Y'/'N')
    LocalDateTime deletedAt;  // 삭제 일시
    // BaseEntity: createdAt, updatedAt (JPA Auditing)
}
```

### 4.2 ShortUrlResponse DTO

```java
public class ShortUrlResponse {
    Long id;
    String shortKey;      // 단축 키
    String shortUrl;      // 전체 단축 URL (publicUrl + shortKey)
    String longUrl;       // 원본 URL
    LocalDateTime createdAt;
    String expiredAt;     // ISO-8601 문자열 또는 null
    Boolean deleted;      // 증분 동기화 시 evict 판단용
}
```

### 4.3 ERD (현재 구조)

```
┌──────────────────────────────┐
│          TBL_SHORT_URL        │
├──────────────────────────────┤
│ ID           (PK, IDENTITY)   │
│ LONG_URL     (NOT NULL, 2000) │
│ SHORT_URL    (NOT NULL, UNIQUE)│
│ EXPIRED_AT   (TIMESTAMP)      │
│ IS_DEL       (CHAR(1), 'N')   │
│ DELETED_AT   (TIMESTAMP)      │
│ CREATED_AT   (TIMESTAMP)      │
│ UPDATED_AT   (TIMESTAMP)      │
└─────────────┬────────────────┘
              │ 1:N
┌─────────────┴────────────────┐
│   TBL_REDIRECTION_HISTORY     │
├──────────────────────────────┤
│ ID           (PK, IDENTITY)   │
│ SHORT_URL_KEY (VARCHAR)       │
│ REFERER      (VARCHAR)        │
│ USER_AGENT   (VARCHAR)        │
│ IP           (VARCHAR)        │
│ DEVICE_TYPE  (VARCHAR)        │
│ OS           (VARCHAR)        │
│ BROWSER      (VARCHAR)        │
│ REDIRECT_AT  (TIMESTAMP)      │
│ CREATED_AT   (TIMESTAMP)      │
└──────────────────────────────┘
```

> 07 설계 대비 제거된 테이블: `TBL_USER`, `TBL_CLIENT_ACCESS_KEY`
> 제거된 FK: `ShortUrl.user_id`, `ShortUrl.client_access_key_id`

---

## 5. 프로파일별 설정

### 5.1 Admin Server

| 설정 | local | dev | prod |
|---|---|---|---|
| DB | H2 (file mode) | Oracle (JNDI) | Oracle (JNDI) |
| DDL | update | validate | none |
| 포트 | 8080 | 8080 | 8080 |
| public-url | `http://localhost:8080/r/` | `http://dev.domain/s/` | `https://domain/s/` |

### 5.2 Redirect Server

| 설정 | local | dev | prod |
|---|---|---|---|
| Admin API | `http://localhost:8080` | `http://admin.dev.domain` | `http://admin.domain` |
| 포트 | 8081 | Tomcat 기본 | Tomcat 기본 |
| 컨텍스트 | `/` | `/s` | `/s` |

---

## 6. 설계 적절성 검토

### 6.1 긍정적 측면

| # | 항목 | 평가 |
|---|---|---|
| 1 | **장애 격리** | admin 재기동/장애 시에도 redirect는 Caffeine 캐시로 정상 서비스 지속. 역방향도 마찬가지 |
| 2 | **운영 인프라 호환** | 기존 Tomcat WAS에 WAR 배포 — 폐쇄망 인프라 정책 충족 |
| 3 | **캐시 정확도** | per-entry TTL이 expiredAt에 정확히 맞춤 — 글로벌 TTL 대비 정밀 |
| 4 | **DB 접근 최소화** | redirect 서버가 DB에 직접 접근하지 않음 — DB 드라이버/커넥션풀 불필요 |
| 5 | **증분 폴링** | 5분 주기 증분 동기화로 네트워크/DB 부하 최소화 (전체 로드는 기동 시 1회) |
| 6 | **단순한 의존성** | redirect 서버에 JPA/DB 드라이버 없음 — WAR 크기 최소화, 기동 빠름 |

### 6.2 리스크 및 개선 필요 사항

| # | 리스크 | 영향도 | 현재 상태 | 개선 방안 |
|---|---|---|---|---|
| R1 | **Admin 장애 시 캐시 정체** | 중 | 5분 폴링 실패 시 기존 캐시로 동작, 신규 URL만 누락 | 폴링 실패 카운터 + 임계치 초과 시 알람 로그 |
| R2 | **XSS 취약점** | 상 | `buildErrorHtml(reason)` 이스케이프 없이 삽입 | Thymeleaf 템플릿 또는 `HtmlUtils.htmlEscape()` 적용 |
| R3 | **인라인 HTML** | 중 | 에러 페이지가 Java 문자열로 생성 | `resources/templates/` 로 추출 |
| R4 | **full load 실패 시 서비스 불가** | 상 | 기동 시 admin이 죽어있으면 빈 캐시로 시작 | 재시도 로직 (exponential backoff) + 헬스체크 |
| R5 | **증분 동기화 공백** | 중 | 5분 간격 동안 신규/수정 URL이 redirect에 미반영 | SLA 허용 범위 확인. 운영 시 1분~5분 조정 가능하도록 설정 외부화 |
| R6 | **RedirectionHistoryService fire-and-forget** | 하 | POST 실패 시 이력 유실 | 로컬 파일 버퍼 또는 재시도 큐 검토 (현재 SLA에서는 허용) |
| R7 | **컨트롤러 try-catch 난립** | 중 | ShortUrlController에 각 메서드별 try-catch | GlobalExceptionHandler로 통합 (refactoring-2026-04 P0-1) |
| R8 | **테스트 부재** | 상 | 백엔드 단위/통합 테스트 사실상 없음 | 핵심 경로 테스트 작성 필수 |
| R9 | **문서-코드 불일치** | 중 | 01/02/07 문서가 이전 아키텍처 기술 | 본 문서로 대체. 01/02 문서 별도 업데이트 필요 |

---

## 7. 개선 계획

### Phase 1: 즉시 (보안 + 안정성) — 1~2일

| ID | 작업 | 대상 파일 | 우선순위 |
|---|---|---|---|
| I-1 | XSS 방어: `buildErrorHtml(reason)` 이스케이프 적용 | redirect/controller/ShortUrlRedirectController.java | **P0** |
| I-2 | 에러 페이지 템플릿 분리 | redirect/resources/templates/redirect-error.html | **P0** |
| I-3 | CacheSyncer fullLoad 재시도 (3회, exponential backoff) | redirect/service/ShortUrlCacheSyncer.java | **P0** |
| I-4 | CacheSyncer 폴링 실패 카운터 + WARN 로그 | redirect/service/ShortUrlCacheSyncer.java | P1 |

### Phase 2: 코드 품질 — 3~5일

| ID | 작업 | 대상 파일 | 우선순위 |
|---|---|---|---|
| I-5 | GlobalExceptionHandler 도입 (admin) | admin/exception/GlobalExceptionHandler.java | **P0** |
| I-6 | ShortUrlController try-catch 제거 | admin/controller/ShortUrlController.java | P1 |
| I-7 | 증분 폴링 주기 설정 외부화 (`sync.interval-ms`) | redirect/application.yml + CacheSyncer | P1 |
| I-8 | RedirectionConfigStore 폴링 주기 설정 외부화 | redirect/application.yml + ConfigStore | P2 |

### Phase 3: 테스트 강화 — 5~7일

| ID | 작업 | 테스트 대상 | 우선순위 |
|---|---|---|---|
| I-9 | `Base62Test` — 인코딩 안정성, 길이, 문자셋 | admin/util/Base62.java | P1 |
| I-10 | `ShortUrlServiceImplTest` — 생성/삭제/만료/증분조회 | admin/service/shorturl/ | P1 |
| I-11 | `InternalApiControllerTest` — MockMvc 전체 내부 API | admin/controller/ | P1 |
| I-12 | `ShortUrlCacheSyncerTest` — full load + incremental 시나리오 | redirect/service/ | P1 |
| I-13 | `ShortUrlRedirectControllerTest` — 정상/실패/fallback | redirect/controller/ | P1 |
| I-14 | `RedirectionHistoryRepositoryCustomImplTest` — groupBy | admin/repository/ | P2 |

### Phase 4: 문서 정합화 — 1일

| ID | 작업 | 대상 파일 |
|---|---|---|
| I-15 | 01_Domain_Design_Backend.md 업데이트 (User/ClientAccessKey 제거 반영, 현재 ERD) | docs/구현/01 |
| I-16 | 02_API_Design_Spec.md 업데이트 (Auth/User/ClientKey API 제거, 증분 API 추가) | docs/구현/02 |
| I-17 | CLAUDE.md 현재 4-모듈 구조 반영 | CLAUDE.md |

---

## 8. 빌드 · 배포

### 8.1 빌드 스크립트

```bash
#!/bin/bash
# build-deploy.sh
./gradlew clean \
    :short-url-admin:bootJar \
    :short-url-redirect:bootWar \
    -x test --no-daemon --stacktrace
```

### 8.2 산출물

| 모듈 | 패키징 | 파일 | 배포 대상 |
|---|---|---|---|
| short-url-admin | JAR | `short-url-admin/build/libs/short-url-admin-0.0.1-SNAPSHOT.jar` | 독립 서버 (java -jar) |
| short-url-redirect | WAR | `short-url-redirect/build/libs/short-url-redirect-0.0.1-SNAPSHOT.war` | 외부 Tomcat webapps/ |

### 8.3 배포 순서

1. **Admin 먼저 기동** — redirect의 full load가 admin에 의존
2. **Redirect WAR 배포** — Tomcat 재기동 또는 hot deploy
3. **스모크 테스트** — `/verify` 엔드포인트로 기본 동작 확인

### 8.4 프로파일 지정

```bash
# Admin (JAR)
java -jar short-url-admin-*.jar --spring.profiles.active=dev

# Redirect (WAR) — Tomcat 시스템 프로퍼티 또는 setenv.sh
export JAVA_OPTS="-Dspring.profiles.active=dev"
```

---

## 9. 07 설계와의 비교

| 항목 | 07 (단일앱 통합) | 09 (서버 분리) |
|---|---|---|
| 모듈 수 | 3 (common, admin, admin-ui) | 4 (+ redirect) |
| 배포 산출물 | JAR 1개 | JAR 1개 + WAR 1개 |
| 리다이렉트 서버 DB 접근 | JPA 직접 조회 | 없음 (HTTP 폴링) |
| 캐시 갱신 | refreshAfterWrite 60s (DB 비동기 재로드) | per-entry TTL + 5분 증분 폴링 |
| negative cache | 있음 (30s) | 없음 (캐시 미스 → fallback) |
| 장애 격리 | 불가 | 완전 격리 |
| 기동 의존성 | 없음 (단일 앱) | admin 먼저 기동 필요 |
| 외부 Tomcat | 불가 (JAR only) | redirect WAR 배포 가능 |
| 스케일링 | 전체 스케일링 | 독립 스케일링 |

---

## 10. 검증 포인트

### 10.1 기능 검증

- [ ] `GET /verify` → `config.defaultHost` 로 302 리다이렉트
- [ ] `GET /{shortKey}` (정상) → 원본 URL 로 302 리다이렉트
- [ ] `GET /{shortKey}` (미존재) → fallback URL 로 302 리다이렉트
- [ ] `GET /{shortKey}` (만료) → Caffeine TTL 자동 만료 후 fallback
- [ ] 증분 동기화: admin에서 URL 생성 후 5분 이내 redirect 서버 캐시 반영
- [ ] 증분 동기화: admin에서 URL 삭제 후 5분 이내 redirect 서버 캐시 evict
- [ ] 이력 저장: 리다이렉트 발생 후 admin DB에 이력 행 존재

### 10.2 장애 시나리오

- [ ] Admin 다운 상태에서 redirect 기존 캐시로 정상 서비스
- [ ] Admin 복구 후 redirect 다음 폴링 주기에 정상 동기화 재개
- [ ] Redirect 재기동 시 full load 성공 확인

### 10.3 성능 기준

| 지표 | 기대값 |
|---|---|
| 리다이렉트 P99 응답시간 | < 10ms (캐시 히트 시) |
| 캐시 히트율 | > 99% (정상 운영 시) |
| 증분 동기화 응답시간 | < 500ms (변경분 100건 이내) |

---

## 11. 부록: ShortUrlResponse.deleted 필드 설계 근거

증분 동기화 API(`/api/internal/short-urls/changes?since=`)는 `@SQLRestriction("IS_DEL = 'N'")`을
우회하는 네이티브 쿼리를 사용한다. 따라서 soft-delete된 행도 결과에 포함된다.

redirect 서버의 `ShortUrlCacheSyncer.incrementalSync()`는 각 항목에 대해:

```java
if (Boolean.TRUE.equals(item.getDeleted()) || isExpired(item)) {
    cacheService.evict(item.getShortKey());
} else {
    cacheService.put(item);  // Caffeine per-entry TTL 자동 설정
}
```

`deleted` 필드가 없으면 soft-delete된 URL을 `expiredAt`으로만 판별해야 하는데,
삭제 시 `expiredAt`을 변경하지 않으므로 만료 전 삭제된 URL을 감지할 수 없다.
이것이 `ShortUrlResponse`에 `deleted` 필드를 추가한 이유이다.
