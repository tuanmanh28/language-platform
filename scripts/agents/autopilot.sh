#!/usr/bin/env bash
# One command for the whole backlog: agents implement, verify and get reviewed; every task then goes through a pull
# request and is squash-merged on GitHub only after CI is green.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

if [[ "$(git rev-parse --abbrev-ref HEAD)" != "main" ]]; then
  echo "Check out main first." >&2
  exit 1
fi

if ! command -v gh >/dev/null || ! gh auth status >/dev/null 2>&1; then
  echo "Install and sign in to the GitHub CLI first: brew install gh && gh auth login" >&2
  exit 1
fi

args=(scripts/agents/orchestrator.py run --auto --ci --parallel "${PARALLEL:-2}" "$@")
if command -v caffeinate >/dev/null; then
  exec caffeinate -i python3 "${args[@]}"
fi
exec python3 "${args[@]}"
