#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

for service in api craw; do
  jar_file="core/core-${service}/build/libs/core-${service}.jar"
  test -f "$jar_file"
  mkdir -p "build/docker/${service}"
  cp "$jar_file" "build/docker/${service}/app.jar"
done
