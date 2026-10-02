#!/usr/bin/env bash
# Syncs static/css/theme.css from the template into every service, or checks for drift.
# Usage:
#   ./scripts/sync-theme.sh          copy the template's theme.css into every service
#   ./scripts/sync-theme.sh --check  exit 1 if any service's theme.css differs (for CI/pre-commit)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REL="src/main/resources/static/css/theme.css"
SOURCE="$ROOT/trackph-microservice-template/$REL"

services=()
for dir in "$ROOT"/*/; do
    name="$(basename "$dir")"
    [ "$name" = "trackph-microservice-template" ] && continue
    [ -f "$dir$REL" ] && services+=("$name")
done

if [ "${1:-}" = "--check" ]; then
    status=0
    for service in "${services[@]}"; do
        if ! diff -q "$SOURCE" "$ROOT/$service/$REL" > /dev/null; then
            echo "DRIFT: $service/$REL differs from the template"
            status=1
        fi
    done
    [ $status -eq 0 ] && echo "All services match the template's theme.css"
    exit $status
else
    for service in "${services[@]}"; do
        cp "$SOURCE" "$ROOT/$service/$REL"
        echo "Synced $service/$REL"
    done
fi
