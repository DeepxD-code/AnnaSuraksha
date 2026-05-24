# Antigravity System Index
Version: 1.0 — FINAL

---

## PURPOSE

This file is the entry point. Post ALL files in this system to Antigravity together.
This index tells the orchestrator what each file is, what it governs, and the order to read them.

---

## FILE MAP

| File                      | Governs                                         | Read by                        |
|---------------------------|-------------------------------------------------|--------------------------------|
| `SYSTEM_INDEX.md`         | This file — overall system map                  | Orchestrator (first)           |
| `model_routing.md`        | Agent definitions, system prompts, workflow     | Orchestrator (second)          |
| `execution_protocol.md`   | Stage-by-stage reasoning pipeline per agent     | All agents (stage reference)   |
| `architecture_rules.md`   | System design, layering, API, security, infra   | AGENT-1, AGENT-2, AGENT-3      |
| `code_quality_rules.md`   | Code standards, TypeScript, testing, git        | AGENT-3, AGENT-4, AGENT-5, AGENT-6 |

---

## READING ORDER FOR ORCHESTRATOR

```
1. SYSTEM_INDEX.md       ← you are here — understand the system
2. model_routing.md      ← load agent registry + system prompts
3. execution_protocol.md ← load stage pipeline and enforcement rules
4. architecture_rules.md ← load as context to pass to AGENT-1/2/3
5. code_quality_rules.md ← load as context to pass to AGENT-3/4/5/6
```

---

## AGENT → FILE DEPENDENCY MAP

| Agent   | Role          | Must receive                              |
|---------|---------------|-------------------------------------------|
| AGENT-1 | Decision Maker| execution_protocol.md + architecture_rules.md |
| AGENT-2 | Planner       | execution_protocol.md + architecture_rules.md |
| AGENT-3 | Coder         | execution_protocol.md + architecture_rules.md + code_quality_rules.md |
| AGENT-4 | Debugger      | code_quality_rules.md (error handling section) |
| AGENT-5 | Tester        | code_quality_rules.md (testing section)   |
| AGENT-6 | Documenter    | code_quality_rules.md (documentation section) |

When spawning each agent, include only the sections relevant to that agent — do not dump all files into every agent's context.

---

## ENFORCEMENT HIERARCHY

If any rule in these files conflicts, this order wins:

```
1. model_routing.md      (agent boundaries — highest priority)
2. execution_protocol.md (stage enforcement — second)
3. architecture_rules.md (design decisions — third)
4. code_quality_rules.md (implementation standards — fourth)
```

---

## QUICK START

Post all 5 files to Antigravity, then describe your project. The orchestrator will:
1. Cold-start message to user
2. Spawn AGENT-1 for decision + architecture
3. Route through all agents per model_routing.md workflow
4. Deliver tested, documented, production-ready output
