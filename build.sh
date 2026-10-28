#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}"

echo "[build.sh] Building plugin with Gradle wrapper..."
./gradlew build "$@"

echo "[build.sh] Build successful! Output artifacts:"
ls -lh "${SCRIPT_DIR}/build/libs/"*.jar 2>/dev/null || true
