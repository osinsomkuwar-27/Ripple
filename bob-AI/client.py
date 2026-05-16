import requests
import json
import re
import os
from pathlib import Path
from dotenv import load_dotenv

#LOAD ENV
load_dotenv()

IBM_API_KEY = os.getenv("IBM_API_KEY")
PROJECT_ID  = os.getenv("PROJECT_ID")
MODEL_ID    = os.getenv("MODEL_ID", "ibm/granite-13b-chat-v2")
REGION      = os.getenv("REGION", "us-south")
ENV         = os.getenv("ENV", "development")

IAM_URL     = "https://iam.cloud.ibm.com/identity/token"
WATSONX_URL = f"https://{REGION}.ml.cloud.ibm.com/ml/v1/text/chat?version=2023-05-29"

#SYSTEM PROMPT

SYSTEM_PROMPT = """You are a code-change impact analyzer for Java Spring Boot projects.

When given:
1. A changed file name (e.g. Owner.java)
2. A plain-English description of what changed
3. A dependency graph as a JSON adjacency list

Trace all files directly and indirectly affected by the change.

RULES:
- Respond ONLY with a valid JSON object. No explanation, no markdown, no prose.
- Do not wrap output in backticks or code blocks.
- Never add any text before or after the JSON.

Output MUST follow this exact schema (field names are case-sensitive):
{
  "changedFile": "Owner.java",
  "summary": "one sentence, max 20 words",
  "affectedFiles": [
    {
      "file": "Pet.java",
      "risk": "HIGH | MEDIUM | LOW",
      "reason": "one sentence explaining why this file is affected"
    }
  ]
}"""

#DEV MOCK

MOCK_RESPONSE = {
    "changedFile": "Owner.java",
    "summary": "Owner field type change and new methods ripple into Pet and controller layers.",
    "affectedFiles": [
        {
            "file":   "Pet.java",
            "risk":   "HIGH",
            "reason": "Owner directly holds a collection of Pet; type change affects this relationship."
        },
        {
            "file":   "OwnerController.java",
            "risk":   "HIGH",
            "reason": "Controller exposes Owner endpoints; new setAddress method changes API behavior."
        },
        {
            "file":   "OwnerRepository.java",
            "risk":   "MEDIUM",
            "reason": "Repository queries Owner fields; field type change may affect JPA mappings."
        },
        {
            "file":   "PetController.java",
            "risk":   "MEDIUM",
            "reason": "Indirectly affected via Pet which depends on Owner's updated collection type."
        },
        {
            "file":   "VisitController.java",
            "risk":   "LOW",
            "reason": "Indirectly affected through Pet -> Visit chain; low direct coupling."
        }
    ]
}

#DIFF PARSER

def parse_diff(diff_path: str) -> dict:
    """
    Parse a .diff file and extract:
    - changedFile        → AnalyzeRequest.changedFile
    - changeDescription  → AnalyzeRequest.changeDescription
    """
    diff_text = Path(diff_path).read_text()

    changed_file  = "Unknown.java"
    added_lines   = []
    removed_lines = []

    for line in diff_text.splitlines():
        if line.startswith("+++ b/"):
            changed_file = line.split("/")[-1]

        elif line.startswith("+") and not line.startswith("+++"):
            added_lines.append(line[1:].strip())

        elif line.startswith("-") and not line.startswith("---"):
            removed_lines.append(line[1:].strip())

    change_description = (
        f"Added {len(added_lines)} lines and removed {len(removed_lines)} lines. "
        f"Key additions: {'; '.join(added_lines[:5]) if added_lines else 'none'}."
    )

    return {
        "changedFile":       changed_file,
        "changeDescription": change_description,
        "raw_diff":          diff_text,
    }

#IBM AUTH 

def get_iam_token() -> str:
    resp = requests.post(
        IAM_URL,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        data={
            "grant_type": "urn:ibm:params:oauth:grant-type:apikey",
            "apikey": IBM_API_KEY,
        },
        timeout=30,
    )
    resp.raise_for_status()
    return resp.json()["access_token"]

#JSON EXTRACTOR

def extract_json(text: str) -> dict:
    """Safely pull JSON out even if Bob adds a preamble."""
    try:
        return json.loads(text.strip())
    except json.JSONDecodeError:
        pass
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if match:
        try:
            return json.loads(match.group())
        except json.JSONDecodeError:
            pass
    raise ValueError(f"Could not extract valid JSON from Bob's response:\n{text}")

#CORE: CALL BOB

def call_bob(changed_file: str, change_description: str, dependency_graph: dict) -> dict:
    """Call IBM watsonx Bob and return parsed RippleResponse-shaped dict."""

    token = get_iam_token()

    user_message = (
        f"Changed file: {changed_file}\n\n"
        f"Change description: {change_description}\n\n"
        f"Dependency graph:\n{json.dumps(dependency_graph, indent=2)}"
    )

    payload = {
        "model_id":   MODEL_ID,
        "project_id": PROJECT_ID,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user",   "content": [{"type": "text", "text": user_message}]}
        ],
        "temperature":        0,
        "max_tokens":         600,
        "frequency_penalty":  0,
        "presence_penalty":   0,
        "top_p":              1,
    }
    print(f"Calling: {WATSONX_URL}")
    print(f"Model: {MODEL_ID}")
    print(f"Project: {PROJECT_ID}")
    resp = requests.post(
        WATSONX_URL,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type":  "application/json",
            "Accept":        "application/json",
        },
        json=payload,
        timeout=60,
    )
    resp.raise_for_status()

    raw_text = resp.json()["choices"][0]["message"]["content"]
    return extract_json(raw_text)

#MAIN: ANALYZE RIPPLE

def analyze_ripple(diff_path: str, dependency_graph: dict) -> dict:
    """
    Full pipeline:
      .diff file → parse → call Bob (or mock) → RippleResponse-shaped dict

    DEV:  mock response, no API call, no Bobcoins burned
    PROD: real Bob call via IBM watsonx
    """
    diff_data = parse_diff(diff_path)

    print(f"[ENV: {ENV}]")
    print(f"changedFile:       {diff_data['changedFile']}")
    print(f"changeDescription: {diff_data['changeDescription']}\n")

    if ENV == "development":
        print("DEV MODE: returning mock response (no API call)\n")
        mock = MOCK_RESPONSE.copy()
        mock["changedFile"] = diff_data["changedFile"]
        return mock

    if not IBM_API_KEY or IBM_API_KEY == "your_ibm_api_key_here":
        raise EnvironmentError("IBM_API_KEY not set in .env")

    return call_bob(
        diff_data["changedFile"],
        diff_data["changeDescription"],
        dependency_graph,
    )

#VALIDATOR

def validate_output(result: dict) -> bool:
    """
    Validate Bob's output matches Osin's Java classes exactly:
      RippleResponse  → changedFile, summary, affectedFiles
      AffectedFile    → file, reason, risk
    """
    errors = []

    for key in ["changedFile", "summary", "affectedFiles"]:
        if key not in result:
            errors.append(f"Missing top-level key: '{key}'")

    if "affectedFiles" in result:
        for i, af in enumerate(result["affectedFiles"]):
            for field in ["file", "reason", "risk"]:   # matches AffectedFile.java
                if field not in af:
                    errors.append(f"affectedFiles[{i}] missing '{field}'")
            if af.get("risk") not in ("HIGH", "MEDIUM", "LOW"):
                errors.append(f"affectedFiles[{i}].risk must be HIGH / MEDIUM / LOW")

    if errors:
        print("VALIDATION FAILED:")
        for e in errors:
            print(f"  - {e}")
        return False

    print(f"Valid! {len(result['affectedFiles'])} affected files, schema matches Osin's AffectedFile.java.")
    return True

# PETCLINIC GRAPH LOADER

JAVA_KEYWORDS = {
    "for", "public", "private", "protected", "class", "interface",
    "enum", "if", "else", "return", "new", "import", "static",
    "final", "abstract", "extends", "implements", "void", "null"
}

def load_graph(adjacency_path: str) -> dict:
    """
    Load dependency graph from Bhargavi's adjacency.json.
    Uses 'dependents' key — who gets affected when a class changes.
    Falls back to hardcoded PetClinic graph if file missing or broken.
    """
    try:
        with open(adjacency_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        raw = data.get("dependents", {})

        cleaned = {
            cls: [d for d in deps if d not in JAVA_KEYWORDS]
            for cls, deps in raw.items()
            if cls not in JAVA_KEYWORDS
        }

        print(f"Loaded real adjacency graph: {len(cleaned)} classes from {adjacency_path}")
        return cleaned

    except FileNotFoundError:
        print(f"WARNING: {adjacency_path} not found — using hardcoded fallback graph")
    except json.JSONDecodeError as e:
        print(f"WARNING: Could not parse {adjacency_path} ({e}) — using hardcoded fallback graph")
    except Exception as e:
        print(f"WARNING: Unexpected error loading graph ({e}) — using hardcoded fallback graph")

    #Fallback: hardcoded PetClinic graph
    return {
        "Owner":           ["OwnerController", "PetController", "VisitController"],
        "Pet":             ["Owner", "PetController", "PetValidator", "VisitController"],
        "Visit":           ["Owner", "Pet", "VisitController"],
        "PetType":         ["Pet", "PetController", "PetTypeFormatter"],
        "PetValidator":    ["PetController"],
        "Specialty":       ["Vet"],
        "Vet":             ["PetClinicRuntimeHints", "VetController", "Vets"],
        "Vets":            ["VetController"],
    }


#ENTRY POINT 

if __name__ == "__main__":

    petclinic_graph = load_graph("../parser/adjacency.json")

    result = analyze_ripple("test_owner.diff", petclinic_graph)

    print("\nBob ripple analysis result:")
    print(json.dumps(result, indent=2))
    print()
    validate_output(result)