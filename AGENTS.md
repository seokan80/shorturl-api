# Repository Guidelines

## Project Structure & Module Organization
Application code lives in `src/main/java/com/nh/shorturl`, grouped by feature: `controller`, `entity`, `repository`, `config`, `util`, and `type`. Shared constants sit in `constants`, while application entry is `ShortUrlApplication`. Static configuration such as application properties and migrations belong in `src/main/resources`. Keep automated tests under `src/test/java/com/nh/shorturl`, mirroring the main package layout; add resource fixtures to `src/test/resources`. Ancillary workflow files include `api-flow.http` for IntelliJ HTTP Client scenarios and `url_shortener_script.sh` for end-to-end shell testing.

## Admin UI (src/main/ui)
The CMS that operations teams use to manage specs, auth keys, and analytics lives under `src/main/ui`. It is a Vite + React 18 + TypeScript project styled with Tailwind (`package.json`, `tailwind.config.ts`) and ships reusable primitives inspired by shadcn/ui (`src/main/ui/src/components/ui`). Entry wiring happens in `src/main/ui/src/main.tsx`, which wraps `App.tsx` with `BrowserRouter` and a custom `ThemeProvider` that persists the light/dark choice via `localStorage` (`src/main/ui/src/theme/ThemeProvider.tsx`).

### Layout, routing, and features
- `App.tsx` defines the router tree with `AdminLayout` as the shell: the sidebar (`components/layout/AdminSidebar.tsx`) exposes navigation to specs, controls, and analytics, while `AdminTopbar` hosts the `ThemeToggle`.
- Feature modules live in `src/main/ui/src/features` and mirror business concepts: e.g., dashboard widgets read mock data from `src/main/ui/src/data`, spec views (`features/specs`) render detailed tabs, and workflow/settings/auth pages bundle their own cards, tables, and forms.
- Server-managed controls (such as `features/auth/ClientAccessKeyPage.tsx`) already call the backend REST API and expect the Spring responses wrapped in `ApiResult`; keep those fetch paths under `/api/...` so Vite’s dev proxy can forward to `localhost:8080` (`vite.config.ts`).

### Frontend development workflow
- Install dependencies once with `yarn install` (a `yarn.lock` is checked in); use `yarn dev` for Vite’s hot module reload server and `yarn build` for production bundles (`package.json` scripts).
- Unit/UI tests run through Vitest + Testing Library with the shared `renderWithRouter` helper (`src/main/ui/src/test-utils.tsx`) and a lightweight `jsdom` setup in `src/main/ui/src/setupTests.ts`; `yarn test` executes them headlessly, as shown by `DashboardPage.test.tsx`.
- Tailwind utilities are merged via the `cn` helper (`src/main/ui/src/lib/utils.ts`)—compose new components with that helper instead of manual string concatenation, and extend theme tokens in `tailwind.config.ts` when introducing new brand colors.
- When integrating new API calls, honor the existing optimistic-update patterns in `ClientAccessKeyPage` (status banner, busy state labels) so the UI stays consistent; prefer co-locating transient form state inside each page component and lift only cross-feature state up when it truly becomes shared.

## Build, Test, and Development Commands
Run `./gradlew clean build` to compile, execute tests, and assemble the shaded JAR into `build/libs/short-url-0.0.1-SNAPSHOT.jar`. Use `./gradlew bootRun` for a hot-reloading developer server on port 8080. Execute targeted unit and integration suites with `./gradlew test`. Regenerate dependency metadata or inspect tasks via `./gradlew tasks --group application`.

## Coding Style & Naming Conventions
This codebase targets Java 17 with Spring Boot 3; use four-space indentation and keep lines under 120 characters. Adhere to the package naming pattern `com.nh.shorturl.*` and descriptive class names like `ShortUrlController`. Favor Lombok annotations already in use (`@Getter`, `@Builder`) and prefer constructor injection for Spring components. Public REST endpoints should return the `ApiResult` wrapper and reside in `controller`; business logic belongs in dedicated service classes before reaching repositories.

## Testing Guidelines
JUnit 5 (Spring Boot starter) powers the test stack. Name test classes with the `*Test` suffix and place integration suites alongside their domain counterparts (`ShortUrlControllerTest` under the matching package). Mock external boundaries with Spring test slices (`@WebMvcTest`, `@DataJpaTest`) and assert request/response payloads with `MockMvc` or `TestRestTemplate`. Run `./gradlew test` locally before each push, aiming to cover URL generation, JWT/token flows, and repository queries that map native SQL projections.

## Commit & Pull Request Guidelines
Follow the existing conventional prefix plus subject format: `feat:`, `fix:`, `docs:`, `chore:`—note the lowercase keyword, colon, and a short imperative summary (`fix: align redirect stats query`). Group related changes per commit and reference issue IDs where available. Pull requests should include: goal summary, risk assessment, verification notes (commands run, screenshots for API clients), and links to related issues or API tickets.

## Security & Configuration Tips
Configure secrets through environment variables rather than committing them—set `JASYPT_ENCRYPTOR_PASSWORD` before running any Gradle task that reads encrypted properties. Avoid sharing generated JWTs in logs or documentation. When testing API flows, export temporary access keys via your shell session (`export X_access_key=...`) and rotate them after demos.
