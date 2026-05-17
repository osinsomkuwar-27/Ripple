#!/usr/bin/env node
// Usage: node scripts/parse_bob_output.js <input_file> <output_file>
const fs = require("fs");

const inputFile = process.argv[2] || "bob_analysis_output.txt";
const outputFile = process.argv[3] || "impact_result.json";
const raw = fs.readFileSync(inputFile, "utf8");

// Attempt 1: clean JSON
try {
  const parsed = JSON.parse(raw.trim());
  fs.writeFileSync(outputFile, JSON.stringify(parsed, null, 2));
  console.log("SUCCESS: clean JSON");
  process.exit(0);
} catch (_) {}

// Attempt 2: find parseable JSON objects in prose and select impact payload
const starts = [];
for (let i = 0; i < raw.length; i += 1) {
  if (raw[i] === "{") starts.push(i);
}

let best = null;
for (const start of starts) {
  for (let end = raw.lastIndexOf("}"); end > start; end = raw.lastIndexOf("}", end - 1)) {
    const candidate = raw.slice(start, end + 1).trim();
    try {
      const parsed = JSON.parse(candidate);
      if (
        parsed &&
        typeof parsed === "object" &&
        "intent" in parsed &&
        Array.isArray(parsed.affected_files)
      ) {
        best = parsed;
        break;
      }
    } catch (_) {
      // continue trying other ranges
    }
  }
  if (best) break;
}

if (!best) {
  console.error("ERROR: no valid impact JSON object found in Bob output");
  process.exit(1);
}

fs.writeFileSync(outputFile, JSON.stringify(best, null, 2));
console.log("SUCCESS: JSON extracted from prose");
