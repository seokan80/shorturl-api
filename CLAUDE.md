# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Multi-Module Architecture

This is a Gradle multi-module project with four distinct modules:

1. **`common`** - Shared DTOs, validation annotations, and OpenAPI definitions used across modules. Published as `java-library` with `api` dependencies that are transitive to consumers.

2. **`short-url-admin`** - Primary backend module handling user management, JWT authentication, short URL creation/management, and analytics. Exposes REST APIs with Spring Security (JWT), Spring Data JPA, QueryDSL for custom queries, and Swagger UI at `/swagger-ui.html`. Entry point: `ShortUrlAdminApplication.java`.

3. **`short-url-redirect`** - Lightweight redirect service that handles `/r/{shortUrlKey}` requests. Uses Caffeine cache for high-performance lookups and WebClient to fetch URL mappings from the admin service's internal API. Entry point: `ShortUrlRedirectApplication.java`.

4. **`short-url-admin-ui`** - React 18 + TypeScript + Vite frontend bundled via Gradle node plugin. Build produces static assets in `build/dist` that the admin backend serves.

**Key points:**
- Each application module (admin, redirect) is independently runnable with its own `application.yml`
- `common` module contains DTOs (`ShortUrlRequest`, `ShortUrlResponse`, `UserUpdateRequest`, etc.) shared between modules—**always modify DTOs in `common/src/main/java/com/nh/shorturl/dto/`**, not in individual modules
- Build the entire project from root with `./gradlew clean build`, which compiles all modules and runs all tests
- The admin and redirect services can run simultaneously on different ports (configured in their respective `application.yml`)

## Project Structure & Module Organization
Each backend module follows standard Spring Boot layout: application code in `src/main/java/com/nh/shorturl/{module-name}`, grouped by feature (`controller`, `entity`, `repository`, `config`, `service`, `util`). Configuration files and resources live in `src/main/resources`. Tests mirror main package structure under `src/test/java/com/nh/shorturl/{module-name}`. Ancillary workflow files include `api-flow.http` for IntelliJ HTTP Client scenarios and `url_shortener_script.sh` for end-to-end shell testing.

## Admin UI (short-url-admin-ui)
The CMS that operations teams use to manage specs, auth keys, and analytics lives in the `short-url-admin-ui` module. It is a Vite + React 18 + TypeScript project styled with Tailwind (`package.json`, `tailwind.config.ts`) and ships reusable primitives inspired by shadcn/ui (`src/components/ui`). Entry wiring happens in `src/main.tsx`, which wraps `App.tsx` with `BrowserRouter` and a custom `ThemeProvider` that persists the light/dark choice via `localStorage` (`src/theme/ThemeProvider.tsx`).

### Layout, routing, and features
- `App.tsx` defines the router tree with `AdminLayout` as the shell: the sidebar (`components/layout/AdminSidebar.tsx`) exposes navigation to specs, controls, and analytics, while `AdminTopbar` hosts the `ThemeToggle`.
- Feature modules live in `src/features` and mirror business concepts: e.g., dashboard widgets read mock data from `src/data`, spec views (`features/specs`) render detailed tabs, and workflow/settings/auth pages bundle their own cards, tables, and forms.
- Server-managed controls (such as `features/auth/ClientAccessKeyPage.tsx`) already call the backend REST API and expect the Spring responses wrapped in `ApiResult`; keep those fetch paths under `/api/...` so Vite's dev proxy can forward to the admin backend (`vite.config.ts`).

### Frontend development workflow
- Navigate to `short-url-admin-ui` directory for frontend work
- Install dependencies with `yarn install` (a `yarn.lock` is checked in); use `yarn dev` for Vite's hot module reload server and `yarn build` for production bundles
- Unit/UI tests run through Vitest + Testing Library with the shared `renderWithRouter` helper (`src/test-utils.tsx`) and a lightweight `jsdom` setup in `src/setupTests.ts`; `yarn test` executes them headlessly
- Tailwind utilities are merged via the `cn` helper (`src/lib/utils.ts`)—compose new components with that helper instead of manual string concatenation, and extend theme tokens in `tailwind.config.ts` when introducing new brand colors
- When integrating new API calls, honor the existing optimistic-update patterns in `ClientAccessKeyPage` (status banner, busy state labels) so the UI stays consistent; prefer co-locating transient form state inside each page component and lift only cross-feature state up when it truly becomes shared
- The Gradle build automatically runs `yarn install` and `yarn build` via the node-gradle plugin, outputting to `build/dist`

## Build, Test, and Development Commands

### Building
- **Build everything**: `./gradlew clean build` - compiles all modules, runs tests, bundles frontend, and creates executable JARs
- **Build specific module**: `./gradlew :short-url-admin:build` or `./gradlew :short-url-redirect:build`
- **Skip tests**: `./gradlew build -x test`
- **Profile-specific builds**: Use `-Pprofile=dev` or `-Pprofile=prod` (defaults to `local`)

### Running applications
- **Admin service**: `./gradlew :short-url-admin:bootRun` (default port 8080, configurable in `short-url-admin/src/main/resources/application.yml`)
- **Redirect service**: `./gradlew :short-url-redirect:bootRun` (check `short-url-redirect/src/main/resources/application.yml` for port)
- **Run built JAR**: `java -jar short-url-admin/build/libs/short-url-admin-0.0.1-SNAPSHOT.jar`
- **IMPORTANT**: Set `JASYPT_ENCRYPTOR_PASSWORD` environment variable before running: `export JASYPT_ENCRYPTOR_PASSWORD=your_master_key`

### Testing
- **All tests**: `./gradlew test`
- **Single module tests**: `./gradlew :short-url-admin:test`
- **Single test class**: `./gradlew :short-url-admin:test --tests ShortUrlControllerTest`
- **Frontend tests**: `cd short-url-admin-ui && yarn test`
- **Coverage report**: `./gradlew jacocoTestReport` (reports in `build/reports/jacoco/test/html/index.html`)

### Frontend-only commands (from `short-url-admin-ui` directory)
- `yarn install` - install dependencies
- `yarn dev` - start Vite dev server with hot reload (proxies `/api` to backend)
- `yarn build` - production build (output to `dist/`)
- `yarn test` - run Vitest tests

## Coding Style & Naming Conventions
This codebase targets Java 17 with Spring Boot 3.2.5; use four-space indentation and keep lines under 120 characters. Package naming follows `com.nh.shorturl.{module-name}.*` (e.g., `com.nh.shorturl.admin.*`, `com.nh.shorturl.redirect.*`). Favor Lombok annotations already in use (`@Getter`, `@Builder`, `@RequiredArgsConstructor`) and prefer constructor injection for Spring components. Public REST endpoints should return the `ApiResult` wrapper and reside in `controller`; business logic belongs in dedicated service classes (interface + `Impl`) before reaching repositories. Use JPA converters (`BooleanToYnConverter`) for database-specific type mappings.

## Testing Guidelines
JUnit 5 (Spring Boot starter) powers the backend test stack. Name test classes with the `*Test` suffix and place them in the same package structure under `src/test/java`. Mock external boundaries with Spring test slices (`@WebMvcTest`, `@DataJpaTest`) and assert request/response payloads with `MockMvc` or `TestRestTemplate`. Run `./gradlew test` locally before each push, aiming to cover URL generation (Base62 encoding), JWT/token flows, repository queries (including QueryDSL custom implementations), and redirect caching logic. Frontend tests use Vitest + React Testing Library; see `DashboardPage.test.tsx` for patterns.

## Commit & Pull Request Guidelines
Follow the existing conventional prefix plus subject format: `feat:`, `fix:`, `docs:`, `chore:`—note the lowercase keyword, colon, and a short imperative summary (`fix: align redirect stats query`). Group related changes per commit and reference issue IDs where available. Pull requests should include: goal summary, risk assessment, verification notes (commands run, screenshots for API clients), and links to related issues or API tickets.

## Key Architecture Patterns

### Authentication & Authorization Flow
1. **Client Access Keys**: Top-level credentials required for `/api/users/**` and `/api/auth/**` endpoints. Validated by `ClientAccessKeyValidationFilter` which checks the `X-CLIENTACCESS-KEY` header against the `CLIENT_ACCESS_KEY` table.
2. **JWT Tokens**: After user registration, `/api/auth/token/issue` generates a JWT (via `JwtProvider`) and UUID-based refresh token. JWTs authenticate subsequent API calls to protected endpoints via `JwtAuthenticationFilter`.
3. **Dual Security Layers**: `SecurityConfig` configures Spring Security with filter chain: Client Access Key filter for auth endpoints, JWT filter for all others.

### Short URL Generation & Redirect Flow
1. **Generation** (`short-url-admin`): `ShortUrlService` uses `Base62.encode(id)` to create short keys. IDs come from database sequences, ensuring uniqueness.
2. **Storage**: Short URLs stored in `SHORT_URL` table with owner user ID, expiration dates, and soft-delete flags.
3. **Redirect** (`short-url-redirect`): `/r/{key}` hits `ShortUrlRedirectController`, which queries `ShortUrlCacheService` (Caffeine-backed). Cache misses trigger WebClient call to admin service's internal API (`ShortUrlInternalApiController`).
4. **Analytics**: Redirect controller asynchronously posts visit metadata (referer, user-agent, timestamp) to admin service via `RedirectionHistoryService`, which parses and stores in `REDIRECTION_HISTORY` table.

### QueryDSL for Complex Queries
Custom repository implementations (`RedirectionHistoryRepositoryCustomImpl`) use QueryDSL for dynamic grouping and aggregation. Example: `RedirectionHistoryRepository.findRedirectionStats` builds queries based on runtime `groupBy` parameters (REFERER, YEAR, MONTH, etc.).

## Security & Configuration
- **Jasypt encryption**: Set `JASYPT_ENCRYPTOR_PASSWORD` environment variable before running. Encrypted properties in `application.yml` use `ENC(...)` syntax.
- **JWT secrets**: Configured in `application.yml` per environment (local/dev/prod). Never log or commit raw secrets.
- **Database**: H2 for local profile, Oracle for dev/prod. Schema managed via `schema.sql` scripts in `src/main/resources`.
- **Client Access Keys**: Manage via `/api/client-keys` endpoints or directly in database. Include expiration dates and active flags.
