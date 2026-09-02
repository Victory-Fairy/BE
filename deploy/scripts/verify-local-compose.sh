#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
compose_file="$repo_root/compose.local.yaml"

docker compose -f "$compose_file" config --quiet
docker compose -f "$compose_file" config --services | grep -qx mysql
docker compose -f "$compose_file" config --services | grep -qx redis
docker compose -f "$compose_file" config | grep -q 'published: "3308"'
docker compose -f "$compose_file" config | grep -q 'published: "6380"'
