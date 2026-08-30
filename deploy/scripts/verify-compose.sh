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
live_timer="$repo_root/deploy/systemd/victoryfairy-live-game.timer"
test -f "$live_service"
test -f "$live_timer"
grep -q -- '--live-game.enabled=true' "$live_service"
grep -q '/run/victoryfairy-game-sync/job.lock' "$live_service"
grep -q 'OnCalendar=\*:0/10' "$live_timer"
grep -q 'pull api file admin craw' "$repo_root/deploy/scripts/deploy.sh"
