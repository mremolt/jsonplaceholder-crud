#!/usr/bin/env bash
# Starts, checks and stops the local application in the background.
# Used by coding agents and developers who need a running instance without an IDE.
set -euo pipefail

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
run_dir="$project_root/target/local-app"
pid_file="$run_dir/app.pid"
log_file="$run_dir/app.log"
port="${APP_PORT:-8080}"
base_url="http://localhost:$port"
startup_timeout="${APP_STARTUP_TIMEOUT:-240}"

usage() {
    printf 'Usage: %s start|stop|restart|status|logs\n' "$(basename "$0")" >&2
    exit 2
}

app_pid() {
    [[ -f "$pid_file" ]] && cat "$pid_file"
}

is_running() {
    local pid
    pid="$(app_pid)" || return 1
    kill -0 "$pid" 2>/dev/null
}

port_in_use() {
    (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null
}

is_ready() {
    [[ "$(curl -s -o /dev/null -w '%{http_code}' "$base_url/v3/api-docs")" == "200" ]]
}

start_cassandra() {
    printf 'Starting Cassandra and applying schema.cql ...\n'
    docker compose -f "$project_root/compose.yaml" up -d --wait cassandra >/dev/null
    docker compose -f "$project_root/compose.yaml" run --rm cassandra-schema >/dev/null
}

start() {
    if is_running; then
        printf 'Application already running (pid %s) at %s\n' "$(app_pid)" "$base_url"
        return 0
    fi
    if port_in_use; then
        printf 'Error: port %s is used by another process, e.g. an IDE debug session. Stop it first.\n' "$port" >&2
        return 1
    fi

    start_cassandra
    mkdir -p "$run_dir"
    # setsid gives the app its own process group, so stop can terminate Maven and the forked JVM together.
    setsid "$project_root/scripts/dev.sh" >"$log_file" 2>&1 </dev/null &
    echo $! >"$pid_file"
    printf 'Starting application (pid %s), log: %s\n' "$(app_pid)" "$log_file"

    local waited=0
    until is_ready; do
        if ! is_running; then
            printf 'Error: application exited during startup. Last log lines:\n' >&2
            tail -n 40 "$log_file" >&2
            rm -f "$pid_file"
            return 1
        fi
        if (( waited >= startup_timeout )); then
            printf 'Error: application not ready after %ss; it is still running. Inspect %s\n' \
                "$startup_timeout" "$log_file" >&2
            return 1
        fi
        sleep 2
        waited=$((waited + 2))
    done
    printf 'Application ready at %s after %ss\n' "$base_url" "$waited"
}

stop() {
    if ! is_running; then
        rm -f "$pid_file"
        printf 'Application is not running\n'
        return 0
    fi
    local pid
    pid="$(app_pid)"
    kill -TERM -- "-$pid" 2>/dev/null || true
    for _ in $(seq 1 30); do
        is_running || break
        sleep 1
    done
    if is_running; then
        kill -KILL -- "-$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
    printf 'Application stopped\n'
}

status() {
    if is_running && is_ready; then
        printf 'running and ready (pid %s) at %s\n' "$(app_pid)" "$base_url"
    elif is_running; then
        printf 'running but not ready (pid %s)\n' "$(app_pid)"
        return 1
    elif port_in_use; then
        printf 'not started by this script, but port %s is in use by another process\n' "$port"
        return 1
    else
        printf 'not running\n'
        return 1
    fi
}

case "${1:-}" in
    start) start ;;
    stop) stop ;;
    restart) stop && start ;;
    status) status ;;
    logs) tail -n "${LINES:-80}" "$log_file" ;;
    *) usage ;;
esac
