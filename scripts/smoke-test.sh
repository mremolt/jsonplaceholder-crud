#!/usr/bin/env bash
# Exercises a running application over HTTP: auth, CRUD, relations and error handling.
# Usage: scripts/smoke-test.sh [base-url]   (default http://localhost:8080)
set -euo pipefail

base_url="${1:-http://localhost:8080}"
failures=0
# Like a BFF, send the client address; each run gets its own auth rate-limit bucket.
client_address="198.18.$((RANDOM % 256)).$((RANDOM % 256))"
response_file="$(mktemp)"
trap 'rm -f "$response_file"' EXIT

# request METHOD PATH [BODY] - stores the body in $response_file and echoes the status code
request() {
    local method="$1" path="$2" body="${3:-}"
    local args=(-s -o "$response_file" -w '%{http_code}' -X "$method" "$base_url$path"
        -H 'Content-Type: application/json' -H "X-Forwarded-For: $client_address")
    [[ -n "${token:-}" ]] && args+=(-H "Authorization: Bearer $token")
    [[ -n "$body" ]] && args+=(-d "$body")
    curl "${args[@]}"
}

check() {
    local description="$1" expected="$2" actual="$3"
    if [[ "$expected" == "$actual" ]]; then
        printf '  ok    %s\n' "$description"
    else
        printf '  FAIL  %s: expected %s, got %s\n' "$description" "$expected" "$actual"
        sed 's/^/        /' "$response_file" | head -c 600
        printf '\n'
        failures=$((failures + 1))
    fi
}

json() {
    jq -r "$1" "$response_file"
}

printf 'Smoke test against %s\n' "$base_url"
if [[ "$(curl -s -o /dev/null -w '%{http_code}' "$base_url/v3/api-docs")" == "000" ]]; then
    printf 'Application not reachable at %s; start it with scripts/local-app.sh start\n' "$base_url"
    exit 1
fi
email="smoke-$(date +%s)-$RANDOM@example.com"
password="Smoke-test-password-42"
credentials="{\"email\":\"$email\",\"password\":\"$password\"}"

token=""
check "protected endpoint without token" 401 "$(request GET /posts)"
check "register" 201 "$(request POST /auth/register "$credentials")"
check "login" 200 "$(request POST /auth/login "$credentials")"
token="$(json .accessToken)"
refresh_token="$(json .refreshToken)"
token=""
check "refresh rotates tokens" 200 "$(request POST /auth/refresh "{\"refreshToken\":\"$refresh_token\"}")"
check "reused refresh token rejected" 401 "$(request POST /auth/refresh "{\"refreshToken\":\"$refresh_token\"}")"
check "login again" 200 "$(request POST /auth/login "$credentials")"
token="$(json .accessToken)"
logout_token="$(json .refreshToken)"

user_id=$((900000000 + RANDOM))
check "create post" 201 "$(request POST /posts "{\"userId\":$user_id,\"title\":\"Smoke\",\"body\":\"Body\"}")"
post_id="$(json .id)"
check "read post" 200 "$(request GET "/posts/$post_id")"
check "post listed for user" "$post_id" "$(request GET "/posts?userId=$user_id" >/dev/null; json '.[0].id')"
check "patch post" 200 "$(request PATCH "/posts/$post_id" '{"title":"Patched"}')"
check "patch kept body" "Body" "$(json .body)"
check "invalid patch rejected" 400 "$(request PATCH "/posts/$post_id" '{"title":" "}')"
check "merge patch null removes required field" 400 "$(request PATCH "/posts/$post_id" '{"body":null}')"
check "merge patch media type accepted" 200 "$(curl -s -o "$response_file" -w '%{http_code}' -X PATCH \
    "$base_url/posts/$post_id" -H 'Content-Type: application/merge-patch+json' \
    -H "Authorization: Bearer $token" -d '{"title":"Merged"}')"
check "merge patch applied" "Merged" "$(json .title)"
check "replace post" 200 "$(request PUT "/posts/$post_id" "{\"userId\":$((user_id + 1)),\"title\":\"Put\",\"body\":\"New\"}")"
check "relation moved to new user" "$post_id" "$(request GET "/posts?userId=$((user_id + 1))" >/dev/null; json '.[0].id')"
check "relation removed from old user" 0 "$(request GET "/posts?userId=$user_id" >/dev/null; json 'length')"
check "delete post" 200 "$(request DELETE "/posts/$post_id")"
check "deleted post not found" 404 "$(request GET "/posts/$post_id")"
check "invalid paging rejected" 400 "$(request GET '/posts?_limit=0')"
check "resource index" 200 "$(request GET /)"
check "keyset paging" 200 "$(request GET '/posts?_limit=1&_after=1')"
check "logout" 204 "$(request POST /auth/logout "{\"refreshToken\":\"$logout_token\"}")"
check "refresh after logout rejected" 401 "$(request POST /auth/refresh "{\"refreshToken\":\"$logout_token\"}")"

if (( failures > 0 )); then
    printf 'Smoke test FAILED (%s checks)\n' "$failures"
    exit 1
fi
printf 'Smoke test passed\n'
