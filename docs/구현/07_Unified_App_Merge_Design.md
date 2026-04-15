# 07. 단일 애플리케이션 통합 설계 (admin + redirect 병합)

## 1. 목적
`short-url-admin`과 `short-url-redirect`를 하나의 Spring Boot 애플리케이션으로 통합하고,
Redis 없이 Caffeine 로컬 캐시만으로 2중화 환경의 URL 조회 성능과 일관성을 모두 확보한다.

- HTTP 홉 제거: redirect → admin 내부 API 호출(WebClient) 경로 전면 삭제, JPA Repository 직접 호출로 전환
- 캐시 일관성: `expireAfterWrite` + `refreshAfterWrite` 조합으로 노드 간 stale 편차를 상한선으로 제한
- 장애 저감: 존재하지 않는 키(404) 폭주 방지를 위한 negative cache 도입
- 배포 단순화: 하나의 JAR, 하나의 컨테이너, 하나의 `application.yml`
- 기능 축소: **폐쇄망 내부 도구** 전제로 User/Auth/ClientAccessKey 관리 기능을 본 브랜치에서 전부 제거 (main 브랜치에는 유지)

## 2. 현재 구조와 문제점

### 2.1 AS-IS
| 모듈 | 역할 | 데이터 소스 |
|---|---|---|
| `short-url-admin` | 사용자/단축 URL CRUD, 통계 | JPA → Oracle/H2 |
| `short-url-redirect` | `/r/{key}` 리다이렉트 | Caffeine → (miss 시 WebClient로 admin 조회) |

- `short-url-redirect/src/main/java/com/nh/shorturl/redirect/service/ShortUrlCacheWarmer.java:27` — 기동 시 `GET /api/internal/short-urls/all`로 전체 로드
- `short-url-redirect/src/main/java/com/nh/shorturl/redirect/service/ShortUrlServiceImpl.java:18` — 로컬 캐시에 없으면 `null` 반환 (fallback 없음)
- `short-url-admin/src/main/java/com/nh/shorturl/admin/service/shorturl/ShortUrlServiceImpl.java:248` — CRUD 직후 `redirectApiClient.put(...)`로 HTTP 캐시 알림
- `short-url-redirect/src/main/java/com/nh/shorturl/redirect/service/RedirectionConfigStore.java:26` — 60초 주기로 `/api/internal/redirection-config` 폴링

### 2.2 문제점
1. **HTTP 홉 오버헤드**: 캐시 미스·CRUD 알림마다 admin ↔ redirect 왕복 필요
2. **기동 의존성**: redirect 기동 시 admin이 반드시 먼저 살아 있어야 함 (ShortUrlCacheWarmer 실패 시 서비스 불가)
3. **TTL 없음**: `CaffeineCacheManager`에 `expireAfterWrite` 미설정 → 삭제/만료된 URL이 노드마다 영구 stale 가능
4. **404 폭주 위험**: 존재하지 않는 키가 매 요청마다 캐시 미스 처리 → `null` 반환 (negative cache 없음)
5. **이원화된 배포**: JAR 2개, application.yml 2개, 운영 복잡도 증가

## 3. TO-BE 아키텍처

### 3.1 모듈 구성
`short-url-redirect` 모듈을 제거하고 `short-url-admin`의 서브 패키지로 편입한다.

```
short-url-admin/
└── src/main/java/com/nh/shorturl/admin/
    ├── controller/        # 기존 관리 API
    ├── redirect/          # (신규) redirect 전용 레이어
    │   ├── controller/    #   ShortUrlRedirectController  (/r/{key})
    │   ├── service/       #   ShortUrlLookupService       (캐시 + DB)
    │   │                  #   RedirectionConfigStore      (@Value 기반)
    │   │                  #   RedirectionHistoryAsyncWriter
    │   └── config/        #   RedirectCacheConfig         (Caffeine 스펙)
    ├── service/shorturl/  # 관리 CRUD (WebClient 알림 제거)
    └── config/SecurityConfig.java  # /r/** permitAll 유지
```

- `settings.gradle`에서 `short-url-redirect` 모듈 제거
- `common` 모듈의 DTO는 변경 없음 (`ShortUrlResponse`, `RedirectionConfigResponse` 재사용)
- `short-url-admin/build.gradle`에 `caffeine`, `spring-boot-starter-cache` 추가

### 3.2 런타임 배치 (2중화)
```
          ┌──────────────┐          ┌──────────────┐
  LB ───► │  Node A      │          │  Node B      │
          │  (admin+rdr) │          │  (admin+rdr) │
          │  Caffeine    │          │  Caffeine    │
          └──────┬───────┘          └──────┬───────┘
                 │                         │
                 └──────────┬──────────────┘
                            ▼
                        Oracle DB
```
- 각 노드는 독립적인 로컬 Caffeine 캐시를 보유 (공유 캐시 없음)
- 쓰기(관리 API)는 어느 노드로 가든 DB 즉시 반영 + 해당 노드 로컬 캐시 즉시 무효화
- 다른 노드는 `refreshAfterWrite` 주기 안에 DB에서 최신 값을 **비동기 재로드**하여 수렴

## 4. 캐시 전략 (옵션 1 + 3 조합)

### 4.1 Positive cache: `shortUrl`
| 항목 | 값 | 근거 |
|---|---|---|
| `maximumSize` | `10_000` | 기존 값 유지 |
| `expireAfterWrite` | `5m` (300초) | stale 상한선 |
| `refreshAfterWrite` | `60s` | stale 편차 상한선, 실제 리로드는 비동기 |
| `recordStats` | `true` | `/actuator/caches` 모니터링 |
| `CacheLoader` | `ShortUrlRepository.findByShortUrl` | 비동기 DB 재조회 |

**의미**:
- 조회 후 60초가 지나면 다음 요청은 **기존 값 즉시 반환** + 백그라운드에서 DB 재조회
- 5분이 지나면 동기 리로드 (짧은 지연 허용)
- 어떤 노드에서든 수정/삭제 후 최대 60초 이내에 타 노드가 수렴

### 4.2 Negative cache: `shortUrlMissing`
| 항목 | 값 |
|---|---|
| `maximumSize` | `5_000` |
| `expireAfterWrite` | `30s` |

- DB에서 "없음"을 확인한 키를 30초간 기억하여 반복 404를 차단
- 신규 URL이 생성되면 해당 키를 즉시 negative cache에서 제거 (쓰기 노드에 한해)

### 4.3 쓰기 시 캐시 처리
```java
// 생성/수정 후
cacheManager.getCache("shortUrl").put(key, response);
cacheManager.getCache("shortUrlMissing").evict(key);

// 삭제 후
cacheManager.getCache("shortUrl").evict(key);
cacheManager.getCache("shortUrlMissing").put(key, Boolean.TRUE);
```
- 쓰기 노드는 즉시 일관성 확보
- 비쓰기 노드는 `refreshAfterWrite=60s`로 수렴 (옵션 1의 효과)

### 4.4 조회 플로우
```
GET /r/{key}
  │
  ├─ shortUrlMissing.get(key) 존재? → 404 즉시 반환
  │
  ├─ shortUrl.get(key, CacheLoader) 실행
  │     CacheLoader:
  │       Optional<ShortUrl> = shortUrlRepository.findByShortUrl(key)
  │       ├─ 존재 & !expired → toResponse() 반환
  │       ├─ 존재 & expired  → shortUrlMissing.put(key) 후 null 반환
  │       └─ 없음            → shortUrlMissing.put(key) 후 null 반환
  │
  ├─ null이면 fallback/error 페이지
  └─ 있으면 히스토리 비동기 기록 + 302 redirect
```

## 5. 컴포넌트 상세

### 5.1 `RedirectCacheConfig` (신규)
```java
@Configuration
@EnableCaching
public class RedirectCacheConfig {

    public static final String SHORT_URL_CACHE = "shortUrl";
    public static final String SHORT_URL_MISSING_CACHE = "shortUrlMissing";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager();
        mgr.registerCustomCache(SHORT_URL_CACHE,
            Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .refreshAfterWrite(Duration.ofSeconds(60))
                .recordStats()
                .build(cacheLoader()));
        mgr.registerCustomCache(SHORT_URL_MISSING_CACHE,
            Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .recordStats()
                .build());
        return mgr;
    }
    // CacheLoader는 ShortUrlLookupService에서 주입
}
```

### 5.2 `ShortUrlLookupService` (신규, redirect 전용 조회)
- `ShortUrlRepository`를 직접 주입받아 사용 (동일 JVM, 동일 트랜잭션 컨텍스트 아님)
- `@Transactional(readOnly = true)` 로 트랜잭션 읽기 모드 사용
- DB 조회 후 `isExpired()` 체크하여 만료면 negative cache로 격하
- 기존 `short-url-redirect/.../ShortUrlServiceImpl.java` 대체

### 5.3 `ShortUrlRedirectController` 이관
- 경로: `/r/{shortKey}` (기존 admin `SecurityConfig`가 `/r/**`를 이미 permitAll)
- 기존 redirect 모듈의 루트 `/{shortUrl}` 매핑은 제거 (admin UI 루트 경로와 충돌 회피)
- `rootRedirect()`의 `/` 매핑은 삭제 (admin UI가 `/`를 소유)
- `appendTrackingFields`, `handleFailure` 로직은 그대로 이식

### 5.4 `RedirectionConfigStore` 단순화
- 기존 60초 폴링 제거
- `@Value("${short-url.redirection.*}")`로 직접 주입받는 `@Component` 빈으로 전환
- 동일 JVM 내에서 admin이 소유한 설정값을 바로 읽을 수 있으므로 폴링 불필요

### 5.5 `RedirectionHistoryAsyncWriter` (신규)
- 기존 `redirect/service/RedirectionHistoryServiceImpl` 대체
- `@Async`로 비동기 호출, admin `RedirectionHistoryService.saveRedirectionHistory(RedirectionHistoryRequest)` 직접 호출
- HttpServletRequest → `RedirectionHistoryRequest` 변환 로직 이관
- `@EnableAsync`는 admin에 이미 없으면 `RedirectCacheConfig`에 추가

### 5.6 `ShortUrlServiceImpl` 수정 (admin 쓰기 경로)
- `redirectApiClient` (WebClient) 의존성 제거
- `notifyCacheUpdate`, `notifyCacheEviction` 삭제
- 대신 `CacheManager`를 주입받아 동일 JVM 내에서 즉시 put/evict
- `WebClient` 빈, `WebClientConfig`의 `redirectApiClient` 설정 제거

### 5.7 삭제 대상
| 경로 | 사유 |
|---|---|
| `short-url-redirect/` 전체 | admin으로 흡수 |
| `short-url-admin/.../ShortUrlInternalApiController.java` | 더 이상 필요 없음 (redirect 모듈이 사라짐) |
| `short-url-admin/.../WebClientConfig.java`의 `redirectApiClient` 빈 | HTTP 알림 경로 제거 |
| `ShortUrlServiceImpl`의 `notifyCacheUpdate/Eviction` | 로컬 CacheManager로 대체 |

## 6. Security 구성

### 6.1 현재 상태
- `short-url-admin/.../SecurityConfig.java:45`에 `/r/**`가 이미 `permitAll`
- `JwtAuthenticationFilter`는 `shouldNotFilter()`에서 POST `/api/short-url`만 우회 — **`/r/**` 경로에도 JWT 필터가 걸린다** (현재 토큰 없어도 통과하긴 하지만 불필요한 필터 실행)

### 6.2 변경
- `JwtAuthenticationFilter.shouldNotFilter()`에 `/r/` prefix 요청 스킵 추가
- `ClientAccessKeyValidationFilter`는 `/api/short-url`만 대상이므로 영향 없음
- `/api/internal/**` permitAll 규칙은 삭제 (내부 API 컨트롤러가 없어지므로)

## 7. 트랜잭션·동시성 고려

- `ShortUrlLookupService`는 `@Transactional(readOnly = true)` — open-in-view 경계 안에서 lazy 필드 접근은 이미 `toResponse()`로 Eager 매핑 완료
- CacheLoader 내부에서 DB 조회 시 Hibernate 세션 필요 → Spring Cache의 `@Cacheable` 대신 **`CacheLoader` 기반**으로 선택한 이유:
  - `refreshAfterWrite` 활성화 조건이 `CacheLoader` 존재이기 때문
  - Spring의 `@Cacheable`만으로는 refreshAfterWrite가 동작하지 않음
- 비동기 재로드용 `Executor`는 Caffeine 기본값(ForkJoinPool.commonPool) 사용, 필요 시 전용 풀로 교체 가능

## 8. application.yml 변경

```yaml
# short-url-admin/src/main/resources/application.yml
short-url:
  redirect:
    public-url: http://localhost:8080/r/   # 포트 통합: 8080 단일
  redirection:
    fallback-url: "https://www.nhbank.com"
    default-host: "https://www.nhbank.com"
    show-error-page: true
    tracking-fields: "utm_source,utm_medium,utm_campaign"
  cache:
    short-url:
      maximum-size: 10000
      expire-after-write-seconds: 300
      refresh-after-write-seconds: 60
    missing:
      maximum-size: 5000
      expire-after-write-seconds: 30
```
- 캐시 파라미터를 외부 설정으로 노출하여 운영 중 튜닝 가능

## 9. 빌드·배포 영향

| 항목 | Before | After |
|---|---|---|
| 모듈 | common, admin, redirect, admin-ui (4개) | common, admin, admin-ui (3개) |
| JAR | 2개 | 1개 |
| 포트 | 8080(admin) + 8081(redirect) | 8080 단일 |
| 의존성 | redirect에 caffeine | admin에 caffeine 추가 |
| 기동 순서 | admin → redirect | 단일 |

## 10. 리스크와 완화책

| 리스크 | 영향 | 완화 |
|---|---|---|
| 리다이렉트 트래픽이 admin JPA 세션풀 잠식 | 관리 API 응답 저하 | 캐시 히트율 모니터링(`/actuator/caches`), 필요 시 HikariCP `maximum-pool-size` 증설 |
| `refreshAfterWrite=60s` 동안 타 노드 stale | 삭제된 URL이 최대 60초간 동작 | SLA 허용 범위 확인, 필요 시 30초로 축소 |
| Negative cache로 신규 URL이 30초간 404 | 즉시 쓰기 노드에서는 eviction으로 해결, 타 노드에서는 30초 지연 | 네거티브 TTL을 쓰기 노드 eviction으로 보완 |
| 포트 통합 시 외부 단축 URL 도메인 변경 | 기존 발급 URL 호환성 | `public-url` 이행 기간 동안 리버스 프록시로 `/r/**` 라우팅 유지 |
| Spring Cache `@Cacheable`에서 refreshAfterWrite 미동작 | 의도한 리프레시 실패 | `CacheLoader` 기반 `LoadingCache` 직접 사용 |

## 11. 마이그레이션 순서 (구현 단계)

1. `short-url-admin/build.gradle`에 `spring-boot-starter-cache`, `caffeine` 추가
2. `com.nh.shorturl.admin.redirect` 패키지 신설 (`config`, `controller`, `service`)
3. `RedirectCacheConfig`, `ShortUrlLookupService`(CacheLoader 포함) 작성
4. `ShortUrlRedirectController`, `RedirectionConfigStore`, `RedirectionHistoryAsyncWriter` 이관 및 내부 호출로 교체
5. `admin.ShortUrlServiceImpl`에서 WebClient 알림 제거, `CacheManager` 직접 사용
6. `ShortUrlInternalApiController`, `WebClientConfig#redirectApiClient` 삭제
7. `JwtAuthenticationFilter.shouldNotFilter()`에 `/r/` 스킵 추가, `/api/internal/**` 규칙 제거
8. `application.yml` 통합 (포트, public-url, cache 설정)
9. `settings.gradle`에서 `short-url-redirect` 제거, `short-url-redirect/` 디렉터리 삭제
10. `./gradlew :short-url-admin:build` 및 수동 smoke test

## 12. 검증 포인트
- `GET /r/{key}` 정상/만료/미존재 각각 동작
- 생성 직후 같은 노드에서 1ms 이내 캐시 히트
- 삭제 후 같은 노드에서 negative cache 적중 확인
- `refreshAfterWrite` 동작: 60초 경과 후 첫 요청은 즉시 응답, 로그에서 비동기 DB 재조회 확인
- `/actuator/caches` 또는 로그로 히트율/미스율/로드시간 확인
- 관리 API 응답 시간 회귀 없음

## 13. 기능 축소 — User/Auth/ClientAccessKey 제거

### 13.1 배경
이 브랜치는 **폐쇄망/내부 도구** 전제로 운영한다. 인증/소유자 개념이 필요 없으므로,
본 설계 적용과 함께 User·Auth(JWT)·ClientAccessKey 관련 기능을 완전히 제거한다.
원본은 `main` 브랜치와 `pre-unified-merge` 태그에 그대로 보존되어 있다.

### 13.2 제거된 엔드포인트
| 이전 | 상태 |
|---|---|
| `POST /api/users` | 삭제 |
| `GET /api/users/**` | 삭제 |
| `POST /api/auth/token/issue` | 삭제 |
| `POST /api/auth/token/re-issue` | 삭제 |
| `GET/POST/PUT/DELETE /api/client-keys/**` | 삭제 |

### 13.3 제거된 구성요소
- Spring Security 필터 체인, `SecurityConfig`, `JwtAuthenticationFilter`, `ClientAccessKeyValidationFilter`
- `spring-boot-starter-security`, `jjwt-api/impl/jackson` 의존성
- `application*.yml` 의 `jwt.secret`, `jwt.expiration` 설정
- `User`/`ClientAccessKey` 엔티티와 JPA Repository, Service, Controller, DTO
- `TokenService`, `JwtProvider`, `CustomUserDetailsService`
- `data.sql` 의 `TBL_USER`, `TBL_CLIENT_ACCESS_KEY` 초기 데이터
- 프론트엔드 `UserManagementPage`, `ClientAccessKeyPage`, `AuthControlsPage` 및 사이드바/라우트 참조

### 13.4 ShortUrl 엔티티 변경
기존 `ShortUrl.user` (NOT NULL FK) 및 `ShortUrl.clientAccessKey` (nullable FK) 필드를 제거하여
단축 URL은 순수한 key→URL 매핑이 된다. `ShortUrlResponse` 의 `createdBy`, `userId` 필드도 삭제.
Oracle DDL 은 향후 정리 필요 (JPA ddl-auto=validate 프로파일에서 운영 반영 시 스키마 정합성 확인).

### 13.5 ShortUrlController 변경
- `Principal`, `HttpServletRequest`, `ClientAccessKey` 파라미터 전부 제거
- `create/delete/list/updateExpiration` 모두 인증 없이 호출 가능
- 누구나 전체 CRUD 가능하므로 **반드시 폐쇄망에서만** 운영해야 함

### 13.6 보안 주의
- Spring Security 의존성 자체가 빠졌으므로 CSRF·CORS 기본값이 모두 적용되지 않는다.
- 본 브랜치를 외부 노출이 가능한 환경에 배포하면 **누구나 URL CRUD 가능**. 반드시 내부망/
  리버스 프록시 레벨 ACL 로 접근을 통제할 것.
- 필요 시 `main` 브랜치의 인증 체계를 cherry-pick 하여 복원 가능.
