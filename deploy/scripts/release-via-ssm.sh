#!/usr/bin/env bash
set -euo pipefail

readonly instance_id="${1:-}"
readonly image_tag="${2:-}"

if [[ ! $instance_id =~ ^i-[0-9a-f]+$ ]] || [[ ! $image_tag =~ ^[0-9a-f]{40}$ ]]; then
  echo "usage: release-via-ssm.sh <ec2-instance-id> <40-character-git-sha>" >&2
  exit 64
fi

encode() {
  base64 < "$1" | tr -d '\n'
}

compose_payload="$(encode deploy/compose.yaml)"
nginx_payload="$(encode deploy/nginx/victoryfairy.conf)"
script_payload="$(encode deploy/scripts/deploy.sh)"

remote_command="$(printf '%s\n' \
  'set -eu' \
  'install -d -m 0755 /opt/victoryfairy/bin /opt/victoryfairy/nginx' \
  "printf '%s' '$compose_payload' | base64 -d > /opt/victoryfairy/compose.next.yaml" \
  "printf '%s' '$nginx_payload' | base64 -d > /opt/victoryfairy/nginx/victoryfairy.next.conf" \
  "printf '%s' '$script_payload' | base64 -d > /opt/victoryfairy/bin/deploy.next" \
  'chmod 0755 /opt/victoryfairy/bin/deploy.next' \
  "/opt/victoryfairy/bin/deploy.next '$image_tag'")"
parameters="$(jq -cn --arg command "$remote_command" '{commands: [$command]}')"

command_id="$(aws ssm send-command \
  --instance-ids "$instance_id" \
  --document-name AWS-RunShellScript \
  --timeout-seconds 900 \
  --parameters "$parameters" \
  --query 'Command.CommandId' \
  --output text)"

for _ in {1..180}; do
  status="$(aws ssm get-command-invocation \
    --command-id "$command_id" \
    --instance-id "$instance_id" \
    --query 'Status' \
    --output text 2>/dev/null || true)"

  case "$status" in
    Success)
      aws ssm get-command-invocation --command-id "$command_id" --instance-id "$instance_id" \
        --query 'StandardOutputContent' --output text
      exit 0
      ;;
    Pending|InProgress|Delayed|"") sleep 5 ;;
    *)
      aws ssm get-command-invocation --command-id "$command_id" --instance-id "$instance_id" \
        --query '[StandardOutputContent,StandardErrorContent]' --output text
      exit 1
      ;;
  esac
done

echo "SSM deployment timed out" >&2
exit 1
