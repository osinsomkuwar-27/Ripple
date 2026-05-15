import json

with open("adjacency.json", "r", encoding="utf-8") as f:
    data = json.load(f)

print("=" * 55)
print("ALL CLASSES FOUND")
print("=" * 55)
for cls in data["all_classes"]:
    print(f"  {cls}")

print()
print("=" * 55)
print("HERO DEMO — What depends on Owner?")
print("=" * 55)
owner_ripples = data["dependents"].get("Owner", [])
print(f"Owner has {len(owner_ripples)} dependents:\n")
for cls in owner_ripples:
    print(f"  => {cls}")

if len(owner_ripples) < 8:
    print("\nWARNING: fewer than 8 ripples found. Parser may need tuning.")
else:
    print(f"\nGREAT: {len(owner_ripples)} ripples. Demo will look impressive.")

print()
print("=" * 55)
print("FULL DEPENDENCY MAP (what each class uses)")
print("=" * 55)
for cls, deps in sorted(data["dependencies"].items()):
    if deps:
        print(f"  {cls}")
        for d in deps:
            print(f"      --> {d}")

print()
print("=" * 55)
print("FULL DEPENDENTS MAP (what uses each class)")
print("=" * 55)
for cls, used_by in sorted(data["dependents"].items()):
    print(f"  {cls}  ({len(used_by)} dependents)")
    for u in used_by:
        print(f"      <-- {u}")