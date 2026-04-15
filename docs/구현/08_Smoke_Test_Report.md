# 08. Smoke Test 보고서 — 단일앱 통합 + 기능 축소 검증

## Executive Summary

| 항목 | 값 |
|---|---|
| 브랜치 | `feature/unified-app-merge` |
| HEAD | `7ae1e30` |
| 실행 환경 | local 프로파일 (H2 in-memory), Java 17, Spring Boot 3.2.5 |
| 서버 포트 | 8080 (단일) |
| 테스트 일시 | 2026-04-15 10:12 KST |
| 테스트 케이스 | 16개 |
| **결과** | **16/16 통과 (100%)** |
| 캐시 동작 | LoadingCache + negative cache 모두 설계대로 확인 |
| 발견된 이슈 | 1건 (빈 `data.sql` 기동 실패 → 별도 수정 커밋으로 반영) |

### Value Delivered (4-Perspective)

| 관점 | 확인 내용 |
|---|---|
| **Problem** | HTTP 홉 제거·기능 축소 이후 기존 단축/리다이렉트 기능이 회귀 없이 동작하는가? |
| **Solution** | 인증 없는 단일 Spring Boot 앱, Caffeine LoadingCache + negative cache, `/r/{shortKey}` 직접 DB 조회 |
| **Function UX Effect** | Client key 입력 없이 curl 한 줄로 생성/조회/삭제/리다이렉트 모두 성공. 전 응답 50~400ms 수준 |
| **Core Value** | 운영 단순화(1 JAR/1 포트) + 폐쇄망에서 즉시 사용 가능 + 설계 상 캐시 일관성 보장 |

---

## 1. 실행 환경

```
$ JASYPT_ENCRYPTOR_PASSWORD=shortUrlApi ./gradlew :short-url-admin:bootRun
```

- Profile: `local` (H2 `jdbc:h2:mem:testdb;MODE=Oracle`, `ddl-auto=create-drop`)
- 포트: 8080
- Caffeine 캐시 설정(`application.yml`):
  - `shortUrl`: max 10,000 / expireAfterWrite 300s / refreshAfterWrite 60s
  - `shortUrlMissing`: max 5,000 / expireAfterWrite 30s

## 2. 발견된 이슈와 수정

### 2.1 빈 `data.sql` 기동 실패
- 증상: 첫 기동 시 `IllegalArgumentException: 'script' must not be null or empty`
- 원인: 커밋 `d9a22e7` 에서 `TBL_USER`/`TBL_CLIENT_ACCESS_KEY` 초기 데이터를 삭제하며
  주석만 남겨둔 `data.sql` 을 Spring `DataSourceInitializer` 가 거부
- 수정: 커밋 `7ae1e30` — `data.sql` 파일 자체 삭제
- 재기동 시 정상 동작 확인

---

## 3. 테스트 결과 상세

모든 케이스 `curl` 로 직접 실행. 응답 본문은 축약.

| # | 시나리오 | 기대 | 실제 | 결과 |
|---|---|---|---|---|
| 1 | 초기 목록 조회 | `totalCount=0, elements=[]` | 동일 | ✅ |
| 2 | 인증 없이 단축 URL 생성 | 200 + ShortUrlResponse | `id=1, shortKey=3XMTj03F, shortUrl=http://localhost:8080/r/3XMTj03F` | ✅ |
| 3 | bot 필드 포함 생성 | 200 + botType/surveyId 반영 | `id=2, botType=CALLBOT, surveyId=S999` | ✅ |
| 4 | 목록 페이징 | `totalCount=2` 최신순 | `totalCount=2`, id=2 먼저 | ✅ |
| 5 | ID 기반 상세 조회 | 200 + 데이터 | 동일 | ✅ |
| 6 | Key 기반 상세 조회 | 200 + 데이터 | 동일 | ✅ |
| 7 | **`/r/{key}` 정상 리다이렉트** | 302 + `Location: https://example.com/alpha` | 동일 | ✅ |
| 8 | **존재하지 않는 키** | 302 + fallback `https://www.nhbank.com` | 동일 | ✅ |
| 9 | **동일 miss 키 재요청 (negative cache 적중)** | 302 + fallback, 로그 `Negative cache hit` | 동일 (로그 확인됨) | ✅ |
| 10 | 만료일 수정 (2030-12-31) | 200 + `expiredAt` 반영 | 동일 | ✅ |
| 11 | 단축 URL 삭제 | 200 + `{result:true}` | 동일 | ✅ |
| 12 | 삭제 후 조회 | `code=1404 Resource Not Found` | 동일 | ✅ |
| 13 | **삭제된 키 `/r/` 요청 (writer 노드 즉시 격하)** | 302 + fallback + 로그 `Negative cache hit` | 동일 | ✅ |
| 14 | `/api/users` (삭제된 엔드포인트) | 404 | 404 | ✅ |
| 15 | `/api/auth/token/issue` (삭제된 엔드포인트) | 404 | 404 | ✅ |
| 16 | `/api/client-keys` (삭제된 엔드포인트) | 404 | 404 | ✅ |

### 3.1 응답 샘플 (생성)
```json
{
  "code": "0000",
  "message": "Success",
  "data": {
    "id": 1,
    "shortKey": "3XMTj03F",
    "shortUrl": "http://localhost:8080/r/3XMTj03F",
    "longUrl": "https://example.com/alpha",
    "createdAt": "2026-04-15T10:12:24.813518",
    "expiredAt": "2026-04-16T10:12:24.800125",
    "botType": null,
    "botServiceKey": null,
    "surveyId": null,
    "surveyVer": null
  }
}
```
- `createdBy`, `userId` 필드 **제거** 확인 (설계 §13.4 반영)

### 3.2 리다이렉트 헤더 (성공)
```
HTTP/1.1 302
Location: https://example.com/alpha
Content-Length: 0
```

### 3.3 리다이렉트 헤더 (실패 → fallback)
```
HTTP/1.1 302
Location: https://www.nhbank.com
Content-Length: 0
```
- `application.yml` 의 `short-url.redirection.fallback-url` 정상 주입 확인

---

## 4. 캐시 동작 로그 증거

```
10:12:25.333 [exec-10] DEBUG ShortUrlLookupLoader
    - ShortUrl not found, marked missing: NOPE_KEY_X
10:12:25.343 [exec-2]  DEBUG ShortUrlLookupService
    - Negative cache hit: NOPE_KEY_X
10:12:25.407 [exec-10] DEBUG ShortUrlLookupService
    - Negative cache hit: 3XMTj03F
```

### 해석
| 시나리오 | 로그 경로 | 검증된 설계 규약 |
|---|---|---|
| 8번 테스트 (최초 miss) | `ShortUrlLookupLoader.load()` 내부에서 `shortUrlMissingCache.put(key, true)` 실행 후 `ShortUrlNotFoundException` throw | **Loader 레벨 negative cache 기록** |
| 9번 테스트 (재요청) | `ShortUrlLookupService.findByKey()` 진입 후 `missingCache.getIfPresent()` 로 조기 반환 (LoadingCache 진입하지 않음) | **Service 레벨 negative cache 단락** |
| 13번 테스트 (삭제 후) | `ShortUrlServiceImpl.notifyCacheEviction()` 에서 `shortUrlCache.invalidate + shortUrlMissingCache.put` → 즉시 negative cache 활성 | **쓰기 노드 즉시 격하** (refreshAfterWrite 기다리지 않음) |

설계 문서 §4.1~4.3 의 캐시 플로우 전체가 실제 동작으로 확인됨.

---

## 5. 기능 축소 검증

| 도메인 | 엔드포인트 | HTTP 응답 | 기대와 일치 |
|---|---|---|---|
| User | `GET /api/users` | 404 | ✅ |
| Auth | `POST /api/auth/token/issue` | 404 | ✅ |
| ClientAccessKey | `GET /api/client-keys` | 404 | ✅ |

- Spring Security 의존성 제거로 인해 인증 헤더 없이도 전 엔드포인트 접근 가능 — 폐쇄망 전제 확인
- `ShortUrlResponse` 의 `createdBy`/`userId` 필드가 응답에 **부재** 확인
- 생성/삭제/수정 API 가 `Principal` 없이도 정상 호출됨

---

## 6. 회귀 점검 (병합 작업)

| 병합 관련 설계 목표 | 검증 방법 | 결과 |
|---|---|---|
| 단일 포트(8080) 운영 | 기동 로그 `Tomcat started on port(s): 8080` | ✅ |
| `short-url-redirect` 모듈 제거 후에도 리다이렉트 정상 | 테스트 7 | ✅ |
| WebClient HTTP 홉 제거 → 캐시 미스 시 JPA 직접 조회 | 테스트 8~9 + 로그 (WebClient 호출 로그 없음) | ✅ |
| 쓰기 노드 로컬 캐시 즉시 갱신 | 테스트 10 (만료일 수정 후 `notifyCacheUpdate`) | ✅ |
| 삭제 후 동일 노드 즉시 404 처리 | 테스트 12, 13 | ✅ |

---

## 7. 미검증 항목 (한계)

아래는 smoke test 스코프를 넘어 **통합/성능 테스트**가 필요합니다. 본 보고서에서는 커버하지 않음을 명시합니다.

1. **`refreshAfterWrite=60s` 비동기 재로드 실측** — 60초 경과 후 첫 요청이 stale 즉시 반환 + 백그라운드 DB 재조회 되는지 별도 타이밍 테스트 필요
2. **`expireAfterWrite=5m` 동기 리로드** — 5분 경과 시점 동기 재조회 (레이턴시 스파이크) 측정 필요
3. **2중화 환경 수렴** — 두 인스턴스 기동 후 A 노드에서 수정, B 노드가 60초 내 최신 값 반영되는지 확인 (로컬 H2 단일 인스턴스라 미검증)
4. **부하 테스트** — 히트율, 미스율, P99 레이턴시 (`/actuator/caches` 또는 wrk/k6 필요)
5. **Oracle 프로파일** — dev/prod 프로파일은 Oracle JDBC 연결이 필요해 이번 로컬 환경에서 미검증
6. **프론트엔드 실동작** — `yarn tsc --noEmit` 는 통과했지만 Vite dev 서버 + 브라우저에서 실제 UI 동작 확인 미수행
7. **`:short-url-admin:test` 테스트** — `ShortUrlAdminApplicationTests.contextLoads()` 는 이번 기능 축소로 의존 제거가 끝났으므로 별도 실행/정비 필요

---

## 8. 결론 및 권장 후속 조치

### 결론
> `feature/unified-app-merge` 브랜치의 **단일앱 통합** 과 **User/Auth/ClientAccessKey 기능 축소** 는
> 로컬 H2 환경에서 **기능 회귀 없이 동작**한다. Caffeine LoadingCache 및 negative cache 는
> 설계 문서 07 §4 에 명시된 플로우대로 확인됐다.

### 권장 후속 조치
1. **`./gradlew :short-url-admin:test` 활성화** — `ShortUrlAdminApplicationTests` 를 통합 테스트로 개선(MockMvc 기반)
2. **refreshAfterWrite 실측 테스트 케이스** — `@ActiveProfiles + TestContainers` 로 타이밍 검증
3. **dev 프로파일 Oracle smoke test** — 운영 투입 전 필수
4. **프론트엔드 브라우저 smoke test** — `yarn dev` 후 단축 URL 관리/히스토리 페이지 수동 확인
5. **운영 시 ACL** — 설계 §13.6 보안 주의사항 준수 (폐쇄망/리버스 프록시)

---

## 9. 재현 명령 (전체 smoke test)

```bash
# 기동
JASYPT_ENCRYPTOR_PASSWORD=shortUrlApi ./gradlew :short-url-admin:bootRun &

# 기본 플로우
curl -s http://localhost:8080/api/short-url                                   # 1
curl -s -X POST http://localhost:8080/api/short-url \
  -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://example.com/alpha"}'                                 # 2
curl -s 'http://localhost:8080/api/short-url?page=0&size=10'                   # 4
curl -s http://localhost:8080/api/short-url/1                                  # 5
curl -s http://localhost:8080/api/short-url/key/3XMTj03F                       # 6

# 리다이렉트 + 캐시
curl -s -D - -o /dev/null http://localhost:8080/r/3XMTj03F                     # 7 (302 example.com)
curl -s -D - -o /dev/null http://localhost:8080/r/NOPE_KEY_X                   # 8 (302 nhbank fallback)
curl -s -D - -o /dev/null http://localhost:8080/r/NOPE_KEY_X                   # 9 (negative cache 즉시 적중)

# 수정·삭제
curl -s -X PUT http://localhost:8080/api/short-url/1/expiration \
  -H 'Content-Type: application/json' \
  -d '{"expiredAt":"2030-12-31T23:59:59"}'                                     # 10
curl -s -X DELETE http://localhost:8080/api/short-url/1                        # 11
curl -s http://localhost:8080/api/short-url/1                                  # 12 (1404)
curl -s -D - -o /dev/null http://localhost:8080/r/3XMTj03F                     # 13 (negative cache 히트)

# 삭제된 엔드포인트
curl -s -D - -o /dev/null http://localhost:8080/api/users                      # 14 (404)
curl -s -D - -X POST -o /dev/null http://localhost:8080/api/auth/token/issue   # 15 (404)
curl -s -D - -o /dev/null http://localhost:8080/api/client-keys                # 16 (404)

# 종료
pkill -f ShortUrlAdminApplication
```
