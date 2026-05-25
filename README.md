# AnnaSuraksha v5 — PDS Fraud Intelligence

[![Build and Test](https://github.com/DeepxD-code/AnnaSuraksha/actions/workflows/maven.yml/badge.svg)](https://github.com/DeepxD-code/AnnaSuraksha/actions/workflows/maven.yml)

AnnaSuraksha is a demo-ready prototype of a Public Distribution System (PDS) fraud intelligence
platform. It provides deterministic fraud scoring, multi-layer ghost detection, supply-chain
discrepancy detection, real-time SSE alerts, and an explainability layer that integrates with an
LLM (Groq) with a deterministic fallback.

This README was expanded to include implementation notes, security cautions, and a productization
plan. Remove demo mode and secrets before any real data deployment.

---

Quick facts
- Server port: 8081 (default)
- Demo DB: H2 in-memory (jdbc:h2:mem:annasuraksha)
- H2 console: /h2-console (username: sa, password: <empty>)
- Demo seeding: enabled by default (15 beneficiaries, 45 FPS deliveries seeded)

Quick start (demo)
1. mvn -DskipTests clean package
2. mvn -DskipTests spring-boot:run
3. Open http://localhost:8081

Demo accounts (seeded) — change these before publishing
- admin@annasuraksha.gov.in / Admin@123 (ROLE_ADMIN)
- officer@up.gov.in / Officer@123 (ROLE_GOVT_OFFICER)
- auditor@cag.gov.in / Audit@123 (ROLE_AUDITOR)
- fps@mh.gov.in / Fps@123 (ROLE_FPS_OPERATOR)

API highlights
- Fraud scoring endpoints: /api/fraud/* (summary, high-risk, score/{id}, score-all)
- Simulation endpoints: /api/simulate/* (8 scenarios + cleanup)
- Supply chain: /api/supply-chain/* (warehouse-load, dispatch, fps-receive, ledger verify)
- Alerts: /api/alerts/* (SSE stream, active alerts, acknowledge)
- Public: /api/stats, /api/transparency/*

Important implementation notes
- FraudRiskScoringService: deterministic feature extraction and weighted scoring with in-memory
  cache (ConcurrentHashMap). Weights are constants; non-linear floors ensure HIGH scores for
  crucial signals (duplicate Aadhaar, impossible travel).
- GhostDetectionService: 3 layers (duplicate Aadhaar, entitlement/category anomalies,
  velocity/cross-state) and applyFlags marks beneficiaries as GHOST.
- BlockchainService: local SHA-256 hash chain stored in beneficiary blockHash/prevBlockHash fields.
  Useful for audit trails in-demo but not tamper-proof in production.
- FraudExplanationService: calls Groq LLM for concise audit-style explanations; deterministic
  fallback built-in if Groq is unavailable.

Hard-coded/demo data (must remove for production)
- Demo user emails/passwords listed above (DemoDataSeeder)
- Demo beneficiaries and Aadhaar-like raw values embedded in DemoDataSeeder and SimulationService
  (e.g., "999900001111", "888700006661", etc.) used for scenario generation.
- application.properties contains placeholders/defaults:
  - groq.api.key=${GROQ_API_KEY:YOUR_GROQ_KEY_HERE}
  - jwt.secret=${JWT_SECRET:ANNASURAKSHA_V5_JWT_SECRET_KEY_32CHARS_MIN}
  - annasuraksha.seed.demo-data=true

Security & privacy (short)
- Do NOT expose H2 console in production.
- Use a persistent DB (Postgres) with proper migrations (Flyway/Liquibase).
- Store secrets in a vault or environment variables; rotate the JWT secret and API keys.
- Create readonly DB roles and masked views for any admin query console to avoid PII leakage.
- Add DPIA, encryption at rest, audit logs, and a pen-test before any government deployment.

Enabling H2 locally (safe demo mode)
-----------------------------------
If you need the H2 console UX for local demos, enable it only on a developer machine and never in
production. Recommended approaches:

- One-off run (recommended for demos):
  - Linux / macOS:
    ```bash
    mvn -DskipTests spring-boot:run -Dspring-boot.run.arguments="--spring.h2.console.enabled=true --annasuraksha.seed.demo-data=true"
    ```
  - Windows PowerShell:
    ```powershell
    $env:SPRING_H2_CONSOLE_ENABLED='true'; $env:ANNASURAKSHA_SEED_DEMO='true'; mvn -DskipTests spring-boot:run
    ```

- Dev profile (safer): create `src/main/resources/application-dev.properties` and set:
  ```properties
  spring.h2.console.enabled=true
  annasuraksha.seed.demo-data=true
  ```
  Then run with the dev profile:
  ```bash
  mvn -DskipTests spring-boot:run -Dspring-boot.run.profiles=dev
  ```

Notes:
- For reproducible demo credentials, set environment variables before starting the app:
  - DEMO_ADMIN_PW, DEMO_OFFICER_PW, DEMO_AUDIT_PW, DEMO_FPS_PW
- The H2 console will be available at: http://localhost:8081/h2-console when enabled. Use JDBC URL
  `jdbc:h2:mem:annasuraksha`, username `sa`, and empty password (demo only).
- Always disable the H2 console and demo seeding in staging/production.

Quick dev flow (mint a dev token)
--------------------------------
1. Enable H2 and set a bootstrap secret (only on your dev machine):

   Bash / macOS / Linux:

   ```bash
   export SPRING_H2_CONSOLE_ENABLED=true
   export DEV_BOOTSTRAP_SECRET=verysecret
   mvn -DskipTests spring-boot:run
   ```

   PowerShell:

   ```powershell
   $env:SPRING_H2_CONSOLE_ENABLED='true'; $env:DEV_BOOTSTRAP_SECRET='verysecret'; mvn -DskipTests spring-boot:run
   ```

2. Mint a short-lived dev token using the bootstrap secret (no admin token required):

   ```bash
   curl -sS -X POST http://localhost:8081/api/auth/dev-token \
     -H "Content-Type: application/json" \
     -H "X-BOOTSTRAP-SECRET: verysecret" \
     -d '{"email":"admin@local"}' | jq -r '.data.token'
   ```

3. Use the token to open the dev console or H2 console in your browser by setting an Authorization header
   in developer tools or using curl to access a protected route:

   ```bash
   curl -H "Authorization: Bearer <TOKEN>" http://localhost:8081/dev/console
   ```

Remember: never enable H2 or set DEV_BOOTSTRAP_SECRET on shared or production machines.
Productization checklist (priority)
1. Immediate: disable demo seeding by default; remove plaintext demo passwords; require env vars for
   JWT_SECRET and GROQ_API_KEY.
2. Short term: replace H2 with Postgres + migrations; unit and integration tests; Redis for caching.
3. Medium term: move scoring off the main app into a scalable worker/streaming system (Kafka + workers),
   signed ledger snapshots/notarization, SSO for admin console, and full compliance controls.

Pitch & pilot (short)
- Target: State PDS/food departments, NIC, CAG/state auditors, donors (World Bank).
- Pilot ask: 1 district/state, 3 months, read-only data feed (6 months history), success metrics (TPs,
  estimated Rs. recovered, time-to-detect, false-positive rate).
- Demo script: login as admin → run GHOST_BENEFICIARY scenario → show high-risk list + explanation → show alerts.

Contribution & license
- Verify third-party dependency licenses before commercial use. Add a LICENSE file (MIT recommended
  for open-source demos) and a short CONTRIBUTORS/NON-USE note if AI assistance was used.

Support & next steps
- If you want, I can (pick one):
  - Add a docker-compose for Postgres + pgweb + readonly user for a secure admin console demo.
  - Patch the code to disable demo seeding except under a `dev` profile and require env vars for secrets.
  - Add a one-page pitch and a 2-minute demo script (curl steps + screenshots).

---

This summary was generated from the repository code and a short security/productization review.
