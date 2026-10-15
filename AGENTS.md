# AGENTS.md

Guidance for AI coding agents (and humans) working in this repository.

## Project overview

`hmpps-audit-api` is a Kotlin/Spring Boot service that listens for `AuditEvent`
messages on an SQS queue, stores them in a Postgres database, and exposes
REST endpoints to add and query audit events. It is part of the MoJ HMPPS
estate and uses standard HMPPS Spring Boot tooling (`hmpps-kotlin-spring-boot-starter`,
`hmpps-sqs-spring-boot-starter`).

## Tech stack

- JAVA 25
- Kotlin (JVM toolchain 25), Spring Boot
- Spring Data JPA + Flyway migrations (Postgres in prod, H2 in tests)
- Spring Security with OAuth2 resource server
- AWS SQS (via LocalStack locally), S3, Athena
- Gradle (Kotlin DSL) build, `./gradlew` wrapper
- ktlint for Kotlin code style

## Repository layout

- `src/main/kotlin/uk/gov/justice/digital/hmpps/hmppsauditapi/` — application code
  - `config/` — Spring configuration
  - `health/` — health indicators
  - `jpa/` + `jpa/model/` — JPA entities/repositories
  - `model/` — domain models
  - `resource/` + `resource/model/`, `resource/swagger/` — REST controllers and DTOs
  - `exception/` — exception handling
  - `listeners/` + `listeners/model/` — SQS message listeners
  - `services/` — business logic
- `src/main/resources/db/` — Flyway migrations (`audit`, `audit_h2`, `audit_postgres`)
- `src/test/kotlin/...` — tests mirroring the main package structure
- `readme/` — detailed docs (build/test/run, maintenance, inserting data, queue admin)
- `helm_deploy/` — Helm chart for deployment
- `.github/workflows/` — CI pipelines (build/test, security scans, deploy)

## Build, test, and run

Build without tests:
```bash
./gradlew clean build -x test
```

Run the test suite (requires LocalStack running; tests use H2 for the database):
```bash
TMPDIR=/private$TMPDIR docker compose up localstack
./gradlew test
```

Run a single test class/method:
```bash
./gradlew test --tests "uk.gov.justice.digital.hmpps.hmppsauditapi.SomeClassTest"
```

Ktlint Gradle tasks are available, but do not run them in this repository.
Skip ktlint validation and formatting entirely.

Run the app locally (dependencies via docker compose, app via Gradle):
```bash
TMPDIR=/private$TMPDIR docker compose up --scale hmpps-audit-api=0
./gradlew bootRun --args='--spring.profiles.active=dev,localstack'
```

See `readme/build_test_run.md`, `readme/maintenance.md`, `readme/inserting_data.md`,
and `readme/queue_admin.md` for more detail.

## Conventions

- Follow existing package structure and naming conventions (`hmppsauditapi` under
  `uk.gov.justice.digital.hmpps`).
- Follow the existing Kotlin formatting conventions; do not run ktlint Gradle tasks.
- Add Flyway migrations under `src/main/resources/db/` rather than editing existing ones.
- Mirror new test files under `src/test/kotlin/...` matching the main package path.
- Prefer the smallest Gradle command that validates a change (e.g. a targeted `--tests`
  filter) over running the full suite when iterating.

## PRs and commits

- Keep changes focused and avoid unrelated refactors.
- Update relevant docs in `readme/` when behaviour, build, or run steps change.

## Dependency injection

Prefer constructor injection over field injection in Spring components, including test classes.

- In production code, declare dependencies as constructor parameters (e.g. `private val fooRepository: FooRepository`) rather than using `@Autowired` on a field.
- In integration test classes, pass `@Autowired` repositories/beans as primary constructor parameters instead of using `@Autowired` on a `lateinit var` field.

## Tests

Prefer `@BeforeAll`/`@AfterAll` over `@BeforeEach`/`@AfterEach` where possible in nested integration test classes.

- Use `@TestInstance(TestInstance.Lifecycle.PER_CLASS)` on the nested test class so that `@BeforeAll`/`@AfterAll` can be non-static (instance) methods.
- Build shared fixture data once in `@BeforeAll` and clean it up once in `@AfterAll`, rather than rebuilding it before every test, when the fixture is not mutated in a way that would leak between tests.
- When copying or adapting an integration test, preserve the full nested test structure, including `Security`, `Validation`, and `HappyPath` scenarios for each endpoint.
- Place each `@AfterAll` method immediately after the corresponding `@BeforeAll` setup method.
- OAuth2 propagation tests must verify the exact WireMock header value with `equalTo("Bearer ABCDE")`; do not weaken this assertion to a regex or non-empty-header matcher.

## Linting

Do not run `ktlintCheck` (or any ktlint gradle task) in this repository. Skip ktlint validation entirely.
