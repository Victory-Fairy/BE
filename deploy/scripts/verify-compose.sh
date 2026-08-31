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
grep -q 'pull api file admin craw' "$repo_root/deploy/scripts/deploy.sh"

runtime_dir="$(mktemp -d)"
trap 'rm -rf "$runtime_dir"' EXIT
printf '%s\n' 'LIVE_GAME_NEXT_AT=2026-09-04 18:20:00 Asia/Seoul' \
    | SYSTEMD_RUNTIME_DIR="$runtime_dir" LIVE_GAME_DRY_RUN=1 "$schedule_script" schedule
grep -q 'OnCalendar=2026-09-04 18:20:00 Asia/Seoul' "$runtime_dir/victoryfairy-live-game.timer"

printf '%s\n' 'LIVE_GAME_NEXT_AT=NONE' \
    | SYSTEMD_RUNTIME_DIR="$runtime_dir" LIVE_GAME_DRY_RUN=1 "$schedule_script" schedule
test ! -e "$runtime_dir/victoryfairy-live-game.timer"
