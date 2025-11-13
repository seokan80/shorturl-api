# Repository Guidelines

## Project Structure & Module Organization
The Gradle root contains three modules: `common` (shared DTOs, error codes, utility layers), `short-url-admin` (admin APIs and CMS flows), and `short-url-redirect` (edge redirect service). Each module keeps feature-oriented packages under `src/main/java/com/nh/shorturl/<area>` with the familiar `controller`, `service`, `repository`, `entity`, and `config` folders, mirrored in `src/test/java`. Application properties, Flyway migrations, and profile YAMLs live in each module’s `src/main/resources`. The operations console is a Vite + React workspace at `short-url-admin/src/main/ui`, where primitives sit in `src/components/ui`, feature routes in `src/features`, and shared helpers in `src/lib`.

## Build, Test, and Development Commands
- `./build.sh <local|dev|prod>` – profile-aware wrapper that builds every module.
- `./gradlew clean build -Pprofile=local` – compile, run tests, and publish module JARs to `build/libs`.
- `./gradlew :short-url-admin:bootRun` / `./gradlew :short-url-redirect:bootRun` – launch a single Spring app with dev reload.
- `./gradlew test` or `./gradlew :module:test` – execute the JUnit 5 suites.
- Inside `short-url-admin/src/main/ui`: `yarn install`, `yarn dev` (proxying `/api` to `localhost:8080`), `yarn build`, `yarn test`.

## Coding Style & Naming Conventions
Target Java 17 with four-space indentation and sub-120-character lines. Keep packages under `com.nh.shorturl`, name Spring beans after their responsibility, and favor constructor injection plus Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`). All REST endpoints return the shared `ApiResult`. Frontend code uses React 18 functional components, Tailwind utilities composed via the `cn` helper, and theme tokens defined in `tailwind.config.ts`.

## Testing Guidelines
JUnit 5 backs backend tests; name files `*Test`, mirror the main package path, and rely on Spring slices (`@WebMvcTest`, `@DataJpaTest`) plus MockMvc/TestRestTemplate for boundary assertions. Cover auth issuance, redirect routing, and repository projections before every push via `./gradlew test` (or the module-specific variant). The admin UI relies on Vitest + Testing Library; co-locate specs near components and stub `/api/...` fetches.

## Commit & Pull Request Guidelines
Commits follow the `type: imperative summary` format. Keep change sets focused, reference issue IDs when available, and avoid mixing refactors with features. Pull requests should state the goal, risks, verification commands (`./gradlew clean build -Pprofile=local`, `yarn test`), and attach screenshots or HTTP-client traces for UI/API tweaks while flagging new env vars or migrations.

## Security & Configuration Tips
Load secrets (DB credentials, JWT/Jasypt keys) through environment variables and export `JASYPT_ENCRYPTOR_PASSWORD` before Gradle tasks that touch encrypted properties. Do not log issued tokens or client access keys. After running `api-flow.http` or `url_shortener_script.sh`, rotate demo credentials and clear temporary shell exports to keep secrets scoped.
