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
- H2 console: /h2-console (username: sa, password: <empty>) — localhost-only, no JWT required in dev
- Demo seeding: disabled by default; enable with `--annasuraksha.seed.demo-data=true`

Quick start (demo)
1. Set env vars for reproducible passwords (optional):
   ```
   set DEMO_ADMIN_PW=admin123& set DEMO_OFFICER_PW=officer123& set DEMO_AUDIT_PW=audit123& set DEMO_FPS_PW=fps123
   ```
2. `mvn -DskipTests clean package`
3. `java -jar target\annasuraksha-5.0.0.jar --spring.h2.console.enabled=true --annasuraksha.seed.demo-data=true`
4. Open http://localhost:8081

Demo accounts (seeded with env vars or auto-generated passwords)
| Email | Default role | Env var |
|---|---|---|
| admin@annasuraksha.gov.in | ROLE_ADMIN | DEMO_ADMIN_PW |
| officer@up.gov.in | ROLE_GOVT_OFFICER | DEMO_OFFICER_PW |
| auditor@cag.gov.in | ROLE_AUDITOR | DEMO_AUDIT_PW |
| fps@mh.gov.in | ROLE_FPS_OPERATOR | DEMO_FPS_PW |

Passwords are auto-generated (16-char Base64) if env vars are not set. For reproducible demos, set the env vars before starting the app.

API highlights
- Fraud scoring endpoints: /api/fraud/* (summary, high-risk, score/{id}, score-all)
- Simulation endpoints: /api/simulate/* (8 scenarios + cleanup)
- Supply chain: /api/supply-chain/* (warehouse-load, dispatch, fps-receive, ledger verify)
- Alerts: /api/alerts/* (SSE stream, active alerts, acknowledge)
- Public: /api/stats, /api/transparency/*

Admin API endpoints
-------------------
All admin endpoints require `ROLE_ADMIN` and are under `/api/admin/`.

### Ledger (snapshot management)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/ledger/verify/{id}` | Query a single snapshot's on-chain anchor and compare root hashes |
| GET | `/api/admin/ledger/verify-all` | Iterate all snapshots and check on-chain anchors for each |
| POST | `/api/admin/ledger/anchor/{id}` | (Re)anchor an existing snapshot on-chain (retries anchoring) |

### Audit logs
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/admin/audit/recent?pathPrefix=/api/admin/ledger` | Fetch recent audit log entries filtered by path prefix |

### Public ledger endpoints (officers/auditors/admins)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/ledger/snapshot` | Create a new Merkle snapshot of all beneficiaries |
| GET | `/api/ledger/snapshot/{id}/proof/{beneficiaryId}` | Get Merkle proof for a beneficiary in a snapshot |
| GET | `/api/ledger/snapshot/{id}/verify/{beneficiaryId}` | Verify a stored Merkle proof against the snapshot root |
| GET | `/api/ledger/snapshot/{id}/anchor` | Retrieve snapshot anchor metadata and on-chain anchor record |

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
  - annasuraksha.seed.demo-data=false (enabled via CLI flag for dev)

Security & privacy (short)
- Do NOT expose H2 console in production.
- Use a persistent DB (Postgres) with proper migrations (Flyway/Liquibase).
- Store secrets in a vault or environment variables; rotate the JWT secret and API keys.
- Create readonly DB roles and masked views for any admin query console to avoid PII leakage.
- Add DPIA, encryption at rest, audit logs, and a pen-test before any government deployment.

Enabling H2 locally (safe demo mode)
-----------------------------------
If you need the H2 console UX for local demos, enable it only on a developer machine and never in
production. The H2 console is protected by `H2ConsoleInterceptor` — it only accepts requests from
localhost (no JWT required, all attempts are audit-logged).

- One-off run (recommended for demos):
  - Linux / macOS:
    ```bash
    java -jar target/annasuraksha-5.0.0.jar --spring.h2.console.enabled=true --annasuraksha.seed.demo-data=true
    ```
  - Windows (PowerShell):
    ```powershell
    java -jar target\annasuraksha-5.0.0.jar --spring.h2.console.enabled=true --annasuraksha.seed.demo-data=true
    ```

- Dev profile: create `src/main/resources/application-dev.properties`:
  ```properties
  spring.h2.console.enabled=true
  annasuraksha.seed.demo-data=true
  ```
  Then run with `--spring.profiles.active=dev`.

- Helper script (Windows): `.\run-demo.ps1` — starts the app with H2 + demo data + known passwords.

Notes:
- H2 console: http://localhost:8081/h2-console, JDBC URL `jdbc:h2:mem:annasuraksha`, user `sa`, empty password.
- Set `DEMO_ADMIN_PW`, `DEMO_OFFICER_PW`, `DEMO_AUDIT_PW`, `DEMO_FPS_PW` env vars for reproducible credentials.
- Always disable H2 and demo seeding in staging/production.

Quick dev flow (mint a dev token)
-------------------------------
1. Start with H2 enabled (see above) and optionally set a bootstrap secret:
   ```bash
   export DEV_BOOTSTRAP_SECRET=verysecret
   java -jar target/annasuraksha-5.0.0.jar --spring.h2.console.enabled=true --annasuraksha.seed.demo-data=true
   ```

2. Mint a short-lived dev token:
   ```bash
   curl -sS -X POST http://localhost:8081/api/auth/dev-token \
     -H "Content-Type: application/json" \
     -H "X-BOOTSTRAP-SECRET: verysecret" \
     -d '{"email":"admin@local"}' | jq -r '.data.token'
   ```

3. Use the token to access protected routes:
   ```bash
   curl -H "Authorization: Bearer <TOKEN>" http://localhost:8081/api/admin/ledger/verify-all
   ```

Remember: never enable H2 or DEV_BOOTSTRAP_SECRET on shared or production machines.

Local demo helpers
------------------
Two helper scripts are included to simplify local demos:

- Bash (Linux/macOS): `scripts/dev-demo.sh`
  - Usage: `chmod +x scripts/dev-demo.sh && ./scripts/dev-demo.sh [BOOTSTRAP_SECRET] [EMAIL] [PORT]`
  - Starts the app with H2 enabled, mints a dev token using the provided secret, and prints the token + curl example.

- PowerShell (Windows): `scripts/dev-demo.ps1`
  - Usage: `.\scripts\dev-demo.ps1 [BootstrapSecret] [Email] [Port]`
  - Equivalent behavior for Windows developers; writes PID to `.dev_pid` and logs to `dev-app.log`.
Productization checklist (priority)
1. ✅ Done: demo seeding disabled by default; passwords auto-generated (env var overridable);
   JWT_SECRET and GROQ_API_KEY use env vars with placeholders.
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
