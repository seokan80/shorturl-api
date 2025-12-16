# 모듈 기능 개요 및 관계도

프로젝트 모듈을 빠르게 파악할 수 있도록 각 모듈의 주요 책임과 내부 기능을 정리하고, 상호 연동 흐름을 머메이드 다이어그램으로 시각화했습니다.

## 모듈별 주요 기능

### common
- 공통 응답 코드 `ApiResult`를 정의해 모든 백엔드 모듈이 동일한 상태코드/메시지 체계를 공유합니다. 【F:common/src/main/java/com/nh/shorturl/type/ApiResult.java†L1-L120】
- 요청/응답 DTO와 검증 어노테이션 의존성을 제공해 나머지 모듈에서 재사용합니다. 【F:common/build.gradle†L1-L13】

### short-url-admin
- 단축 URL 생성·조회·삭제·만료일 변경 API를 제공하고, JWT 사용자와 API 키 기반 클라이언트를 모두 처리합니다. 【F:short-url-admin/src/main/java/com/nh/shorturl/admin/controller/ShortUrlController.java†L23-L166】
- 액세스 토큰/리프레시 토큰 발급 및 재발급 엔드포인트를 제공하며, 클라이언트 접근 키 검증을 함께 수행합니다. 【F:short-url-admin/src/main/java/com/nh/shorturl/admin/controller/AuthController.java†L13-L62】
- 리디렉션 기록 저장과 단축 URL 캐시 초기화용 내부 API를 노출해 redirect 모듈과 연동합니다. 【F:short-url-admin/src/main/java/com/nh/shorturl/admin/controller/ShortUrlInternalApiController.java†L13-L45】
- 공통 DTO 모듈(`common`)에 의존하고, Spring Web/JPA/Validation, Security(JWT), WebFlux, Jasypt, DB 드라이버를 포함합니다. 【F:short-url-admin/build.gradle†L1-L31】

### short-url-redirect
- `/r/{shortUrl}`로 들어온 요청을 원본 URL로 리디렉션하고, 비동기로 리디렉션 통계를 admin 모듈에 전달합니다. 【F:short-url-redirect/src/main/java/com/nh/shorturl/redirect/controller/ShortUrlRedirectController.java†L17-L47】
- 캐시된 단축 URL을 갱신/제거하는 내부 API를 제공하여 admin 모듈이 캐시를 관리할 수 있게 합니다. 【F:short-url-redirect/src/main/java/com/nh/shorturl/redirect/controller/CacheManagementController.java†L10-L30】
- 기동 시 admin 모듈의 내부 API에서 모든 단축 URL을 가져와 캐시를 예열하고, WebClient 기본 baseUrl을 admin API로 설정합니다. 【F:short-url-redirect/src/main/java/com/nh/shorturl/redirect/service/ShortUrlCacheWarmer.java†L15-L45】【F:short-url-redirect/src/main/java/com/nh/shorturl/redirect/config/AppConfig.java†L20-L37】
- 공통 DTO 모듈(`common`)을 사용하며, Web/WebFlux, Cache(Caffeine) 의존성을 포함합니다. 【F:short-url-redirect/build.gradle†L1-L20】

### short-url-admin-ui
- React Router 기반으로 대시보드, 프로젝트/사양 뷰, 인증·단축 URL·리디렉션 제어, 사용자·클라이언트 키 관리 등 관리자 화면을 구성합니다. 【F:short-url-admin-ui/src/App.tsx†L18-L50】
- Vite + Yarn 빌드 파이프라인을 Gradle로 래핑하여 의존성 설치와 프로덕션 번들을 자동화합니다. 【F:short-url-admin-ui/build.gradle†L1-L60】
- UI 엔트리포인트에서 `ThemeProvider`와 `BrowserRouter`를 감싸 React 앱을 부팅합니다. 【F:short-url-admin-ui/src/main.tsx†L1-L16】

## 모듈 관계도 (Mermaid)

```mermaid
graph LR
    common[common (DTO/타입 공유)]
    admin[short-url-admin (관리 API)]
    redirect[short-url-redirect (리디렉션 엣지)]
    admin_ui[short-url-admin-ui (관리자 UI)]

    common --> admin
    common --> redirect
    admin --> redirect
    admin_ui --> admin
    admin --> admin_ui

    subgraph 데이터 흐름
        admin -- 내부 API (캐시/통계) --> redirect
        redirect -- WebClient 호출 --> admin
    end
```
