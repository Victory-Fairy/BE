#!/usr/bin/env bash
set -euo pipefail

readonly runtime_dir=/opt/victoryfairy
readonly env_file="$runtime_dir/.env"
readonly compose_file="$runtime_dir/compose.yaml"
readonly aws_region=ap-northeast-2
readonly image_tag="${1:-}"

if [[ ! $image_tag =~ ^[0-9a-f]{40}$ ]]; then
  echo "usage: deploy.sh <40-character-git-sha>" >&2
  exit 64
fi

if [[ $EUID -ne 0 ]]; then
  echo "deploy.sh must run as root" >&2
  exit 77
fi

ecr_registry="$(sed -n 's/^ECR_REGISTRY=//p' "$env_file" | head -n 1)"
if [[ ! $ecr_registry =~ ^[0-9]{12}\.dkr\.ecr\.ap-northeast-2\.amazonaws\.com$ ]]; then
  echo "invalid ECR_REGISTRY in $env_file" >&2
  exit 78
fi

aws ecr get-login-password --region "$aws_region" \
  | docker login --username AWS --password-stdin "$ecr_registry"

previous_env="$(mktemp)"
candidate_env="$(mktemp)"
trap 'rm -f "$previous_env" "$candidate_env"' EXIT
cp "$env_file" "$previous_env"

awk -v tag="$image_tag" '
  BEGIN { replaced = 0 }
  /^IMAGE_TAG=/ { print "IMAGE_TAG=" tag; replaced = 1; next }
  { print }
  END { if (!replaced) print "IMAGE_TAG=" tag }
' "$env_file" > "$candidate_env"

APP_ENV_FILE="$env_file" docker compose --env-file "$candidate_env" -f "$compose_file" config --quiet
APP_ENV_FILE="$env_file" docker compose --env-file "$candidate_env" -f "$compose_file" pull api file admin
install -o root -g root -m 0600 "$candidate_env" "$env_file"

if ! docker compose --env-file "$env_file" -f "$compose_file" up -d --remove-orphans redis api file admin nginx; then
  install -o root -g root -m 0600 "$previous_env" "$env_file"
  docker compose --env-file "$env_file" -f "$compose_file" up -d --remove-orphans redis api file admin nginx || true
  docker compose --env-file "$env_file" -f "$compose_file" restart nginx || true
  exit 1
fi

docker compose --env-file "$env_file" -f "$compose_file" restart nginx
docker compose --env-file "$env_file" -f "$compose_file" ps
