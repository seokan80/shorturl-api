# 리팩토링 계획: 2026-04 사이클 (v2 — 서버 분리 후)

## Executive Summary

| 항목 | 내용 |
|---|---|
| Feature | `refactoring-2026-04` |
| 작성일 | 2026-04-16 (v2 갱신) |
| 목표 기간 | 2026-04-16 ~ 2026-04-30 (2주) |
| 브랜치 | `feature/unified-app-merge` |
| 선행 설계 | [09_Server_Separation_Design.md](../구현/09_Server_Separation_Design.md) |

### Value Delivered

| 관점 | 내용 |
|---|---|
| Problem | XSS 취약점, 캐시 기동 실패 복원력 부재, 문서-코드 불일치, 테스트 부재, 설정 하드코딩 |
| Solution | 보안 방어 + 기동 복원력 + 문서 정합화 + 핵심 경로 테스트 + 설정 외부화 |
| Function UX | 에러 응답 포맷 일관성, 안전한 에러 페이지, 운영 시 폴링 주기/만료 정책 재빌드 없이 조정 |
| Core Value | 보안 강화, 장애 복원력, 유지보수성 향상, 신규 개발자 온보딩 비용 감소 |

---

## 0. 현재 상태 (v1 대비 변경점)

### 완료된 항목 (v1에서 계획, 이미 반영)

| v1 항목 | 상태 | 반영 커밋/위치 |
|---|---|---|
| P0-1. GlobalExceptionHandler 도입 | **완료** | `admin/exception/GlobalExceptionHandler.java` 존재. 컨트롤러 try-catch 제거됨 |
| P0-3. 문서 정합화 일부 | **부분 완료** | CLAUDE.md 업데이트됨. 단 01/02 설계 문서는 미갱신 |
| P1-1. 설정 외부화 (default-expiration-days) | **완료** | `application.yml`에 `short-url.default-expiration-days: 1`, `@Value` 주입 |
| 서버 분리 (admin=JAR, redirect=WAR) | **완료** | 4-모듈 구조 (09 설계서 참조) |
| 캐시 아키텍처 재설계 | **완료** | per-entry Caffeine + 5분 증분 폴링 |

### 아키텍처 변경으로 v1 항목 재검토

| v1 항목 | v2 상태 | 사유 |
|---|---|---|
| P0-2. XSS 방어 | **대상 변경** → redirect 모듈의 `ShortUrlRedirectController.buildErrorHtml()` | admin의 redirect 패키지가 아닌 별도 모듈로 이동 |
| P1-2. FE 공통 훅 추출 | **유지** | 아키텍처 변경과 무관 |
| P1-3. 백엔드 테스트 | **범위 확대** | redirect 모듈 테스트도 추가 필요 |
| P2-1. 히스토리 denormalize 검토 | **유지** | admin 모듈에서 변경 없음 |
| P2-2. UserAgent 파싱 | **유지** | admin 모듈에서 변경 없음 |

---

## 1. 설계 적절성 평가

### 1.1 아키텍처 적절성 점수

| 평가 항목 | 점수 (5점 만점) | 비고 |
|---|---|---|
| 장애 격리 | ★★★★★ | admin 다운 → redirect 캐시로 독립 운영 |
| 운영 호환성 | ★★★★★ | 기존 Tomcat 인프라에 WAR 배포 가능 |
| 캐시 정확도 | ★★★★☆ | per-entry TTL 정밀. 증분 5분 공백은 SLA 허용 범위 확인 필요 |
| 기동 복원력 | ★★☆☆☆ | **fullLoad 실패 시 재시도 없음, 빈 캐시로 시작** |
| 보안 | ★★☆☆☆ | **XSS 취약점 존재**, 내부 API 인증 없음 |
| 코드 품질 | ★★★☆☆ | GlobalExceptionHandler 완료, 테스트 부재 |
| 문서 정합성 | ★★☆☆☆ | 01/02 문서가 이전 아키텍처 기술 |
| 설정 유연성 | ★★★☆☆ | 만료일 외부화 완료. 폴링 주기/캐시 크기 하드코딩 |

**종합**: 아키텍처 골격은 적절하나, **보안·복원력·테스트** 3개 영역에 즉시 개선 필요.

### 1.2 발견된 주요 문제

#### [Critical] C-1. XSS 취약점 — redirect 에러 페이지

**위치**: `short-url-redirect/.../ShortUrlRedirectController.java:112`

```java
"<p><small>" + reason + "</small></p>"
```

`reason`이 `e.getMessage()`에서 오므로, 공격자가 shortKey에 스크립트를 삽입하면 실행됨.
폐쇄망이라도 내부 사용자 브라우저를 경유한 XSS는 가능하므로 반드시 수정 필요.

#### [Critical] C-2. fullLoad 실패 시 복원력 부재

**위치**: `short-url-redirect/.../ShortUrlCacheSyncer.java:94-111`

admin이 기동 전이거나 네트워크 장애 시 `fullLoad()` 한 번 실패하면 빈 캐시로 시작.
다음 `incrementalSync()` (5분 후)에서 `since==null` 체크로 재시도하지만, 그 사이 모든 요청이 fallback.

#### [High] H-1. 증분 폴링 주기 하드코딩

**위치**: `ShortUrlCacheSyncer.java:52` — `fixedRate = 300_000`
**위치**: `RedirectionConfigStore.java` — `fixedRate = 60_000`

운영 환경에서 폴링 주기를 변경하려면 재빌드 필요. `application.yml`로 외부화해야 함.

#### [High] H-2. 내부 API 무인증

`/api/internal/**` 에 인증/IP 화이트리스트 없음. 폐쇄망이라도 내부 사용자가 직접 호출 가능.
최소한 IP 기반 필터나 공유 시크릿 헤더 검증 권장.

#### [Medium] M-1. 테스트 부재

- admin: `GlobalExceptionHandler` 동작 검증 테스트 없음
- redirect: `ShortUrlCacheSyncer`, `ShortUrlRedirectController` 테스트 없음
- 공통: `Base62`, `ShortUrlServiceImpl` 단위 테스트 없음

#### [Medium] M-2. 문서-코드 불일치

- `01_Domain_Design_Backend.md`: User/ClientAccessKey ERD, 2-모듈 아키텍처 기술
- `02_API_Design_Spec.md`: Auth/User/ClientKey API 목록 존재, 증분 동기화 API 누락
- `07_Unified_App_Merge_Design.md`: 단일앱 통합 기술 (현재는 분리됨)

---

## 2. 우선순위별 작업 목록

### 🔴 P0 — 금주 완료 (보안 + 복원력)

#### P0-1. XSS 방어: buildErrorHtml 이스케이프 적용

- **대상**: `short-url-redirect/src/main/java/com/nh/shorturl/redirect/controller/ShortUrlRedirectController.java:103-113`
- **수정**:
  ```java
  // Before
  "<p><small>" + reason + "</small></p>"

  // After — 의존성 없이 직접 이스케이프
  private String escapeHtml(String input) {
      if (input == null) return "";
      return input.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
  }
  ```
- **검증**: `reason = "<script>alert(1)</script>"` → `&lt;script&gt;` 로 렌더링 확인
- **소요**: 30분

#### P0-2. 에러 페이지 템플릿 분리

- **대상**: 동일 파일의 `buildErrorHtml()` 메서드
- **방법**: `src/main/resources/templates/redirect-error.html` 정적 파일로 추출.
  `ClassPathResource`로 읽어 `String.format(template, escapedReason)` 치환.
  Thymeleaf 의존성 추가 불필요.
- **소요**: 1시간

#### P0-3. CacheSyncer fullLoad 재시도 (exponential backoff)

- **대상**: `short-url-redirect/.../ShortUrlCacheSyncer.java:94-111`
- **수정**:
  ```java
  private void fullLoad() {
      int maxRetries = 3;
      long waitMs = 5_000;
      for (int attempt = 1; attempt <= maxRetries; attempt++) {
          try {
              List<ShortUrlResponse> all = webClient.get()
                  .uri("/api/internal/short-urls/all")
                  .retrieve().bodyToFlux(ShortUrlResponse.class)
                  .collectList().block();
              if (all != null) {
                  all.forEach(cacheService::put);
                  lastSyncTime.set(LocalDateTime.now());
                  log.info("[cache-sync] Full load completed. {} items.", all.size());
                  return;
              }
          } catch (Exception e) {
              log.warn("[cache-sync] Full load attempt {}/{} failed: {}",
                       attempt, maxRetries, e.getMessage());
              if (attempt < maxRetries) {
                  try { Thread.sleep(waitMs); } catch (InterruptedException ie) {
                      Thread.currentThread().interrupt(); return;
                  }
                  waitMs *= 2;
              }
          }
      }
      log.error("[cache-sync] Full load failed after {} retries. Cache is empty!", maxRetries);
  }
  ```
- **소요**: 30분

#### P0-4. appendTrackingFields URL 인코딩

- **대상**: `ShortUrlRedirectController.java:72-87`
- **현재 문제**: tracking field 값을 URL 인코딩 없이 그대로 append
  ```java
  sb.append(field.trim()).append("=").append(value);  // value 미인코딩
  ```
- **수정**: `URLEncoder.encode(value, StandardCharsets.UTF_8)` 적용
- **소요**: 15분

---

### 🟠 P1 — 2주 이내 (코드 품질 + 테스트)

#### P1-1. 폴링 주기 설정 외부화

- **대상**:
  - `ShortUrlCacheSyncer.java` — `fixedRate = 300_000`
  - `RedirectionConfigStore.java` — `fixedRate = 60_000`
- **수정**: `@Scheduled(fixedRateString = "${short-url.sync.interval-ms:300000}")`
- **application.yml 추가**:
  ```yaml
  short-url:
    sync:
      interval-ms: 300000        # 캐시 증분 동기화 주기
    config:
      poll-interval-ms: 60000    # 설정 폴링 주기
  ```
- **소요**: 1시간

#### P1-2. 캐시 크기 설정 외부화

- **대상**: `AppConfig.java` — `maximumSize(10_000)`, null TTL `24시간`
- **수정**: `@Value`로 주입
  ```yaml
  short-url:
    cache:
      maximum-size: 10000
      default-ttl-hours: 24
  ```
- **소요**: 30분

#### P1-3. 백엔드 테스트 작성 (admin)

| 테스트 클래스 | 대상 | 유형 | 검증 포인트 |
|---|---|---|---|
| `Base62Test` | `admin/util/Base62.java` | 단위 | 인코딩 길이, 문자셋, 재현성 |
| `ShortUrlServiceImplTest` | `admin/service/shorturl/` | Mockito | 생성·삭제·만료·findChangedSince |
| `ShortUrlControllerTest` | `admin/controller/` | `@WebMvcTest` | CRUD + 404/400 에러 응답 |
| `InternalApiControllerTest` | `admin/controller/` | `@WebMvcTest` | 전체/증분/설정/이력 API |
| `ShortUrlRepositoryTest` | `admin/repository/` | `@DataJpaTest` | findChangedSince 네이티브 쿼리 |
| `GlobalExceptionHandlerTest` | `admin/exception/` | MockMvc | 예외 → ResultEntity 매핑 |

- **목표**: admin 핵심 서비스 커버리지 60%
- **소요**: 3~4일

#### P1-4. 백엔드 테스트 작성 (redirect)

| 테스트 클래스 | 대상 | 유형 | 검증 포인트 |
|---|---|---|---|
| `ShortUrlCacheSyncerTest` | `redirect/service/` | Mockito + MockWebServer | fullLoad, incrementalSync, retry |
| `ShortUrlRedirectControllerTest` | `redirect/controller/` | MockMvc | 정상 302, 미존재→fallback, XSS 방어 |
| `ShortUrlCacheServiceTest` | `redirect/service/` | 단위 | put/get/evict, per-entry TTL |

- **소요**: 2일

#### P1-5. 프론트엔드 공통 훅 추출

- **대상**: `ShortUrlManagementPage.tsx`, `RedirectionHistoryPage.tsx`
- **추출**:
  - `src/lib/apiClient.ts` → `useApiRequest<T>()` 훅
  - `src/lib/formats.ts` → `formatDateTime`, `formatDuration`
  - 페이지별 커스텀 훅 (`useShortUrlList`, `useShortUrlForm`)
- **소요**: 2~3일

---

### 🟡 P2 — 여력 시

#### P2-1. 내부 API 최소 인증

- `/api/internal/**`에 공유 시크릿 헤더 검증 (`X-Internal-Key`)
- admin `application.yml`에 시크릿 설정, redirect에도 동일 값 설정
- `OncePerRequestFilter` 또는 `HandlerInterceptor`로 구현
- **소요**: 2시간

#### P2-2. 문서 정합화

| 문서 | 변경 내용 |
|---|---|
| `01_Domain_Design_Backend.md` | User/ClientAccessKey 제거, 현재 ERD, 4-모듈 구조, 서비스 간 통신 패턴 |
| `02_API_Design_Spec.md` | Auth/User/ClientKey API 제거, 증분 동기화 API 추가, 에러 코드 갱신 |
| `07_Unified_App_Merge_Design.md` | 상단에 "⚠️ 본 문서는 역사 기록용. 현재 아키텍처는 09 참조" 안내 추가 |

#### P2-3. UserAgent 파싱 라이브러리 대체

- `UserAgentParser.java` 수동 `contains()` → `yauaa` (nl.basjes:yauaa) 검토
- POC 후 벤치마크

#### P2-4. negative cache 도입 검토

현재 redirect 서버에 negative cache가 없음. 존재하지 않는 키 반복 요청 시 매번 fallback 처리.
per-entry 캐시 특성상 miss key는 Caffeine에 들어가지 않으므로, 별도 negative cache 또는
miss 키도 짧은 TTL로 put하는 방식 검토.

---

## 3. 작업 순서

```
Week 1 (4/16~4/20)
├── Day 1: P0-1 XSS 방어 + P0-4 URL 인코딩 (30분+15분)
├── Day 1: P0-2 에러 페이지 템플릿 분리 (1시간)
├── Day 2: P0-3 CacheSyncer fullLoad 재시도 (30분)
├── Day 2: P1-1 폴링 주기 외부화 + P1-2 캐시 크기 외부화 (1.5시간)
├── Day 3-4: P1-3 admin 테스트 작성
└── Day 5: P1-4 redirect 테스트 작성

Week 2 (4/21~4/25)
├── Day 6-8: P1-5 프론트엔드 공통화
├── Day 9: P2-1 내부 API 최소 인증 (여력 시)
└── Day 10: P2-2 문서 정합화 + 최종 빌드 검증
```

---

## 4. 리스크 & 롤백

| 리스크 | 완화책 |
|---|---|
| XSS 이스케이프로 기존 에러 메시지 깨짐 | `escapeHtml()`은 안전 변환만 하므로 정상 텍스트에 영향 없음 |
| fullLoad 재시도 중 기동 시간 증가 (최대 35초) | `initialDelay` 경과 전까지는 incrementalSync 미동작. 서비스 시작은 즉시 |
| 폴링 주기 외부화로 기존 하드코딩 값 누락 | `${...:300000}` 기본값 명시로 설정 없어도 동작 |
| 테스트 작성 중 기존 동작 변경 발견 | 테스트는 현재 동작을 검증하는 골든 테스트로 먼저 작성 |

---

## 5. 완료 기준 (Definition of Done)

- [ ] `./gradlew clean build` 성공, 신규 테스트 초록
- [ ] `./gradlew :short-url-admin:jacocoTestReport` 핵심 서비스 커버리지 ≥ 60%
- [ ] `yarn test` (admin-ui) 초록
- [ ] XSS 페이로드 `<script>alert(1)</script>` 주입 시 스크립트 미실행
- [ ] CacheSyncer fullLoad 3회 재시도 후 성공 시나리오 테스트 통과
- [ ] 폴링 주기가 `application.yml`에서 변경 가능
- [ ] 09 설계 문서가 현재 구현과 일치
