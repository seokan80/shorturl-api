# Repository Guidelines

## Project Structure & Module Organization

Gradle multi-module project with three modules (see `settings.gradle`):

- **`common/`** — Shared DTOs (`common/src/main/java/com/nh/shorturl/dto/...`), validation annotations, and `type.ApiResult`. Published as a Java library; consumers inherit transitive `api` dependencies. **Always modify shared DTOs here**, not inside individual modules.
- **`short-url-admin/`** — Single Spring Boot application serving **both** management APIs (`/api/**`) and the public redirect endpoint (`/r/{shortKey}`). Entry point: `ShortUrlAdminApplication.java`. Feature packages inside `src/main/java/com/nh/shorturl/admin/`: `controller`, `service/{shorturl,history}`, `repository`, `entity`, `config`, `converter`, `util`, and a dedicated `redirect/` sub-package (`controller`, `service`, `config`) for the redirect layer. Static configuration and `schema.sql` live in `src/main/resources`. Tests mirror the main layout under `src/test/java/com/nh/shorturl/admin/...`.
- **`short-url-admin-ui/`** — Vite + React 18 + TypeScript frontend bundled via the Gradle node plugin. Production build outputs to `short-url-admin-ui/build/dist`, which the admin backend serves at `/`.

> The former `short-url-redirect` module was absorbed into `short-url-admin/redirect/`. There is **one JAR, one container, one `application.yml`, one port (8080)**. Do not look for a separate redirect module.

Ancillary files: `api-flow.http` (IntelliJ HTTP Client scenarios), `url_shortener_script.sh` (end-to-end shell testing), `docs/구현/07_Unified_App_Merge_Design.md` (rationale for the merged layout).

## Closed-network scope (important)

The `feature/unified-app-merge` branch targets a **closed internal network**:

- No Spring Security, no JWT, no `X-CLIENTACCESS-KEY` header, no User entity.
- `ShortUrl` is a pure `key → URL` mapping without an owner FK.
- All `/api/**` endpoints are unauthenticated — **never deploy this branch to a publicly reachable environment**.
- The original authentication stack is preserved in `main` and in the `pre-unified-merge` tag; cherry-pick from there if auth is needed again.

## Admin UI (`short-url-admin-ui/`)

Vite + React 18 + TypeScript with Tailwind. Entry wiring in `src/main.tsx` wraps `App.tsx` in `BrowserRouter` and a custom `ThemeProvider` (persists light/dark in `localStorage` via `src/theme/ThemeProvider.tsx`). `App.tsx` uses `AdminLayout` as the shell — sidebar (`components/layout/AdminSidebar.tsx`) plus topbar with `ThemeToggle`.

- Feature modules under `src/features/` mirror business concepts (short URL management, redirection history, dashboard). Pages own their transient state and call `/api/**` directly via `fetch`. Unwrap `ResultEntity<T>` / `ResultList<T>` responses before rendering.
- Reusable primitives inspired by shadcn/ui live in `src/components/ui/`. Merge Tailwind classes with the `cn` helper in `src/lib/utils.ts` instead of string concatenation, and extend theme tokens in `tailwind.config.ts` for new brand colors.
- `/r/**` is reserved for the backend redirect endpoint — do not register frontend routes under that path.

### Frontend development workflow
- Install deps once with `yarn install` (a `yarn.lock` is checked in).
- `yarn dev` — Vite dev server with HMR. `/api/**` is proxied to the backend per `vite.config.ts`.
- `yarn build` — production bundle into `dist/` (Gradle's node plugin calls this during `:short-url-admin-ui:build`, placing artifacts under `build/dist`).
- `yarn test` — Vitest + Testing Library headless run. Shared helpers in `src/test-utils.tsx`, jsdom bootstrap in `src/setupTests.ts`.

## Build, Test, and Development Commands

- `./gradlew clean build` — full build: compile, test, bundle frontend, produce `short-url-admin/build/libs/short-url-admin-0.0.1-SNAPSHOT.jar`.
- `./gradlew :short-url-admin:bootRun` — run the backend on port 8080 (serves `/api/**`, `/r/{key}`, and the admin UI at `/`).
- `./gradlew :short-url-admin:test` — backend tests only.
- `./gradlew :short-url-admin:test --tests ShortUrlControllerTest` — single test class.
- `./gradlew jacocoTestReport` — coverage at `short-url-admin/build/reports/jacoco/test/html/index.html`.
- `./gradlew build -x test` — skip tests.
- `-Pprofile=dev` / `-Pprofile=prod` — profile selection (defaults to `local`).
- Frontend-only: `cd short-url-admin-ui && yarn test` (or `yarn dev` / `yarn build`).

## Coding Style & Naming Conventions

Java 17, Spring Boot 3.2.5. Four-space indentation, 120-char line limit. Packages follow `com.nh.shorturl.admin.*` (the redirect layer is a sub-package, not a top-level package). Favor Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) and constructor injection for Spring components. Services use the interface + `Impl` pair (`ShortUrlService` → `ShortUrlServiceImpl`). Public REST endpoints live in `controller/` and return the `ResultEntity` / `ResultList` envelope with an `ApiResult` code; controllers should stay thin and delegate to services. Use JPA converters (`BooleanToYnConverter`) for DB-specific type mappings.

## Testing Guidelines

JUnit 5 (via `spring-boot-starter-test`). Name classes with the `*Test` suffix and place them under `short-url-admin/src/test/java/com/nh/shorturl/admin/...` mirroring the main package. Prefer Spring slices (`@WebMvcTest`, `@DataJpaTest`) over full-context loads; assert HTTP payloads with `MockMvc`. Critical areas that warrant coverage: `Base62` encoding, `ShortUrlServiceImpl` create/expire/cache flow, `ShortUrlLookupService` positive+negative cache transitions, and `RedirectionHistoryRepositoryCustomImpl` QueryDSL `groupBy` branches. Run `./gradlew test` before each push. Frontend tests use Vitest + React Testing Library.

## Commit & Pull Request Guidelines

Follow the existing conventional prefix format: `feat:`, `fix:`, `docs:`, `chore:` — lowercase keyword, colon, short imperative subject (`fix: align redirect stats query`). Group related changes per commit and reference issue IDs when available. Pull requests should include goal summary, risk assessment, verification notes (commands run, screenshots for API clients), and links to related issues or tickets.

## Security & Configuration Tips

- **Jasypt** — Encrypted properties in `application*.yml` use `ENC(...)`. Set `JASYPT_ENCRYPTOR_PASSWORD` before running any Gradle task that reads encrypted values.
- **Database** — H2 in file mode for the `local` profile (data survives restarts). Dev/prod use Oracle. Schema is managed via `schema.sql`; the former `data.sql` was removed because an empty file caused `DataSourceInitializer` to fail.
- **No auth layer** — There is no Spring Security or JWT on this branch. All management endpoints are unauthenticated by design. See "Closed-network scope" above.
- **Do not log secrets** — Avoid echoing Jasypt master keys, DB credentials, or Oracle connection strings in logs, commits, or documentation.
