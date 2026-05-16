import os
import re
import json
from pathlib import Path

PETCLINIC_REPO = Path(r"C:\Users\Vaishali Tamba\Desktop\spring-petclinic")

DEMO_SCENARIOS = {

    "scenario_1_owner_rename": {
        "description": "Rename address field to streetAddress in Owner.java",
        "changed_files": [
            "src/main/java/org/springframework/samples/petclinic/owner/Owner.java"
        ],
        "simulated_change": {
            "file": "src/main/java/org/springframework/samples/petclinic/owner/Owner.java",
            "old_line": "private String address;",
            "new_line": "private String streetAddress;"
        }
    },

    "scenario_2_visit_field": {
        "description": "Rename date field to visitDate in Visit.java",
        "changed_files": [
            "src/main/java/org/springframework/samples/petclinic/owner/Visit.java"
        ],
        "simulated_change": {
            "file": "src/main/java/org/springframework/samples/petclinic/owner/Visit.java",
            "old_line": "private LocalDate date;",
            "new_line": "private LocalDate visitDate;"
        }
    },

    "scenario_3_vet_repository": {
        "description": "Add new query method to VetRepository.java",
        "changed_files": [
            "src/main/java/org/springframework/samples/petclinic/vet/VetRepository.java"
        ],
        "simulated_change": {
            "file": "src/main/java/org/springframework/samples/petclinic/vet/VetRepository.java",
            "old_line": "Collection<Vet> findAll() throws DataAccessException;",
            "new_line": "Collection<Vet> findBySpecialty(String specialty) throws DataAccessException;"
        }
    }
}

def generate_diff_for_scenario(scenario_name, scenario):
    print(f"\n[Processing] {scenario_name}")
    print(f"  Description : {scenario['description']}")

    change = scenario["simulated_change"]
    file_rel_path = change["file"]
    old_line = change["old_line"].strip()
    new_line = change["new_line"].strip()

    full_path = PETCLINIC_REPO / Path(file_rel_path.replace("/", os.sep))

    if not full_path.exists():
        print(f"  ERROR: File not found: {full_path}")
        return None

    src_lines = full_path.read_text(encoding="utf-8").splitlines()

    found_line_num = None
    for i, line in enumerate(src_lines):
        if old_line in line.strip():
            found_line_num = i
            break

    if found_line_num is None:
        print(f"  WARNING: Could not find '{old_line}' in {file_rel_path}")
        diff_content = f"""--- a/{file_rel_path}
+++ b/{file_rel_path}
@@ -1,3 +1,3 @@
 // Placeholder diff - original line not found
-{old_line}
+{new_line}
"""
    else:
        context_start = max(0, found_line_num - 3)
        context_end = min(len(src_lines), found_line_num + 4)

        diff_lines = []
        diff_lines.append(f"--- a/{file_rel_path}")
        diff_lines.append(f"+++ b/{file_rel_path}")
        diff_lines.append(f"@@ -{context_start+1},{context_end-context_start} +{context_start+1},{context_end-context_start} @@")

        for i in range(context_start, context_end):
            line = src_lines[i]
            if i == found_line_num:
                diff_lines.append(f"-    {old_line}")
                diff_lines.append(f"+    {new_line}")
            else:
                diff_lines.append(f" {line}")

        diff_content = "\n".join(diff_lines) + "\n"
        print(f"  Found target line at line {found_line_num + 1}")

    output_filename = f"{scenario_name}.diff"
    with open(output_filename, "w", encoding="utf-8") as out:
        out.write(diff_content)

    print(f"  Written : {output_filename}")
    return output_filename


def write_pr_payload(scenario_name, scenario, diff_filename):
    payload = {
        "pr_number": 42,
        "pr_title": scenario["description"],
        "base_branch": "main",
        "head_branch": f"feature/{scenario_name}",
        "changed_files": scenario["changed_files"],
        "diff_file": diff_filename,
        "repo": "spring-petclinic"
    }

    payload_filename = f"{scenario_name}_pr_payload.json"
    with open(payload_filename, "w", encoding="utf-8") as out:
        json.dump(payload, out, indent=2)

    print(f"  PR payload : {payload_filename}")
    return payload_filename


print("=" * 55)
print("GENERATE_DIFF.PY — P4 Bhargavi")
print("Generating .diff files for all three demo scenarios")
print("=" * 55)

generated_files = []

for scenario_name, scenario in DEMO_SCENARIOS.items():
    diff_file = generate_diff_for_scenario(scenario_name, scenario)
    if diff_file:
        payload_file = write_pr_payload(scenario_name, scenario, diff_file)
        generated_files.append(diff_file)
        generated_files.append(payload_file)

print()
print("=" * 55)
print("DONE. Files generated:")
print("=" * 55)
for fname in generated_files:
    print(f"  {fname}")

print()
print("Send all .diff and _pr_payload.json files to P5 (Tanishka).")