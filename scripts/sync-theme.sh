#!/usr/bin/env bash
# Syncs the shared UI files from the template into every service, or checks for drift.
# Usage:
#   ./scripts/sync-theme.sh          copy the template's shared UI files into every service
#   ./scripts/sync-theme.sh --check  exit 1 if any service has drifted (for CI/pre-commit)
#
# Two kinds of check:
#   - theme.css and theme-toggle.js must be byte-identical across every service (no
#     legitimate reason to diverge), so these are synced/diffed directly.
#   - layout.html is allowed to diverge (a service may add its own global script, e.g.
#     milestone-service's alert-badge poller), so it is NOT byte-synced. Instead this
#     script checks that it still includes fragments/nav :: links and still declares the
#     layout(pageTitle, content) fragment — i.e. hasn't regressed to hardcoded nav links
#     or lost the fragment definition. Each service owns its own fragments/nav.html and
#     config/BrandConfig.java — see dev-guide.md "Theme & UI".
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE="$ROOT/trackph-microservice-template"

SYNCED_FILES=(
    "src/main/resources/static/css/theme.css"
    "src/main/resources/static/js/theme-toggle.js"
)
LAYOUT_REL="src/main/resources/templates/layout.html"
NAV_REL="src/main/resources/templates/fragments/nav.html"

services=()
for dir in "$ROOT"/*/; do
    name="$(basename "$dir")"
    [ "$name" = "trackph-microservice-template" ] && continue
    [ -f "$dir$LAYOUT_REL" ] && services+=("$name")
done

if [ "${1:-}" = "--check" ]; then
    status=0
    for service in "${services[@]}"; do
        for rel in "${SYNCED_FILES[@]}"; do
            if ! diff -q "$TEMPLATE/$rel" "$ROOT/$service/$rel" > /dev/null 2>&1; then
                echo "DRIFT: $service/$rel differs from the template"
                status=1
            fi
        done

        layout="$ROOT/$service/$LAYOUT_REL"
        if ! grep -q 'fragments/nav :: links' "$layout" 2>/dev/null; then
            echo "DRIFT: $service/$LAYOUT_REL no longer includes fragments/nav :: links (hardcoded nav links?)"
            status=1
        fi
        if ! grep -q 'th:fragment="layout(pageTitle, content)"' "$layout" 2>/dev/null; then
            echo "DRIFT: $service/$LAYOUT_REL is missing the layout(pageTitle, content) fragment declaration"
            status=1
        fi
        if [ ! -f "$ROOT/$service/$NAV_REL" ]; then
            echo "DRIFT: $service/$NAV_REL is missing"
            status=1
        fi
    done
    [ $status -eq 0 ] && echo "All services match the template's shared UI files"
    exit $status
else
    for service in "${services[@]}"; do
        for rel in "${SYNCED_FILES[@]}"; do
            cp "$TEMPLATE/$rel" "$ROOT/$service/$rel"
            echo "Synced $service/$rel"
        done
    done
    echo "Note: layout.html and fragments/nav.html are NOT auto-synced (each service owns its nav/branding)."
    echo "Run './scripts/sync-theme.sh --check' to verify they still follow the shared structure."
fi
