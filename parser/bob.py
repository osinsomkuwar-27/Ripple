import json

with open("adjacency.json", "r", encoding="utf-8") as f:
    data = json.load(f)

lines = []
lines.append("PETCLINIC CLASS DEPENDENCY MAP:")
lines.append("")
for cls, deps in sorted(data["dependencies"].items()):
    if deps:
        lines.append(f"{cls} depends on: {', '.join(deps)}")

lines.append("")
lines.append("REVERSE MAP (who uses each class):")
lines.append("")
for cls, used_by in sorted(data["dependents"].items()):
    if used_by:
        lines.append(f"{cls} is used by: {', '.join(used_by)}")

text = "\n".join(lines)
print(text)

with open("for_bob.txt", "w", encoding="utf-8") as out:
    out.write(text)

print("\nfor_bob.txt saved. Send this to P1.")