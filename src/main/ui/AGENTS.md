# Repository Guidelines

## Project Structure & Module Organization
The Gradle root hosts three modules: `common` (shared DTOs, error codes, util classes), `short-url-admin` (admin APIs, CMS, analytics), and `short-url-redirect` (low-latency redirect edge). Each module keeps feature folders under `src/main/java/com/nh/shorturl/<area>` with the usual `controller`, `service`, `repository`, `entity`, and `config` packages, plus matching `src/test/java` mirrors. Configuration, Flyway migrations, and profile YAMLs sit in each module’s `src/main/resources`. The operations console is a Vite + React app under `short-url-admin/src/main/ui`, with primitives in `src/main/ui/src/components/ui` and business pages in `src/main/ui/src/features`. API flow docs and helper scripts (`api-flow.http`, `url_shortener_script.sh`, `docs/`) live at the root for manual verification.

## Build, Test, and Development Commands
- `./build.sh <local|dev|prod>` — preferred wrapper that applies the chosen Gradle profile across all modules.
- `./gradlew clean build -Pprofile=local` — compile, run tests, and emit per-module JARs in `build/libs`.
- `./gradlew :short-url-admin:bootRun` / `:short-url-redirect:bootRun` — start a single Spring service with dev reload.
- `./gradlew test` or `./gradlew :module:test` — execute the JUnit 5 suites.
- Inside `short-url-admin/src/main/ui`: `yarn install`, `yarn dev` (proxying `/api` to `localhost:8080`), `yarn build`, `yarn test`.

## Coding Style & Naming Conventions
Target Java 17, four-space indentation, and <120-character lines. Keep package prefixes under `com.nh.shorturl`, use descriptive bean names (`AdminAccessKeyController`, `RedirectTrackingService`), and prefer constructor injection with Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`). HTTP responses return the shared `ApiResult`. Frontend code sticks to React 18 functional components, Tailwind utilities composed via the `cn` helper, and theme tokens defined in `tailwind.config.ts`.

## Testing Guidelines
JUnit 5 is mandatory; name files `*Test`, mirror the main package path, and leverage Spring test slices (`@WebMvcTest`, `@DataJpaTest`) to isolate dependencies. Cover auth issuance, redirect routing, and persistence projections before every push by running `./gradlew test` (or module-specific variants). The admin UI relies on Vitest + Testing Library plus `renderWithRouter` (`src/main/ui/src/test-utils.tsx`); co-locate specs near components and mock `/api/...` fetches.

## Commit & Pull Request Guidelines
Use the `type: imperative summary` pattern (`feat: add redirect throttling`, `fix: patch admin token refresh`). Keep commits scoped, reference issue IDs when available, and avoid mixing unrelated refactors. PRs should outline goals, risks, verification commands (`./gradlew clean build -Pprofile=local`, `yarn test`), and attach screenshots or HTTP-client traces for UI/API tweaks; call out new env vars or migrations explicitly.

## Security & Configuration Tips
Load secrets (DB creds, JWT/Jasypt keys) through environment variables and export `JASYPT_ENCRYPTOR_PASSWORD` before Gradle tasks that touch encrypted configs. Never log generated access keys or tokens. When running `api-flow.http` or `url_shortener_script.sh`, rotate demo credentials afterward and clear temporary shell exports to avoid accidental leaks.
