#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../.."

stage_jar() {
  local module="$1"
  local context="$2"
  local jar_file="applications/${module}/build/libs/${module}.jar"

  test -f "$jar_file"
  mkdir -p "build/docker/${context}"
  cp "$jar_file" "build/docker/${context}/app.jar"
}

stage_jar api api
stage_jar crawler craw
