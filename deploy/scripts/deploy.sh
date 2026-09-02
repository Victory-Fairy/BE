#!/usr/bin/env bash
set -euo pipefail

readonly runtime_dir=/opt/victoryfairy
readonly env_file="$runtime_dir/.env"
readonly compose_file="$runtime_dir/compose.yaml"
readonly nginx_file="$runtime_dir/nginx/victoryfairy.conf"
readonly metrics_auth_file="$runtime_dir/nginx/metrics.htpasswd"
readonly installed_script="$runtime_dir/bin/deploy.sh"
readonly candidate_compose="$runtime_dir/compose.next.yaml"
readonly candidate_nginx="$runtime_dir/nginx/victoryfairy.next.conf"
readonly candidate_script="$runtime_dir/bin/deploy.next"
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

for candidate in "$candidate_compose" "$candidate_nginx" "$candidate_script"; do
  if [[ ! -f $candidate ]]; then
    echo "missing release asset: $candidate" >&2
    exit 66
  fi
done

if [[ ! -s $metrics_auth_file ]]; then
  echo "missing metrics authentication file: $metrics_auth_file" >&2
  exit 66
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
previous_compose="$(mktemp)"
previous_nginx="$(mktemp)"
previous_script="$(mktemp)"
trap 'rm -f "$previous_env" "$candidate_env" "$previous_compose" "$previous_nginx" "$previous_script"' EXIT
cp "$env_file" "$previous_env"
cp "$compose_file" "$previous_compose"
cp "$nginx_file" "$previous_nginx"
cp "$installed_script" "$previous_script"

awk -v tag="$image_tag" '
  BEGIN { replaced = 0 }
  /^IMAGE_TAG=/ { print "IMAGE_TAG=" tag; replaced = 1; next }
  { print }
  END { if (!replaced) print "IMAGE_TAG=" tag }
' "$env_file" > "$candidate_env"

APP_ENV_FILE="$env_file" docker compose --env-file "$candidate_env" -f "$candidate_compose" config --quiet
APP_ENV_FILE="$env_file" docker compose --env-file "$candidate_env" -f "$candidate_compose" pull api craw node-exporter
docker run --rm \
  --add-host api:127.0.0.1 \
  -v "$candidate_nginx:/etc/nginx/conf.d/default.conf:ro" \
  -v "$metrics_auth_file:/etc/nginx/metrics.htpasswd:ro" \
  -v "$runtime_dir/letsencrypt:/etc/letsencrypt:ro" \
  nginx:1.27-alpine nginx -t

if ! (
  set -e
  install -o root -g root -m 0644 "$candidate_compose" "$compose_file"
  install -o root -g root -m 0644 "$candidate_nginx" "$nginx_file"
  install -o root -g root -m 0755 "$candidate_script" "$installed_script"
  install -o root -g root -m 0600 "$candidate_env" "$env_file"
  docker compose --env-file "$env_file" -f "$compose_file" up -d --remove-orphans redis api nginx node-exporter
  docker compose --env-file "$env_file" -f "$compose_file" restart nginx
); then
  install -o root -g root -m 0600 "$previous_env" "$env_file"
  install -o root -g root -m 0644 "$previous_compose" "$compose_file"
  install -o root -g root -m 0644 "$previous_nginx" "$nginx_file"
  install -o root -g root -m 0755 "$previous_script" "$installed_script"
  docker compose --env-file "$env_file" -f "$compose_file" up -d --remove-orphans redis api nginx node-exporter || true
  docker compose --env-file "$env_file" -f "$compose_file" restart nginx || true
  exit 1
fi

rm -f "$candidate_compose" "$candidate_nginx" "$candidate_script"
docker compose --env-file "$env_file" -f "$compose_file" ps
