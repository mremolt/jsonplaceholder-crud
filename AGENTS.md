# Agent Instructions

## Spring Boot Work

For Spring MVC, service, persistence, API, or architecture work, read the workspace Spring Boot patterns skill before making changes:

- `.agents/skills/springboot-patterns/SKILL.md`

For feature work, bug fixes, or refactoring, also read the Spring Boot TDD skill and add or update focused tests:

- `.agents/skills/springboot-tdd/SKILL.md`

For authentication, authorization, input validation, CORS/CSRF, secrets, or security headers, read the Spring Boot security skill before making changes:

- `.agents/skills/springboot-security/SKILL.md`

## Running the Application

When asked to run, start, restart, or stop the application, to verify a change in the running app, or to diagnose a launch failure, read and follow the local app skill first:

- `.agents/skills/local-app-run/SKILL.md`

Use `scripts/local-app.sh` to start and stop the app and `scripts/smoke-test.sh` to verify it. Check `scripts/local-app.sh status` before starting, and do not stop processes the script did not start. After a launch failure, inspect the captured output and do not retry until the cause is understood and addressed.

## Architecture

Production code lives under `src/main/java/gmbh/tantive/jsonplaceholder_crud/`, with the application entry point in the root package.

| Package | Contains | Must not contain |
|---|---|---|
| Feature packages (`post`, `comment`, `album`, `photo`, `todo`, `user`) | Controller, `…Request`/`…Response` records, storage model, service interface and its `Default…Service` implementation | Cassandra types, HTTP concerns in services |
| `api` | Generic `ResourceController`, `JsonMergePatcher`, `ApiExceptionHandler`, resource index | Resource-specific logic |
| `shared` | Contracts and generic logic: `ResourceCrudService`, `ResourceBulkService`, `ResourceStore` port, `Relationship`, `AbstractResourceService`, domain exceptions | Spring Web or Cassandra types |
| `persistence` | `CassandraResourceStore`, Cassandra entities and repositories | Knowledge of individual resources or their fields |
| `auth`, `importer`, `logging`, `dev` | Authentication, JSONPlaceholder import and relation backfill, access log, local launcher | |

Request flow: controller (request record, validation, JSON Merge Patch) → service (ids, relations, paging; works on the model) → `ResourceStore` port → `CassandraResourceStore`.

## Design Principles

All new and changed code must follow these principles. Reviews check them explicitly; when existing code violates one, fix it in the same change or call it out.

- **Single responsibility.** One reason to change per class. Controllers map HTTP and records, services hold resource rules, the store does I/O. Split a class when it starts mixing these, as was done with `ResourceStore` and `JsonMergePatcher`.
- **Open/closed.** Adding a resource must not require edits outside its feature package. Components that work on all resources (importer, backfill, resource index) discover them as `ResourceBulkService` beans (ordered with `@Order`); never add hard-coded resource lists. `persistence` stays generic and never learns resource fields.
- **Liskov substitution.** Subclasses of `AbstractResourceService` and `ResourceController` only fill in the documented hooks (`relationReferences`, mapping methods) and keep the base contract, e.g. ids are assigned by the base class.
- **Interface segregation.** Clients depend on the narrowest interface: controllers on the resource service interface (CRUD and queries), bulk jobs on `ResourceBulkService`. Do not add import or maintenance operations to API-facing interfaces.
- **Dependency inversion.** Business code depends on ports in `shared`, not on Cassandra. New infrastructure gets a port in `shared` and an adapter in its own package.
- **Constructor injection only**, with `final` fields; Lombok `@RequiredArgsConstructor` is fine when the constructor only assigns fields.
- **Separate API and storage models.** The API speaks records (`…Request` without id and with Bean Validation, `…Response` with id); services and storage use the model classes. Never expose a storage model in a controller signature. Renaming a stored model field requires a data migration; renaming a record field is an API change.
- **Domain exceptions, mapped once.** Services throw domain exceptions (`ResourceNotFoundException`, `InvalidPageException`, …); `ApiExceptionHandler` maps them to `ProblemDetail`. Do not throw `ResponseStatusException` from services or `shared`. (The `auth` package still uses `ResponseStatusException`; migrate it when you touch it.)
- **Typed values over strings.** Relation names live in the `Relationship` enum; add new ones there. Use enums or value objects instead of string concatenation for keys and identifiers.
- **Lombok for boilerplate only.** `@Getter`/`@Setter` on mutable models, `@RequiredArgsConstructor` for injection. No `@Data` on entities or classes with secrets; prefer records for immutable data.

## API Conventions

- POST and PUT take the complete request record and are validated with `@Valid`.
- PATCH is a JSON Merge Patch (RFC 7396) via `JsonMergePatcher`: fields missing in the patch stay unchanged, `null` removes a field, nested objects merge; the result is validated like a PUT body. Accept `application/merge-patch+json` and `application/json`.
- Errors are `application/problem+json`; validation errors list the fields in `errors`. Database outages return 503, internal errors 500, never 401.
- All endpoints except `POST /auth/register`, `POST /auth/login`, `/error` and the OpenAPI docs require a bearer token. `EndpointSecurityIntegrationTest` enforces this for every new endpoint automatically.

## Persistence Conventions

- Keep Cassandra reads query-driven and bounded; every port method takes a limit.
- Writes that touch a resource and its relations go through one logged batch (`ResourceStore.save`/`delete`). Bulk imports use `saveAll` and stay unbatched.
- Update `schema.cql` for every schema change, keep it idempotent (`IF [NOT] EXISTS`), and document the migration and backfill path for existing data in the script. `spring.cassandra.schema-action` stays `none`.

## Testing

Follow the TDD skill: write the failing test first. Choose the cheapest test that proves the behavior:

| Test type | Use for | Setup |
|---|---|---|
| Unit test | Service rules, mapping, merge patch, helpers | Plain JUnit with `InMemoryResourceStore` or `RecordingBulkService` fakes |
| Web layer test | Request mapping, validation, status codes, JSON shape | `@ApiWebMvcTest(MyController.class)` (production security chain, authenticated by default) with `@MockitoBean` services |
| Integration test | Cassandra behavior, security, OpenAPI, end-to-end flows | `@IntegrationTest` (one shared context and Cassandra container) |

- Assert outcomes (returned values, stored state, HTTP responses), not interactions. Use Mockito `verify` only when the call itself is the observable result, e.g. HTTP requests sent by the importer.
- Every test owns its data: unique resource names, ids or emails instead of shared magic values. Tests sending more than a few auth requests use their own client address because the rate limiter is shared.
- Avoid locale-dependent assertions (validation messages are localized).
- Put tests under the matching feature package in `src/test/java`. Run `mvn test` after changes; it also writes the JaCoCo report to `target/site/jacoco/`. New code should not lower coverage.
- After changes to runtime behavior, also verify the running app with `scripts/local-app.sh start` and `scripts/smoke-test.sh` (see the local app skill).
