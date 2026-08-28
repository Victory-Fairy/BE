#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"

APP_ENV_FILE="$repo_root/deploy/env.example" docker compose \
  --env-file "$repo_root/deploy/env.example" \
  -f "$repo_root/deploy/compose.yaml" \
  --profile jobs \
  config --quiet

grep -q 'proxy_pass http://api:8081' "$repo_root/deploy/nginx/victoryfairy.conf"
grep -q 'proxy_pass http://file:8082' "$repo_root/deploy/nginx/victoryfairy.conf"
grep -q 'proxy_pass http://admin:8084' "$repo_root/deploy/nginx/victoryfairy.conf"
