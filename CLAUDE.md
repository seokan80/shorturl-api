# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Multi-Module Architecture

This is a Gradle multi-module project with three modules (see `settings.gradle`):

1. **`common`** — Shared DTOs, validation annotations, and OpenAPI definitions used across modules. Published as `java-library` with `api` dependencies that are transitive to consumers. DTOs live under `common/src/main/java/com/nh/shorturl/dto/` — **always modify shared DTOs here**, not inside individual modules.

2. **`short-url-admin`** — Single Spring Boot application that serves **both** management APIs and the `/r/{key}` redirect endpoint. Entry point: `ShortUrlAdminApplication.java`. See [docs/구현/07_Unified_App_Merge_Design.md](docs/구현/07_Unified_App_Merge_Design.md) for the rationale behind the merged layout.

3. **`short-url-admin-ui`** — React 18 + TypeScript + Vite frontend bundled via Gradle node plugin. Build produces static assets in `build/dist` that `short-url-admin` serves.

> The former `short-url-redirect` module was absorbed into `short-url-admin/redirect/`. One JAR, one container, one `application.yml`, single port (8080). Do not look for a separate redirect module or WebClient-based internal API — it no longer exists.

### Package layout of `short-url-admin`

```
com.nh.shorturl.admin
├── ShortUrlAdminApplication.java
├── config/               # App-wide config (Caffeine cache, async, etc.)
├── controller/           # /api/short-url, /api/redirection-history
├── service/shorturl/     # Admin CRUD (ShortUrlService + Impl)
├── service/history/      # Analytics persistence (RedirectionHistoryService)
├── repository/           # JPA repositories + QueryDSL custom impls
├── entity/               # ShortUrl, RedirectionHistory
├── redirect/             # Redirect-layer (sub-package, same JVM)
│   ├── controller/       #   ShortUrlRedirectController (/r/{shortKey})
│   ├── service/          #   ShortUrlLookupService, RedirectionConfigStore,
│   │                     #   RedirectionHistoryAsyncWriter
│   └── config/           #   RedirectCacheConfig (Caffeine LoadingCache)
├── converter/            # BooleanToYnConverter, etc.
└── util/                 # Base62, UserAgentParser, DateUtils, RequestInfoUtils
```

### Closed-network scope (important)

This branch (`feature/unified-app-merge`) targets a **closed internal network**. User/Auth(JWT)/ClientAccessKey features were removed intentionally:

- No Spring Security filter chain, no JWT, no `X-CLIENTACCESS-KEY` header.
- `ShortUrl` entity has no owner FK — it's a pure `key → URL` mapping.
- Anyone with network access to the admin URL can call all CRUD endpoints.
- **Never deploy this branch to a publicly reachable environment.** The original auth stack is preserved in `main` and in the `pre-unified-merge` tag.

See [07_Unified_App_Merge_Design.md §13](docs/구현/07_Unified_App_Merge_Design.md) for the full removal list.

## Build, Test, and Development Commands

### Build
- **Build everything**: `./gradlew clean build` — compiles all modules, runs tests, bundles the frontend, produces `short-url-admin-0.0.1-SNAPSHOT.jar`.
- **Build only backend**: `./gradlew :short-url-admin:build`
- **Skip tests**: `./gradlew build -x test`
- **Profile**: `-Pprofile=dev` or `-Pprofile=prod` (defaults to `local`).

### Run
- **Admin (the only service)**: `./gradlew :short-url-admin:bootRun` — listens on `8080` by default. Serves both `/api/**` and `/r/{key}`.
- **Run JAR**: `java -jar short-url-admin/build/libs/short-url-admin-0.0.1-SNAPSHOT.jar`
- **Jasypt master key**: `export JASYPT_ENCRYPTOR_PASSWORD=your_master_key` before running if the active profile uses `ENC(...)` values.
- **Local DB**: H2 in file mode (see `application.yml`) so data survives restarts. Dev/prod use Oracle.

### Test
- **All tests**: `./gradlew test`
- **Backend only**: `./gradlew :short-url-admin:test`
- **Single class**: `./gradlew :short-url-admin:test --tests ShortUrlControllerTest`
- **Coverage**: `./gradlew jacocoTestReport` → `short-url-admin/build/reports/jacoco/test/html/index.html`
- **Frontend tests**: `cd short-url-admin-ui && yarn test`

### Frontend workflow (from `short-url-admin-ui/`)
- `yarn install` — install dependencies (yarn.lock is checked in).
- `yarn dev` — Vite dev server with HMR. `/api/**` is proxied to the backend per `vite.config.ts`.
- `yarn build` — production bundle into `dist/`. Gradle's node plugin invokes this during `:short-url-admin-ui:build`.
- `yarn test` — Vitest + Testing Library, jsdom setup in `src/setupTests.ts`. See `src/test-utils.tsx` for the shared `renderWithRouter` helper.

## Admin UI (`short-url-admin-ui`)

Vite + React 18 + TypeScript with Tailwind. `src/main.tsx` wraps `App.tsx` in `BrowserRouter` and a custom `ThemeProvider` (persists light/dark in `localStorage`). `App.tsx` uses `AdminLayout` as the shell — sidebar (`components/layout/AdminSidebar.tsx`) + topbar with `ThemeToggle`.

- Feature modules live in `src/features/`. Each page owns its transient state (form fields, filters, busy flags) and calls `/api/**` directly via `fetch`. Backend responses are wrapped in `ResultEntity<T>` / `ApiResult` — unwrap before rendering.
- Reusable primitives inspired by shadcn/ui live in `src/components/ui/`. Merge Tailwind classes with the `cn` helper (`src/lib/utils.ts`).
- Extend theme tokens in `tailwind.config.ts` when adding brand colors.
- The backend serves the built assets from `build/dist` at the application root (`/`). `/r/**` is reserved for redirects, so do not add frontend routes under that path.

## Key Architecture Patterns

### Short URL generation & redirect flow

1. **Generation** — `ShortUrlServiceImpl.createShortUrl()` picks a random `Base62.encodeUUID(UUID.randomUUID())` key and retries on collision (`existsByShortUrl`). The entity is saved via JPA and the local Caffeine cache is immediately updated (`notifyCacheUpdate`).
2. **Cache** — `RedirectCacheConfig` builds two Caffeine caches:
   - `shortUrl` — `LoadingCache<String, ShortUrlResponse>` with `expireAfterWrite=5m` + `refreshAfterWrite=60s`, loader hits `ShortUrlRepository.findByShortUrl`.
   - `shortUrlMissing` — negative cache (`expireAfterWrite=30s`) to block 404 floods.
3. **Redirect** — `ShortUrlRedirectController` (`/r/{shortKey}`) asks `ShortUrlLookupService` (which consults both caches), applies tracking-field propagation from `RedirectionConfigStore`, fires `RedirectionHistoryAsyncWriter.write(...)` (async), then `response.sendRedirect(targetUrl)`.
4. **Multi-node consistency** — Each node has its own local Caffeine. Writes are immediate on the node that owns the request; other nodes converge within `refreshAfterWrite` (60s). This is intentional — acceptable for the closed-network use case, avoids Redis.

### Analytics

`RedirectionHistoryAsyncWriter` parses User-Agent (`UserAgentParser`) and request metadata (`RequestInfoUtils`), then inserts a `RedirectionHistory` row. `RedirectionHistoryRepositoryCustomImpl` uses QueryDSL for dynamic aggregation — `findRedirectionStats` builds `GROUP BY` clauses based on the runtime `groupBy` parameter (`REFERER`, `YEAR`, `MONTH`, etc.).

### Response envelope

Public REST endpoints return `ResultEntity<T>` / `ResultList<T>` wrapping an `ApiResult` code. Controllers should be thin: delegate to services, wrap the return value, and let a global handler map exceptions to error results (see `docs/plan/refactoring-2026-04.md` P0-1 — currently each controller still has its own try/catch, this is being refactored).

## Coding Style & Conventions

- **Java 17**, Spring Boot 3.2.5, 4-space indent, 120-char line limit.
- Packages follow `com.nh.shorturl.admin.*` (no `redirect` top-level package anymore — it's a sub-package).
- Use Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) and constructor injection.
- Services use the interface + `Impl` pair (`ShortUrlService` → `ShortUrlServiceImpl`).
- Use JPA converters (`BooleanToYnConverter`) for DB-specific type mappings.
- Commit style: `feat:` / `fix:` / `docs:` / `chore:` — lowercase keyword, colon, short imperative subject in Korean or English (`fix: align redirect stats query`).

## Testing Guidelines

JUnit 5 (via `spring-boot-starter-test`) drives the backend test stack. Place tests under `short-url-admin/src/test/java/com/nh/shorturl/admin/...` mirroring the main package. Prefer Spring slices (`@WebMvcTest`, `@DataJpaTest`) over full context loads where possible, and assert HTTP payloads through `MockMvc`.

Critical areas that should have coverage:
- `Base62.encodeUUID` — length, charset, reproducibility
- `ShortUrlServiceImpl` — create/expire/cache put+evict interaction
- `ShortUrlLookupService` — positive + negative cache transitions
- `RedirectionHistoryRepositoryCustomImpl` — QueryDSL `groupBy` branches

Frontend tests use Vitest + RTL; see any `*.test.tsx` for patterns.

## Security & Configuration

- **Jasypt encryption** — Encrypted properties use `ENC(...)`. Set `JASYPT_ENCRYPTOR_PASSWORD` before run.
- **Database** — H2 (file mode) for `local`, Oracle for `dev`/`prod`. Schema managed via `schema.sql` in `src/main/resources`. An empty `data.sql` is intentionally absent now — its previous presence caused startup failures.
- **No auth layer** — This branch has no Spring Security. All `/api/**` endpoints are unauthenticated. See "Closed-network scope" above.
- **Redirect error page** — Currently rendered as an inline HTML string in `ShortUrlRedirectController#handleFailure`. The `reason` value is interpolated unescaped — this is being refactored (see `docs/plan/refactoring-2026-04.md` P0-2). Do not add new inline HTML; extract to a template.
