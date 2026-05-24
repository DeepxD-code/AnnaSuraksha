# Execution Protocol
Version: 3.0 — FINAL

---

## PURPOSE

This file defines the mandatory reasoning pipeline that ALL agents must follow.
No agent may skip a stage. No agent may begin its work until the previous stage's output is complete.
The orchestrator enforces this pipeline using model_routing.md.

---

## CRITICAL RULE — NO EARLY CODING

**Coding does not begin until Stage 4.**
AGENT-1 and AGENT-2 must complete Stages 1–3 fully before AGENT-3 is spawned.
If AGENT-3 receives a task without a completed plan from AGENT-2, it must respond:
`"BLOCKED: No plan received from AGENT-2. Cannot proceed."`

---

## STAGE PIPELINE

---

### STAGE 1 — Problem Analysis
**Assigned to:** AGENT-1 (`gemini-2.5-pro`)
**Blocks:** Nothing starts before this completes.

AGENT-1 must produce answers to all of the following before outputting its decision JSON:

**Problem Definition:**
- What exact problem is being solved?
- Who are the users and what are their needs?
- What does success look like — measurable acceptance criteria?

**System Requirements:**
- Functional requirements — what must the system do?
- Non-functional requirements — performance, availability, security SLAs?
- Integration requirements — what external systems must it connect to?

**Constraints:**
- Technical constraints (language, platform, existing stack)
- Time/scope constraints
- Budget or infrastructure constraints

**Edge Cases & Risks:**
- What inputs or states could break the system?
- What are the top 3 security risks?
- What happens at 10× scale?

**Output:** AGENT-1 decision JSON (see model_routing.md AGENT-1 system prompt for full schema)

---

### STAGE 2 — Architecture Design
**Assigned to:** AGENT-1 (`gemini-2.5-pro`)
**Blocks:** AGENT-2 cannot start until this completes.
**Rules file:** architecture_rules.md governs all decisions made here.

AGENT-1 must define:

**Architecture Overview:**
- Monolith vs microservices — justified by constraints from Stage 1
- Monorepo vs polyrepo decision
- Chosen layered architecture (must comply with architecture_rules.md)

**Module Breakdown:**
- Which feature modules exist (auth, billing, notifications, etc.)
- Each module's responsibility boundary — what it owns and what it must not touch
- Inter-module communication pattern (direct import, event bus, API call)

**Data Flow:**
- End-to-end data flow diagram: Client → Security → Controller → Service → DB
- Which data is cached and where (Redis / in-memory / CDN)
- State management strategy (frontend)

**Tech Stack Reasoning:**
- Language and runtime (with justification)
- Framework choice (with justification)
- Database choice (relational vs document vs graph — with justification)
- Auth strategy (JWT, session, OAuth — with justification)
- Infrastructure (Docker, cloud provider, deployment target)

**Output:** Append `architectureNotes` and `techStack` fields to the AGENT-1 decision JSON

---

### STAGE 3 — Implementation Plan
**Assigned to:** AGENT-2 (`o3-mini`)
**Blocks:** AGENT-3 cannot start until this completes.
**Input:** AGENT-1 decision JSON from Stages 1–2.

**NO CODING AT THIS STAGE.**

AGENT-2 must produce:

**Modules & File Structure:**
- Complete directory tree with every file named
- Each file's responsibility in one sentence
- Follow the module structure defined in architecture_rules.md exactly

**Task List:**
- Every implementation task as a discrete unit
- Each task assigned to AGENT-3
- Dependencies between tasks (what must complete before what)
- Parallel execution flags for tasks with no dependencies

**API Contracts:**
- Every route: method, path, request body shape, response body shape
- Auth requirements per route
- Rate limit overrides per route (if different from default)

**Database Design:**
- All tables/collections with fields and types
- Foreign key relationships
- Indexes required

**Environment Variables:**
- Complete list of required env vars
- Description and example value for each

**Testing Strategy:**
- Which modules need unit tests
- Which routes need integration tests
- Which flows need E2E tests (passed to AGENT-5)

**Output:** AGENT-2 plan JSON (see model_routing.md AGENT-2 system prompt for full schema)

---

### STAGE 4 — Implementation
**Assigned to:** AGENT-3 (`claude-opus-4-6`)
**Input:** AGENT-2 plan JSON from Stage 3.
**Rules files:** architecture_rules.md + code_quality_rules.md govern all code written here.

AGENT-3 implements one task at a time from the plan:

**Code must:**
- Follow the exact file structure from AGENT-2's plan — no deviation
- Respect all layer boundaries from architecture_rules.md
- Pass all standards from code_quality_rules.md (TypeScript strict, naming, JSDoc, error handling)
- Use only packages listed in AGENT-2's dependency list
- Never hardcode secrets — always use `process.env` via config module
- Validate all inputs with Zod at controller boundary
- Follow the API response envelope: `{ success, data, error, meta }`

**On error / type error / test failure:**
- AGENT-3 flags it and hands off to AGENT-4 (Debugger)
- AGENT-3 does not attempt to self-debug architecture-level issues

**On completion of each module:**
- Hand off to AGENT-5 (Tester) and AGENT-6 (Documenter) — these run in parallel

---

### STAGE 5 — Self Review & Final Gate
**Assigned to:** AGENT-1 (`gemini-2.5-pro`) — re-spawned for final review
**Input:** All outputs from AGENT-3, AGENT-4, AGENT-5, AGENT-6.

This is NOT a self-review by the coder. AGENT-1 runs an independent review gate.

**AGENT-1 checks against the acceptance criteria defined in Stage 1:**

**Architectural weaknesses:**
- Does the implementation match the architecture designed in Stage 2?
- Are layer boundaries respected throughout?
- Are there any hidden coupling issues between modules?

**Performance issues:**
- Are there N+1 query patterns?
- Are unbounded result sets returned anywhere?
- Are expensive operations blocking the event loop?

**Security risks:**
- Are all inputs validated?
- Are secrets properly externalized?
- Are auth/authz checks in place on all protected routes?
- Are rate limits applied to sensitive endpoints?

**Missing edge cases:**
- Do the tests cover the edge cases identified in Stage 1?
- Are error states handled and returning proper responses?

**AGENT-1 outputs:**
- `approved: true` → deliver all outputs to user
- `approved: false` → list specific `rejectionReasons`, return to the failed stage only

**Refactor rule:**
If AGENT-1 flags issues that require code changes → AGENT-3 refactors → AGENT-5 re-tests → AGENT-1 re-reviews.
Maximum 3 review cycles before escalating to human.

---

## STAGE SUMMARY TABLE

| Stage | Name                 | Agent   | Model                       | Produces                        | Coding? |
|-------|----------------------|---------|-----------------------------|---------------------------------|---------|
| 1     | Problem Analysis     | AGENT-1 | gemini-2.5-pro              | Decision JSON                   | ❌ No   |
| 2     | Architecture Design  | AGENT-1 | gemini-2.5-pro              | Extended Decision JSON           | ❌ No   |
| 3     | Implementation Plan  | AGENT-2 | o3-mini                     | Plan JSON                       | ❌ No   |
| 4     | Implementation       | AGENT-3 | claude-opus-4-6             | Production code files           | ✅ Yes  |
| 4b    | Debugging (if needed)| AGENT-4 | gemini-2.0-flash-thinking   | Fixed code files                | ✅ Fix  |
| 4c    | Testing              | AGENT-5 | gpt-4o                      | Test files                      | ✅ Tests|
| 4d    | Documentation        | AGENT-6 | claude-haiku-4-5            | Docs + JSDoc                    | ✅ Docs |
| 5     | Self Review Gate     | AGENT-1 | gemini-2.5-pro              | Approval or rejection list      | ❌ No   |

---

## ENFORCEMENT RULES FOR ORCHESTRATOR

- If AGENT-3 begins output before AGENT-2's plan JSON is received → abort AGENT-3, re-run AGENT-2
- If any agent outputs prose instead of JSON → re-prompt once, then use last valid JSON and log the failure
- If Stage 5 (AGENT-1 review) is rejected → do not restart from Stage 1 — return only to the specific failed stage
- If AGENT-1 rejects 3 consecutive times → pause and ask the user to clarify requirements
