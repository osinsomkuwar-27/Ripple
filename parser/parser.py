import os
import re
import json
from pathlib import Path

# ─────────────────────────────────────────────────────────────────────────────
# PATH SETUP
# Ripple/parser/ is where this script lives.
# spring-petclinic is two levels up (Desktop), then into its java source.
# ─────────────────────────────────────────────────────────────────────────────
BASE = Path("../../spring-petclinic/src/main/java/org/springframework/samples/petclinic")

# ─────────────────────────────────────────────────────────────────────────────
# STEP A — Find all .java files
# ─────────────────────────────────────────────────────────────────────────────
java_files = []
for root, dirs, files in os.walk(BASE):
    for file in files:
        if file.endswith(".java"):
            java_files.append(Path(root) / file)

print(f"[A] Found {len(java_files)} Java files:")
for f in java_files:
    print(f"    {f.name}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP B — Collect all class names defined inside PetClinic
# ─────────────────────────────────────────────────────────────────────────────
all_classes = set()
class_to_file = {}

for f in java_files:
    src = f.read_text(encoding="utf-8")
    match = re.search(r'(?:class|interface|enum)\s+(\w+)', src)
    if match:
        name = match.group(1)
        all_classes.add(name)
        class_to_file[name] = str(f)

print(f"\n[B] Found {len(all_classes)} internal classes:")
for c in sorted(all_classes):
    print(f"    {c}")

# ─────────────────────────────────────────────────────────────────────────────
# STEP C — Build dependency graph
# For each class, find which other internal classes it references
# Three scans: imports, field types, any uppercase token matching a class name
# ─────────────────────────────────────────────────────────────────────────────
graph = {}

for f in java_files:
    src = f.read_text(encoding="utf-8")

    class_match = re.search(r'(?:class|interface|enum)\s+(\w+)', src)
    if not class_match:
        continue
    class_name = class_match.group(1)

    deps = set()

    # Scan 1 — import statements
    for imp in re.findall(r'import\s+[\w.]+\.(\w+)\s*;', src):
        if imp in all_classes and imp != class_name:
            deps.add(imp)

    # Scan 2 — field type declarations (handles generics like List<Pet>)
    for field_type in re.findall(
        r'(?:private|protected|public)\s+(?:List<|Set<|Collection<|Optional<)?(\w+)>?\s+\w+\s*[;=,(]',
        src
    ):
        if field_type in all_classes and field_type != class_name:
            deps.add(field_type)

    # Scan 3 — any uppercase word matching a known class name
    for token in re.findall(r'\b([A-Z][a-zA-Z]+)\b', src):
        if token in all_classes and token != class_name:
            deps.add(token)

    graph[class_name] = sorted(list(deps))

print(f"\n[C] Dependency graph built for {len(graph)} classes")

# ─────────────────────────────────────────────────────────────────────────────
# STEP D — Build reverse index
# dependents[X] = list of classes that USE X
# This is the ripple list Bob needs
# ─────────────────────────────────────────────────────────────────────────────
reverse = {}

for cls, deps in graph.items():
    for dep in deps:
        if dep not in reverse:
            reverse[dep] = []
        if cls not in reverse[dep]:
            reverse[dep].append(cls)

for key in reverse:
    reverse[key] = sorted(reverse[key])

print(f"[D] Reverse index built")

# ─────────────────────────────────────────────────────────────────────────────
# STEP E — Write adjacency.json
# ─────────────────────────────────────────────────────────────────────────────
output = {
    "dependencies": graph,
    "dependents": reverse,
    "all_classes": sorted(list(all_classes))
}

with open("adjacency.json", "w", encoding="utf-8") as out_file:
    json.dump(output, out_file, indent=2)

print(f"\n[E] adjacency.json written!")
print(f"    Total classes : {len(all_classes)}")
print(f"    Total dep edges: {sum(len(v) for v in graph.values())}")