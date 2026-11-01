#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root"

if [[ -z "${JWT_SECRET:-}" ]]; then
    secret_file="$project_root/.jwt-secret"
    if [[ -s "$secret_file" ]]; then
        JWT_SECRET="$(<"$secret_file")"
    else
        if ! command -v openssl >/dev/null 2>&1; then
            printf 'Error: openssl is required to generate a local JWT_SECRET.\n' >&2
            exit 1
        fi
        JWT_SECRET="$(openssl rand -base64 32)"
        (umask 077; printf '%s\n' "$JWT_SECRET" > "$secret_file")
        printf 'Generated a local JWT_SECRET in .jwt-secret. The value is not committed.\n'
    fi
    export JWT_SECRET
fi

if [[ -f "$project_root/.jwt-secret" ]]; then
    chmod 600 "$project_root/.jwt-secret"
fi

exec "$project_root/mvnw" spring-boot:run "$@"
