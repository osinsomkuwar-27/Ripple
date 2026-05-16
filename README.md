# Ripple — AI-Powered Cross-Repo Impact Analyzer

> **Built for IBM Bob Hackathon 2025** · Powered by IBM Bob Shell

Ripple answers the question every developer fears: **"If I change this file, what else breaks?"**

Unlike Copilot or Cursor which answer *"what does this file do?"*, Ripple answers *"if I change this, what breaks everywhere else?"* — a fundamentally different category of intelligence, only possible because IBM Bob holds full repo context.

---

## Demo

Change `Owner.java` → Ripple instantly maps 22 affected files across 6 modules, with risk levels and reasons for each.

```
POST /api/analyze
{
  "changedFile": "Owner.java",
  "changeDescription": "Rename field lastName to surname"
}
```

```json
{
  "id": "3fad9aa7-e063-4dbf-a77d-6e97cc942d2b",
  "result": {
    "changedFile": "Owner.java",
    "summary": "22 files affected across 6 modules",
    "affectedFiles": [
      { "file": "Person.java", "reason": "Parent class defining lastName field", "risk": "HIGH" },
      { "file": "OwnerRepository.java", "reason": "Query method findByLastNameStartingWith()", "risk": "HIGH" },
      { "file": "findOwners.html", "reason": "Search form uses lastName field", "risk": "HIGH" }
    ]
  }
}
```

---

## How It Works

```
Developer describes a change
        ↓
POST /api/analyze (Spring Boot backend)
        ↓
Bob Shell CLI reads the full repo context
bob --auth-method api-key -p "analyze impact of..."
        ↓
Bob identifies affected files, reasons, risk levels
        ↓
Backend parses + stores result with a unique ID
        ↓
Frontend renders interactive ripple map
        ↓
GitHub Action posts impact summary as PR comment
```

---

## Architecture

```
ripple-backend/          ← Spring Boot (Java 17)
  POST /api/analyze      ← triggers Bob Shell, returns impact JSON + ID
  GET  /api/analysis/:id ← fetch previous result by ID

spring-petclinic/        ← demo repo Bob analyzes

.github/workflows/       ← GitHub Action triggers on PR open
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| AI Engine | IBM Bob Shell CLI |
| Backend | Spring Boot 3.2 · Java 17 |
| Demo Repo | Spring PetClinic |
| CI/CD | GitHub Actions |
| Frontend | React + React Flow |
| Deployment | Railway (backend) · Vercel (frontend) |

---

## Getting Started

### Prerequisites
- Java 17+
- Maven
- IBM Bob Shell (`bob --version`)
- Bob API Key from [bob.ibm.com](https://bob.ibm.com)

### 1. Clone the repo

```bash
git clone https://github.com/osinsomkuwar-27/Ripple.git
cd Ripple
```

### 2. Clone the demo repo (Spring PetClinic)

```bash
git clone https://github.com/spring-projects/spring-petclinic.git
```

### 3. Install Bob Shell

```bash
curl -fsSL https://bob.ibm.com/download/bobshell.sh | bash
bob --version
```

### 4. Configure the backend

```bash
cd ripple-backend
cp src/main/resources/application.properties.template \
   src/main/resources/application.properties
```

Edit `application.properties`:
```properties
server.port=8081
bob.api.key=YOUR_BOB_API_KEY_HERE
bob.repo.path=/path/to/spring-petclinic
```

### 5. Run the backend

```bash
mvn spring-boot:run
```

Backend starts on `http://localhost:8081`

---

## API Reference

### POST `/api/analyze`

Triggers Bob to analyze the impact of a code change.

**Request:**
```json
{
  "changedFile": "Owner.java",
  "changeDescription": "Rename field lastName to surname"
}
```

**Response:**
```json
{
  "id": "uuid",
  "result": {
    "changedFile": "Owner.java",
    "summary": "22 files affected across 6 modules",
    "affectedFiles": [
      {
        "file": "Person.java",
        "reason": "Parent class defining lastName field",
        "risk": "HIGH"
      }
    ]
  }
}
```

### GET `/api/analysis/{id}`

Fetch a previous analysis result by ID.

### GET `/api/health`

Returns `"Ripple backend running"` if the service is up.

---

## GitHub Action

Ripple automatically posts an impact summary on every PR:

```yaml
- name: Run Bob impact analysis
  env:
    BOBSHELL_API_KEY: ${{ secrets.BOBSHELL_API_KEY }}
  run: |
    bob --auth-method api-key -p "Analyze impact..." > result.json
```

Add `BOBSHELL_API_KEY` to your GitHub repository secrets to enable this.

---

## Team

| Role | Name | Responsibility |
|------|------|----------------|
| Tech Lead | P1 | IBM Bob workspace, prompt engineering |
| Backend Lead | P2 | Spring Boot API, Bob Shell integration |
| Frontend Lead | P3 | React ripple map visualization |
| Data / Parser | P4 | Java dependency graph, PetClinic analysis |
| Integrations | P5 | GitHub Action, CI/CD pipeline |
| Pitch + Docs | P6 | Demo script, pitch deck, submission |

---

## Why Ripple Wins

**Copilot/Cursor** → answers *"what does this file do?"*

**Ripple** → answers *"if I change this, what breaks everywhere else?"*

This is only possible because IBM Bob holds full repository context — not just the file you're looking at. Ripple exposes that superpower as a developer tool, a dashboard, and a CI/CD integration.

---

## License

MIT · Built at IBM Bob Hackathon · May 2025
