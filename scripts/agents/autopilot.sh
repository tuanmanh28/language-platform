#!/usr/bin/env bash
# One command for the whole backlog: agents implement, verify, review, merge and push until everything is done.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

if [[ "$(git rev-parse --abbrev-ref HEAD)" != "main" ]]; then
  echo "Check out main first." >&2
  exit 1
fi

args=(scripts/agents/orchestrator.py run --auto --push --parallel "${PARALLEL:-2}" "$@")
if command -v caffeinate >/dev/null; then
  exec caffeinate -i python3 "${args[@]}"
fi
exec python3 "${args[@]}"
