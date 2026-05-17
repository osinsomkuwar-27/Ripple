#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${BOBSHELL_API_KEY:-}" ]]; then
  echo "ERROR: BOBSHELL_API_KEY is not set."
  echo "Set it first: export BOBSHELL_API_KEY=your_key"
  exit 1
fi

bob --auth-method api-key -p \
  "List the top 5 Java classes in this project.
Return as JSON array only: [{\"class\": \"\", \"purpose\": \"\"}]
No prose. No explanation." > bob_test_output.txt 2>&1

echo "=== Bob output ==="
cat bob_test_output.txt
