# Repository Guidelines

## Project Structure & Module Organization
Application code lives in `src/main/java/com/nh/shorturl`, grouped by feature: `controller`, `entity`, `repository`, `config`, `util`, and `type`. Shared constants sit in `constants`, while application entry is `ShortUrlApplication`. Static configuration such as application properties and migrations belong in `src/main/resources`. Keep automated tests under `src/test/java/com/nh/shorturl`, mirroring the main package layout; add resource fixtures to `src/test/resources`. Ancillary workflow files include `api-flow.http` for IntelliJ HTTP Client scenarios and `url_shortener_script.sh` for end-to-end shell testing.

## Build, Test, and Development Commands
Run `./gradlew clean build` to compile, execute tests, and assemble the shaded JAR into `build/libs/short-url-0.0.1-SNAPSHOT.jar`. Use `./gradlew bootRun` for a hot-reloading developer server on port 8080. Execute targeted unit and integration suites with `./gradlew test`. Regenerate dependency metadata or inspect tasks via `./gradlew tasks --group application`.

## Coding Style & Naming Conventions
This codebase targets Java 17 with Spring Boot 3; use four-space indentation and keep lines under 120 characters. Adhere to the package naming pattern `com.nh.shorturl.*` and descriptive class names like `ShortUrlController`. Favor Lombok annotations already in use (`@Getter`, `@Builder`) and prefer constructor injection for Spring components. Public REST endpoints should return the `ApiResult` wrapper and reside in `controller`; business logic belongs in dedicated service classes before reaching repositories.

## Testing Guidelines
JUnit 5 (Spring Boot starter) powers the test stack. Name test classes with the `*Test` suffix and place integration suites alongside their domain counterparts (`ShortUrlControllerTest` under the matching package). Mock external boundaries with Spring test slices (`@WebMvcTest`, `@DataJpaTest`) and assert request/response payloads with `MockMvc` or `TestRestTemplate`. Run `./gradlew test` locally before each push, aiming to cover URL generation, JWT/token flows, and repository queries that map native SQL projections.

## Commit & Pull Request Guidelines
Follow the existing conventional prefix plus subject format: `feat:`, `fix:`, `docs:`, `chore:`—note the lowercase keyword, colon, and a short imperative summary (`fix: align redirect stats query`). Group related changes per commit and reference issue IDs where available. Pull requests should include: goal summary, risk assessment, verification notes (commands run, screenshots for API clients), and links to related issues or API tickets.

## Security & Configuration Tips
Configure secrets through environment variables rather than committing them—set `JASYPT_ENCRYPTOR_PASSWORD` before running any Gradle task that reads encrypted properties. Avoid sharing generated JWTs in logs or documentation. When testing API flows, export temporary registration keys via your shell session (`export X_REGISTRATION_KEY=...`) and rotate them after demos.
