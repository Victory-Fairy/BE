#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"

APP_ENV_FILE="$repo_root/deploy/env.example" docker compose \
  --env-file "$repo_root/deploy/env.example" \
  -f "$repo_root/deploy/compose.yaml" \
  --profile jobs \
  config --quiet

grep -q 'proxy_pass http://api:8081' "$repo_root/deploy/nginx/victoryfairy.conf"
test "$(grep -c 'proxy_pass http://api:8081' "$repo_root/deploy/nginx/victoryfairy.conf")" -eq 6
test "$(grep -c 'proxy_pass http://admin:8084' "$repo_root/deploy/nginx/victoryfairy.conf")" -eq 0
grep -q 'SERVER_SERVLET_CONTEXT_PATH: /v2' "$repo_root/deploy/compose.yaml"
grep -q 'file:/config/admin/,file:/config/file/,file:/config/api/' "$repo_root/deploy/compose.yaml"
grep -q 'SPRINGDOC_API_DOCS_PATH: /api/v3/api-docs' "$repo_root/deploy/compose.yaml"
grep -q 'location /v2/api/swagger-ui/' "$repo_root/deploy/nginx/victoryfairy.conf"

live_service="$repo_root/deploy/systemd/victoryfairy-live-game.service"
planner_service="$repo_root/deploy/systemd/victoryfairy-live-game-planner.service"
planner_timer="$repo_root/deploy/systemd/victoryfairy-live-game-planner.timer"
schedule_script="$repo_root/deploy/scripts/schedule-live-game.sh"
test -f "$live_service"
test -f "$planner_service"
test -f "$planner_timer"
test -x "$schedule_script"
grep -q 'schedule-live-game.sh sync' "$live_service"
grep -q 'schedule-live-game.sh plan' "$planner_service"
grep -q 'pull api craw' "$repo_root/deploy/scripts/deploy.sh"
grep -q 'up -d --remove-orphans redis api nginx' "$repo_root/deploy/scripts/deploy.sh"
! grep -q 'file-latest' "$repo_root/deploy/compose.yaml"
! grep -q 'admin-latest' "$repo_root/deploy/compose.yaml"
! grep -q ':core:core-admin:bootJar' "$repo_root/.github/workflows/deploy-backend.yml"
! grep -q 'Build and push admin' "$repo_root/.github/workflows/deploy-backend.yml"
test -x "$repo_root/deploy/scripts/release-via-ssm.sh"
grep -q 'release-via-ssm.sh' "$repo_root/.github/workflows/deploy-backend.yml"
grep -q 'compose.next.yaml' "$repo_root/deploy/scripts/deploy.sh"
grep -q 'victoryfairy.next.conf' "$repo_root/deploy/scripts/deploy.sh"
grep -q -- '--add-host api:127.0.0.1' "$repo_root/deploy/scripts/deploy.sh"

runtime_dir="$(mktemp -d)"
trap 'rm -rf "$runtime_dir"' EXIT
printf '%s\n' 'LIVE_GAME_NEXT_AT=2026-09-04 18:20:00 Asia/Seoul' \
    | SYSTEMD_RUNTIME_DIR="$runtime_dir" LIVE_GAME_DRY_RUN=1 "$schedule_script" schedule
grep -q 'OnCalendar=2026-09-04 18:20:00 Asia/Seoul' "$runtime_dir/victoryfairy-live-game.timer"

printf '%s\n' 'LIVE_GAME_NEXT_AT=NONE' \
    | SYSTEMD_RUNTIME_DIR="$runtime_dir" LIVE_GAME_DRY_RUN=1 "$schedule_script" schedule
test ! -e "$runtime_dir/victoryfairy-live-game.timer"
