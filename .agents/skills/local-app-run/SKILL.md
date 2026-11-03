---
name: local-app-run
description: Start, check, smoke-test and stop the local jsonplaceholder-crud application from a shell (Cassandra via Docker Compose, app via scripts/dev.sh), and diagnose startup failures. Use when asked to run, launch, start, restart or stop the app, to verify a change in the running app, or when a launch fails.
---

# Running the Local Application

The application needs a local Cassandra (Docker Compose) and a JWT secret. `scripts/local-app.sh` handles both and runs the app in the background, so no IDE or debugger extension is required.

## Commands

| Command | Effect |
|---|---|
| `scripts/local-app.sh status` | Shows whether the app runs and answers on port 8080; also detects a foreign process on the port. |
| `scripts/local-app.sh start` | Starts Cassandra (`--wait` for healthy), applies `schema.cql`, starts the app via `scripts/dev.sh` and waits until `/v3/api-docs` answers. Exits non-zero with the last log lines on failure. |
| `scripts/local-app.sh stop` | Terminates the app's process group (Maven and the forked JVM). Cassandra keeps running. |
| `scripts/local-app.sh restart` | `stop` followed by `start`, e.g. after code changes. |
| `scripts/local-app.sh logs` | Prints the last 80 log lines (`LINES=200` for more); full log in `target/local-app/app.log`. |
| `scripts/smoke-test.sh [base-url]` | Exercises the running app over HTTP: 401 without token, register, login, CRUD, relation moves, PATCH validation, 404 and paging errors. |

## Workflow

1. **Check first.** Run `scripts/local-app.sh status`. If the port is used by another process (often the developer's IDE debug session), do not kill it; ask the user or test against that instance.
2. **Start.** Run `scripts/local-app.sh start` (allow up to about 4 minutes on a cold Maven build). Run it in the foreground; the script backgrounds the app itself.
3. **Verify.** Run `scripts/smoke-test.sh`, plus targeted `curl` calls for the change at hand.
4. **Stop when done.** Run `scripts/local-app.sh stop`, unless the user wants the app to keep running.

## Failure Handling

- After a failed start, read the reported log lines (or `scripts/local-app.sh logs`) and identify the cause before starting again. Do not loop on restarts.
- A startup timeout does not mean failure: check `status` once and read the log before deciding.
- Typical causes:

| Symptom | Likely cause | Fix |
|---|---|---|
| `port 8080 is used by another process` | IDE debug session or an earlier run | Ask the user to stop it, or use that instance |
| `AllNodesFailedException`, connection refused on 9042 | Cassandra not running or still starting | `docker compose up -d --wait cassandra` |
| Cassandra container `Exited (137)` | Out of memory (`docker inspect -f '{{.State.OOMKilled}}' jsonplaceholder-cassandra`) | Keep the heap limits in `compose.yaml`; avoid parallel Testcontainers runs |
| `JWT_SECRET must ...` | Invalid `JWT_SECRET` env var or `.jwt-secret` file | Unset the env var or delete `.jwt-secret` to regenerate |
| `COMPILATION ERROR` | Code does not compile | Fix the reported file and line |

## Notes

- `mvn test` starts its own Cassandra containers; this does not need the app or the Compose Cassandra.
- Every 401 that should be a server error is a bug: `/error` is permitted in the security configuration.
