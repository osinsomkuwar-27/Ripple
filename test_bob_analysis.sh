#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${BOBSHELL_API_KEY:-}" ]]; then
  echo "ERROR: BOBSHELL_API_KEY is not set."
  echo "Set it first: export BOBSHELL_API_KEY=your_key"
  exit 1
fi

INTENT="${INTENT:-Rename the Owner class to Client and update all references}"
CHANGED="${CHANGED:-spring-petclinic/src/main/java/org/springframework/samples/petclinic/owner/Owner.java}"

bob --auth-method api-key -p \
"You are an impact analyzer for a Java codebase.
A developer intends to make this change: ${INTENT}
The directly changed file is: ${CHANGED}

Analyze which OTHER files in this repository will be affected.
For each file provide the file path, risk tier (high/medium/low),
risk score (0-100), reason it is affected, and which line numbers
are most likely to break.

Return ONLY this JSON. No prose. No explanation. No markdown.
{
  \"intent\": \"string\",
  \"affected_files\": [
    {
      \"file_path\": \"string\",
      \"risk_tier\": \"high|medium|low\",
      \"risk_score\": 0,
      \"reason\": \"string\",
      \"likely_broken_lines\": [47, 83]
    }
  ],
  \"total_affected\": 0,
  \"high_risk_count\": 0,
  \"medium_risk_count\": 0,
  \"low_risk_count\": 0
}" > bob_analysis_output.txt 2>&1

echo "=== Bob analysis output ==="
cat bob_analysis_output.txt

if cat bob_analysis_output.txt | python3 -m json.tool > /dev/null 2>&1; then
  echo "SUCCESS: Valid JSON returned"
else
  echo "WARNING: Bob added prose — run scripts/parse_bob_output.js to extract JSON"
fi
