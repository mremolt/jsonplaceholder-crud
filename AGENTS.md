# Agent Instructions

## Spring Boot Work

For Spring MVC, service, persistence, API, or architecture work, read the workspace Spring Boot patterns skill before making changes:

- `.agents/skills/springboot-patterns/SKILL.md`

For feature work, bug fixes, or refactoring, also read the Spring Boot TDD skill and add or update focused tests:

- `.agents/skills/springboot-tdd/SKILL.md`

For authentication, authorization, input validation, CORS/CSRF, secrets, or security headers, read the Spring Boot security skill before making changes:

- `.agents/skills/springboot-security/SKILL.md`

## Java Launching

When asked to run, launch, start, stop, or debug this Java application, or diagnose a launch failure, read and follow the Java launch troubleshooting skill first:

- `.agents/skills/java-launch-troubleshooting/SKILL.md`

Use the Java debug tools for Java launches. Check for an existing session before starting another. After a launch failure, inspect the captured output and do not retry until the cause is understood and addressed.

## Project Conventions

- Keep the application entry point in the root package and group production code by resource under `src/main/java/gmbh/tantive/jsonplaceholder_crud/`.
- Keep controllers thin, use constructor injection, and keep each resource's controller, model, and service in its feature package.
- Keep shared contracts in `shared`, Cassandra mappings and repositories in `persistence`, and external import code in `importer`.
- Preserve Jakarta Bean Validation on request models; validate complete POST/PUT payloads while keeping PATCH partial.
- Keep Cassandra reads query-driven and bounded. Update `schema.cql` and provide an explicit, safe backfill path when a schema change affects existing data.
- Put tests under matching feature packages in `src/test/java`; run `mvn test` after changes.
