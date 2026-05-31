# AnnaSuraksha v5.1 — PDS Fraud Intelligence Platform

[![Build and Test](https://github.com/DeepxD-code/AnnaSuraksha/actions/workflows/maven.yml/badge.svg)](https://github.com/DeepxD-code/AnnaSuraksha/actions/workflows/maven.yml)

AnnaSuraksha is a production-grade fraud intelligence platform for India's Public Distribution System (PDS). It combines **deterministic rule-based detection**, **multi-layered ghost analysis**, **geospatial impossible-travel verification**, **Merkle-tree data integrity proofs**, and **AI-powered (Groq LLM) explanations** with automatic deterministic fallback.

---

## What's New in v5.1

| Upgrade | Detail |
|---------|--------|
| **16 scoring signals** (was 12) | Added phone dedup, family composition anomaly, registration velocity — weights still sum to 1.0 |
| **6 ghost detection layers** (was 3) | Phone duplicates, family composition, registration velocity |
| **11 simulation scenarios** (was 8) | FPS cloning network, ration card recycling, ghost family network |
| **CRITICAL risk level** | New tier at ≥0.85 for compound fraud patterns |
| **Confidence scoring** | Per-score confidence based on evidence count + signal strength |
| **Detection verification** | Every simulation reports pass/fail based on actual scored risk |
| **Fraud metrics API** | `/api/fraud/metrics`, `/distribution`, `/top-risk`, `/simulate/accuracy` |
| **50 demo beneficiaries** (was 15) | 40 legitimate across 29 states + 10 embedded fraud cases |
| **Swagger/OpenAPI** | Interactive docs at `/swagger-ui.html`, spec at `/v3/api-docs` |
| **Accuracy tracking** | Simulation engine auto-validates detection and reports accuracy % |

---

## Quick facts

- **Server port:** 8081 (default)
- **Demo DB:** H2 in-memory (`jdbc:h2:mem:annasuraksha`)
- **H2 console:** `/h2-console` (user: `sa`, pass: empty) — localhost-only, no JWT required in dev
- **Demo seeding:** disabled by default; enable with `--annasuraksha.seed.demo-data=true`
- **API docs:** `/swagger-ui.html` (OpenAPI 3.0)

---

## Quick start (demo)

```bash
# 1. Set env vars for reproducible passwords (optional)
set DEMO_ADMIN_PW=admin123
set DEMO_OFFICER_PW=officer123
set DEMO_AUDIT_PW=audit123
set DEMO_FPS_PW=fps123

# 2. Build
mvn -DskipTests clean package

# 3. Run with demo data
java -jar target\annasuraksha-5.1.0.jar --spring.h2.console.enabled=true --annasuraksha.seed.demo-data=true

# 4. Open
open http://localhost:8081
```

---

## Demo accounts

| Email | Role | Env var for password |
|---|---|---|
| admin@annasuraksha.gov.in | ROLE_ADMIN | DEMO_ADMIN_PW |
| officer@up.gov.in | ROLE_GOVT_OFFICER | DEMO_OFFICER_PW |
| auditor@cag.gov.in | ROLE_AUDITOR | DEMO_AUDIT_PW |
| fps@mh.gov.in | ROLE_FPS_OPERATOR | DEMO_FPS_PW |

Passwords are auto-generated (16-char Base64) if env vars not set. Set env vars before starting the app for reproducible demos.

---

## Fraud Detection Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    API Layer (Controllers)                    │
│  /api/fraud/*  /api/simulate/*  /api/alerts/*  /api/ledger/* │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│              FraudRiskScoringService — 16 Signals             │
│  dupAadhaar(0.20)  travel(0.18)  crossState(0.12)           │
│  rationAnomaly(0.09)  catMismatch(0.06)  dealerDiv(0.06)    │
│  dupVoterId(0.04)  dupPhone(0.04)  nightClaim(0.04)         │
│  familyAnomaly(0.03)  claimFreq(0.03)  bulkBurst(0.03)      │
│  regVelocity(0.02)  multiShop(0.02)  newFps(0.01)           │
│  districtRisk(0.01)                                          │
│  ─── compound floors + confidence scoring                    │
└──────────┬──────────────────┬──────────────────┬──────────────┘
           ▼                  ▼                  ▼
┌──────────────────┐ ┌──────────────┐ ┌──────────────────────┐
│ GhostDetection   │ │Impossible    │ │ FraudExplanation     │
│ 6 Layers         │ │Travel        │ │ Groq LLM + Fallback  │
│ L1-L6            │ │33 States     │ │ 16-signal context    │
└────────┬─────────┘ │Haversine +   │ └──────────────────────┘
         │           │Multi-modal   │
         ▼           └──────────────┘
┌──────────────────────────────────────────────────────────────┐
│              FraudMetricsService                             │
│  Accuracy tracking · Scenario verification · Metrics API     │
└──────────────────────────────────────────────────────────────┘
```

### 16 Scoring Signals (weights sum to 1.0)

| # | Signal | Weight | Severity Floor | Description |
|---|--------|--------|----------------|-------------|
| 1 | Duplicate Aadhaar | 0.20 | ≥0.70 | Same `aadhaarHash` used by >1 beneficiary |
| 2 | Impossible Travel | 0.18 | ≥0.75 | Geospatial impossibility via Haversine |
| 3 | Cross-State Fraud | 0.12 | — | Non-ONORC claiming outside home state |
| 4 | Ration Anomaly | 0.09 | — | Ricekg exceeds NFSA cap (familySize × 7) |
| 5 | Category Mismatch | 0.06 | — | AAY for <2 person household |
| 6 | Dealer Diversion | 0.06 | — | % flagged FPS deliveries |
| 7 | Duplicate Voter ID | 0.04 | — | Ghost detection layer 1 signal |
| 8 | Duplicate Phone | 0.04 | — | Same phone across multiple beneficiaries |
| 9 | Night Claim | 0.04 | — | Claim between 22:00–05:00 |
| 10 | Family Anomaly | 0.03 | — | Oversized family or single-person over-entitlement |
| 11 | Claim Frequency | 0.03 | — | Z-score >2 relative to state mean |
| 12 | Bulk Burst | 0.03 | ≥0.80 (w/ night) | Claims >36 in period |
| 13 | Reg. Velocity | 0.02 | — | >5 registrations at same FPS in 90 days |
| 14 | Multi-Shop | 0.02 | — | Claims across >3 different FPS shops |
| 15 | New FPS Shop | 0.01 | — | Recently reassigned FPS |
| 16 | District Risk | 0.01 | — | Base risk of district (placeholder) |

**Compound floors:**
- `dupAadhaar + impossibleTravel` → min 0.90 (CRITICAL)
- `dupAadhaar + dupPhone` → min 0.85 (CRITICAL)
- `bulkBurst + nightClaim` → min 0.80 (HIGH)

### 6-Layer Ghost Detection

| Layer | Name | Detection Logic |
|-------|------|----------------|
| L1 | Aadhaar Duplicate | Same hash across beneficiary records |
| L1b | Phone Duplicate | Same phone across 2+ beneficiaries in different states |
| L2 | Pattern Anomaly | Entitlement exceeds NFSA max; AAY for undersized family |
| L3 | Velocity / Cross-State | Non-ONORC interstate claim; ONORC claim in <24h |
| L4 | Family Composition | Single-person over-entitlement; oversized families |
| L5 | Registration Velocity | FPS registered >5 beneficiaries in 90-day window |
| L6 | Fraud Risk Score | Composite 16-signal ML-free scoring engine |

---

## Full API Reference

### Fraud Analytics (`/api/fraud/*`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/fraud/score/{id}` | OFFICER/AUDITOR | Score single beneficiary |
| POST | `/api/fraud/score-all` | OFFICER/AUDITOR | Batch score all beneficiaries |
| GET | `/api/fraud/high-risk-beneficiaries` | OFFICER/AUDITOR | Top HIGH/CRITICAL risk cases |
| GET | `/api/fraud/suspicious-dealers` | OFFICER/AUDITOR | FPS dealers with high diversion rates |
| GET | `/api/fraud/duplicate-identities` | OFFICER/AUDITOR | Aadhaar hash collisions |
| GET | `/api/fraud/impossible-travel` | OFFICER/AUDITOR | Geospatially impossible claims |
| GET | `/api/fraud/ghost-flags` | OFFICER/AUDITOR | Run ghost detection |
| GET | `/api/fraud/summary` | OFFICER/AUDITOR | Fraud summary dashboard |
| GET | `/api/fraud/metrics` | OFFICER/AUDITOR | Detection accuracy & metrics |
| GET | `/api/fraud/top-risk` | OFFICER/AUDITOR | Top N risk scores (descending) |
| GET | `/api/fraud/distribution` | OFFICER/AUDITOR | Risk level distribution |

### Simulations (`/api/simulate/*`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/simulate/scenarios` | ADMIN/OFFICER | List all 11 scenarios |
| POST | `/api/simulate/fraud-case` | ADMIN/OFFICER | Run one scenario |
| POST | `/api/simulate/fraud-case/all` | ADMIN/OFFICER | Run all 11 + accuracy report |
| GET | `/api/simulate/accuracy` | ADMIN/OFFICER | Current accuracy metrics |
| DELETE | `/api/simulate/cleanup` | ADMIN/OFFICER | Clean up simulation data |

### Supply Chain (`/api/supply-chain/*`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/supply-chain/warehouse-load` | OFFICER/FPS | Register warehouse load |
| POST | `/api/supply-chain/dispatch` | OFFICER/FPS | Dispatch shipment |
| POST | `/api/supply-chain/fps-receive` | OFFICER/FPS | Record FPS receipt |
| GET | `/api/supply-chain/flag-summary` | OFFICER/FPS | Discrepancy summary |

### Alerts (`/api/alerts/*`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/alerts/stream` | OFFICER/AUDITOR | SSE real-time alert stream |
| GET | `/api/alerts/active` | OFFICER/AUDITOR | Active alerts |
| GET | `/api/alerts/history` | OFFICER/AUDITOR | Full alert history |
| POST | `/api/alerts/{id}/acknowledge` | OFFICER/AUDITOR | Acknowledge alert |
| GET | `/api/alerts/stats` | OFFICER/AUDITOR | Alert statistics |

### Blockchain Ledger (`/api/ledger/*`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/ledger/snapshot` | OFFICER/AUDITOR | Create Merkle snapshot |
| GET | `/api/ledger/snapshot/{id}/proof/{benId}` | OFFICER/AUDITOR | Get Merkle proof |
| GET | `/api/ledger/snapshot/{id}/verify/{benId}` | OFFICER/AUDITOR | Verify proof |
| GET | `/api/ledger/snapshot/{id}/anchor` | OFFICER/AUDITOR | On-chain anchor metadata |

### Admin (`/api/admin/*`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/admin/ledger/verify/{id}` | ADMIN | Verify snapshot anchor |
| GET | `/api/admin/ledger/verify-all` | ADMIN | Verify all anchors |
| POST | `/api/admin/ledger/anchor/{id}` | ADMIN | Re-anchor snapshot |
| GET | `/api/admin/audit/recent` | ADMIN | Recent audit logs |

### Public
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/swagger-ui.html` | None | OpenAPI interactive docs |
| GET | `/v3/api-docs` | None | OpenAPI JSON spec |
| GET | `/api/stats` | None | Public stats |
| GET | `/api/transparency/*` | None | Transparency data |
| POST | `/api/auth/login` | None | Get JWT token |
| POST | `/api/auth/dev-token` | Dev only | Mint dev token |

---

## Simulation Scenarios (11 total — ground truth for accuracy)

| Scenario | Fraud Type | Detection | Expected Score |
|----------|-----------|-----------|----------------|
| GHOST_BENEFICIARY | Duplicate Aadhaar across states | L1 ghost, HIGH risk | ≥0.70 |
| IMPOSSIBLE_TRAVEL | RJ→KL claim in 1.5h (2,200km) | Geospatial impossibility | ≥0.75 |
| DEALER_DIVERSION | 80% flagged FPS deliveries | Dealer diversion signal | HIGH |
| CATEGORY_FRAUD | AAY for 1-person household | L2 pattern anomaly | ≥0.50 |
| BULK_CLAIM_BURST | 5 claims in 72h, 3 shops | Multi-shop + burst | HIGH |
| SUPPLY_CHAIN_LEAK | 40% grain missing (500→300kg) | Supply discrepancy | Flagged |
| CROSS_STATE_NON_ONORC | GJ non-ONORC claiming in MH | Cross-state NFSA S.7 violation | ≥0.50 |
| BIOMETRIC_DUPLICATE | Same Aadhaar in TN, KA, KL | L1 triple duplicate | ≥0.70 |
| FPS_CLONING_NETWORK | 3 ghosts at one FPS in 10 days | L5 velocity + scoring | HIGH |
| RATION_CARD_RECYCLING | Deceased card reused | L1 duplicate + AAY | ≥0.70 |
| GHOST_FAMILY_NETWORK | 5 fakes sharing one phone | L1b phone dup + L5 velocity | HIGH |

Run all scenarios to verify accuracy: `POST /api/simulate/fraud-case/all`

---

## Dataset

| Entity | Count |
|--------|-------|
| Beneficiaries | 50 (40 legitimate across 29 states + 10 fraud cases) |
| FPS Deliveries | 150 (3 per beneficiary, ~15% flagged) |
| Supply Chain Shipments | 3 |
| Demo Users | 4 (Admin, UP Officer, CAG Auditor, MH FPS Operator) |
| States Represented | 29 of 33 modeled |
| Ghost Flags (post-seed) | Auto-raised by 6-layer detection |

---

## Detection Accuracy

The system uses **deterministic rule-based detection** (not ML) — a deliberate choice for government PDS systems where every flag must be auditable and traceable to specific NFSA 2013 provisions.

**Measured via simulation verification:** Each of the 11 scenarios has known ground truth. After running a scenario, the engine scores the created beneficiary and compares the result against expected thresholds. Accuracy is reported as `(scenarios where detection verified) / (total scenarios) × 100`.

Access via: `GET /api/fraud/metrics` or `GET /api/simulate/accuracy`

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.5 |
| Build | Maven (wrapper: `mvnw`) |
| Database | H2 in-memory (demo); Postgres-ready schema |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security + JWT (jjwt 0.12.5) + BCrypt(12) |
| API Docs | SpringDoc OpenAPI 2.5.0 (Swagger UI) |
| LLM | Groq API (`llama-3.3-70b-versatile`) with deterministic fallback |
| Real-time | SSE (Server-Sent Events) via Spring MVC async |
| Merkle Proofs | SHA-256 binary Merkle tree |
| Blockchain | Hyperledger Fabric (mock for demo, real SDK stub) |
| Observability | Actuator + Micrometer + Prometheus |

---

## Key Implementation Details

- **FraudRiskScoringService:** 16 deterministic features extracted per beneficiary, weighted sum with non-linear compound floors. Four-tier cache (CRITICAL=2min, HIGH=5min, MEDIUM=15min, LOW=60min).
- **GhostDetectionService:** 6-layer pipeline running L1→L5 sequentially. Each flag carries `estimatedMonthlyLossRs`. `applyFlags()` marks beneficiaries as GHOST in DB.
- **ImpossibleTravelDetector:** Haversine formula on 33 state capitals. Multi-modal minimum travel time: rail at 120km/h or air at 600km/h + 3h overhead.
- **FraudMetricsService:** Tracks simulation accuracy across runs. Reports precision, risk distribution, monthly/annual loss estimates.
- **FraudExplanationService:** Groq LLM with structured JSON context. Deterministic fallback builds 3-sentence explanation from features.

---

## OpenAPI / Swagger

After starting the app, visit:
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **JSON spec:** http://localhost:8081/v3/api-docs

All endpoints are documented with request/response schemas and auth requirements.

---

## Security & Privacy

- H2 console localhost-restricted and audit-logged (disabled by default)
- Demo seeding disabled by default (enable via CLI flag only)
- JWT secret and Groq API key use env vars with dev-only defaults
- Passwords auto-generated with SecureRandom; env var overridable for demos
- BCrypt(12) for password hashing
- H2ConsoleInterceptor records all access attempts in audit log

---

## Productization Roadmap

1. **Short term:** Postgres + Flyway migrations, Redis caching, rate limiting with Redis
2. **Medium term:** Kafka event stream for scoring, signed snapshot notarization, SSO
3. **Long term:** Multi-state deployment, state-specific weight profiles, active learning

---

## Demo Script (2-minute)

1. Login at http://localhost:8081 as admin@annasuraksha.gov.in
2. Run all simulations: `POST /api/simulate/fraud-case/all`
3. Check accuracy: `GET /api/simulate/accuracy`
4. View fraud summary: `GET /api/fraud/summary`
5. Check metrics dashboard: `GET /api/fraud/metrics`
6. Browse API docs: http://localhost:8081/swagger-ui.html

---

## License

Verify third-party dependency licenses before commercial use. MIT recommended for open-source demos.
