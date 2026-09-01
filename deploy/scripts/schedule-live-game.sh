#!/usr/bin/env bash
set -euo pipefail

action="${1:-}"
runtime_dir="${SYSTEMD_RUNTIME_DIR:-/run/systemd/system}"
timer_path="$runtime_dir/victoryfairy-live-game.timer"

schedule_next() {
    next_at="$(sed -n 's/^LIVE_GAME_NEXT_AT=//p' | tail -n 1)"
    if [[ -z "$next_at" ]]; then
        echo "Crawler did not report the next live-game run" >&2
        exit 1
    fi

    if [[ "$next_at" == "NONE" ]]; then
        if [[ "${LIVE_GAME_DRY_RUN:-0}" != "1" ]]; then
            systemctl stop victoryfairy-live-game.timer 2>/dev/null || true
        fi
        rm -f "$timer_path"
        if [[ "${LIVE_GAME_DRY_RUN:-0}" != "1" ]]; then
            systemctl daemon-reload
        fi
        return
    fi

    if [[ ! "$next_at" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}\ [0-9]{2}:[0-9]{2}:[0-9]{2}\ Asia/Seoul$ ]]; then
        echo "Invalid next live-game run: $next_at" >&2
        exit 1
    fi

    if [[ "${LIVE_GAME_DRY_RUN:-0}" != "1" ]]; then
        now_epoch="$(date +%s)"
        next_epoch="$(TZ=Asia/Seoul date -d "${next_at% Asia/Seoul}" +%s)"
        if (( next_epoch <= now_epoch )); then
            next_at="$(date -u -d "@$((now_epoch + 60))" '+%Y-%m-%d %H:%M:%S UTC')"
        fi
    fi

    mkdir -p "$runtime_dir"
    candidate="$(mktemp "$runtime_dir/.victoryfairy-live-game.timer.XXXXXX")"
    trap 'rm -f "$candidate"' EXIT
    printf '%s\n' \
        '[Unit]' \
        'Description=Run the next live KBO game sync' \
        '' \
        '[Timer]' \
        "OnCalendar=$next_at" \
        'Persistent=true' \
        'AccuracySec=1s' \
        'Unit=victoryfairy-live-game.service' \
        > "$candidate"
    install -m 0644 "$candidate" "$timer_path"

    if [[ "${LIVE_GAME_DRY_RUN:-0}" != "1" ]]; then
        systemctl daemon-reload
        systemctl restart victoryfairy-live-game.timer
    fi
}

run_crawler() {
    output="$(mktemp)"
    trap 'rm -f "$output"' EXIT
    mkdir -p /run/victoryfairy-game-sync
    /usr/bin/flock --wait 1800 /run/victoryfairy-game-sync/job.lock \
        /usr/bin/docker compose --env-file /opt/victoryfairy/.env \
        -f /opt/victoryfairy/compose.yaml --profile jobs run --rm craw \
        --spring.main.web-application-type=none --live-game.enabled=true \
        "--live-game.action=$action" | tee "$output"
    "$0" schedule < "$output"
}

case "$action" in
    plan|sync)
        run_crawler
        ;;
    schedule)
        schedule_next
        ;;
    *)
        echo "Usage: $0 plan|sync|schedule" >&2
        exit 2
        ;;
esac
