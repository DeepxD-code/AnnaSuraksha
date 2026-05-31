# System Architecture Rules
Version: 6.0 — FINAL

---

## SYSTEM CONTEXT (READ FIRST)

This file is part of a 5-file system. See SYSTEM_INDEX.md for the full map.

| Agent   | How this file is used                                              |
|---------|--------------------------------------------------------------------|
| AGENT-1 | Governs all decisions in Stages 1–2 and the Stage 5 review gate   |
| AGENT-2 | Governs file structure and module design in Stage 3 planning       |
| AGENT-3 | Governs layering, data flow, API standards, and config in Stage 4  |

All architectural choices made by AGENT-1 must comply with the rules in this file.
If AGENT-1 deviates from any rule, it must justify the deviation in `architectureNotes`.

---

## Architectural Principles

- **Separation of Concerns** — each layer has one distinct job, no overlap
- **Single Responsibility Principle** — one reason to change per module
- **Loose Coupling** — modules communicate through interfaces, not implementations
- **High Cohesion** — related logic lives together in the same module
- **Defense in Depth** — security enforced at every layer, not just the edge
- **Fail Fast** — validate config and dependencies at startup, not at runtime

---

## Layered Architecture

```
Presentation Layer      →  UI, HTTP routes, API gateway, request parsing
Application Layer       →  orchestration, use cases, DTO mapping, transactions
Domain Layer            →  business rules, entities, domain events, invariants
Infrastructure Layer    →  database, Redis, S3, external APIs, message queues
Security Layer          →  JWT validation, RBAC, rate limiting, input sanitization
```

### Layer Responsibility Matrix

| Layer          | Owns                                              | Forbidden from                     |
|----------------|---------------------------------------------------|------------------------------------|
| Presentation   | HTTP handling, request parsing, response format   | Business logic, DB access          |
| Application    | Use case coordination, DTO mapping, transactions  | Direct DB access, HTTP concerns    |
| Domain         | Business rules, domain entities, invariants       | DB drivers, HTTP, external APIs    |
| Infrastructure | DB access, Redis, S3, third-party APIs            | Business logic, HTTP handling      |
| Security       | JWT validation, RBAC, rate limiting, sanitization | Business logic, DB access          |

**Strictly forbidden cross-layer access:**
- UI → DB directly (must flow through all intermediate layers)
- Presentation → Domain (must go through Application layer)
- Domain → Infrastructure (Domain defines interfaces; Infrastructure implements them)

---

## Module Structure

Each feature is self-contained. No module imports from another module's internals.
Modules may only communicate via well-defined interfaces or shared types in `src/shared/`.

```
src/
  modules/
    auth/
      controller.ts       ← HTTP: parse request, call service, format response
      service.ts          ← Application: business logic and orchestration
      model.ts            ← Domain: entity, Prisma/Mongoose schema
      dto.ts              ← Zod schemas for input validation
      middleware.ts       ← Auth guards, role checks, request validators
      routes.ts           ← Route definitions (imports controller + middleware only)
      errors.ts           ← Domain-specific error subclasses for this module
      types.ts            ← Module-local TypeScript types and interfaces
      tests/
        auth.unit.test.ts
        auth.integration.test.ts
    billing/
    notifications/
    analytics/

  shared/
    errors.ts             ← Base error classes: AppError, NotFoundError, etc.
    logger.ts             ← Structured logger instance (Pino or Winston)
    types.ts              ← Shared TypeScript types used across modules
    middleware/
      requestId.ts        ← Attach UUID to every incoming request
      errorHandler.ts     ← Global error handler middleware

  config/
    index.ts              ← Validate and export all env vars at startup (fail fast)
    database.ts           ← DB connection and pool setup
    redis.ts              ← Redis client setup

  app.ts                  ← Express app assembly (no business logic)
  server.ts               ← HTTP server entry point with graceful shutdown
```

---

## Data Flow

```
Client Request
  → Security Layer (rate limit check → JWT validation → RBAC check)
    → Presentation (controller: parse request → validate DTO via Zod)
      → Application (service: coordinate use case → apply business rules)
        → Domain (entity: enforce invariants)
          → Infrastructure (repository: DB read/write)
            ← Response flows back up the same chain unchanged
```

---

## Dependency Injection

Do not use global singletons in services. Inject all dependencies explicitly:

```ts
// BAD — tight coupling, untestable
class AuthService {
  private db = new PrismaClient(); // hidden, cannot be mocked
}

// GOOD — dependencies injected, fully mockable in tests
class AuthService {
  constructor(
    private readonly userRepository: UserRepository,
    private readonly logger: Logger,
    private readonly config: Config
  ) {}
}
```

- Define interfaces for all infrastructure dependencies — allows AGENT-5 to mock them.
- Use a DI container (`tsyringe`, `inversify`) on large projects; manual injection on small ones.

---

## API Standards

### Response Envelope

All API responses must follow this exact shape:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "meta": {
    "timestamp": "2026-03-15T10:00:00Z",
    "requestId": "uuid-v4"
  }
}
```

Error response:
```json
{
  "success": false,
  "data": null,
  "error": "Human-readable error message",
  "meta": {
    "timestamp": "2026-03-15T10:00:00Z",
    "requestId": "uuid-v4",
    "code": "VALIDATION_ERROR"
  }
}
```

### API Versioning

- Version all APIs via URL prefix: `/api/v1/`, `/api/v2/`
- Never modify a versioned endpoint in a breaking way — create a new version
- Mark deprecated versions with `Deprecation: true` response header
- Support previous version for at least 6 months after new version ships

### HTTP Method Semantics

| Method   | Semantics             | Idempotent |
|----------|-----------------------|------------|
| `GET`    | Read only             | Yes        |
| `POST`   | Create resource       | No         |
| `PUT`    | Full replace          | Yes        |
| `PATCH`  | Partial update        | No         |
| `DELETE` | Remove resource       | Yes        |

Required status codes: `200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`, `422`, `429`, `500`

### Health Check Endpoints (required on every service)

```
GET /health  → { status: "ok", uptime: 123, timestamp: "..." }
GET /ready   → 200 if service can accept traffic; 503 if not (DB not ready etc.)
```

- Must never require authentication
- Must respond within 200ms
- AGENT-1 must flag as rejected in Stage 5 review if these are missing

---

## CORS Configuration

Never use `cors()` with no options — that allows all origins:

```ts
app.use(cors({
  origin: process.env.ALLOWED_ORIGINS?.split(',') ?? [],
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
  allowedHeaders: ['Content-Type', 'Authorization', 'X-Request-ID'],
  credentials: true,
  maxAge: 86400
}));
```

- Never allow `*` in production
- Preflight (`OPTIONS`) requests must respond with `204`

---

## Security Architecture

### Authentication & Authorization

- JWT access tokens: 15 minute maximum lifetime.
- JWT refresh tokens: 7 day maximum lifetime, rotated on every use.
- Store refresh tokens in `httpOnly`, `Secure`, `SameSite=Strict` cookies — never in `localStorage`.
- Implement RBAC — define roles at domain layer, enforce in security middleware.
- Never trust client-supplied roles — re-validate server-side on every request.

### Input Validation

- Validate all incoming data at Presentation layer using Zod before it reaches Application.
- Use strict mode — reject requests with unexpected or extra fields.
- Sanitize string inputs that will render to HTML.

### Rate Limiting

- Default: 100 requests/minute per IP on public endpoints.
- Auth endpoints (login, register, password reset): 10 requests/minute per IP.
- Store counters in Redis for distributed enforcement.
- Return `429 Too Many Requests` with `Retry-After` header on breach.

---

## Graceful Shutdown (required on every service)

Every service must handle `SIGTERM` and `SIGINT` signals cleanly:

```ts
const shutdown = async (signal: string): Promise<void> => {
  logger.info(`${signal} received — beginning graceful shutdown`);
  server.close(async () => {
    await db.$disconnect();
    await redisClient.quit();
    logger.info('Graceful shutdown complete');
    process.exit(0);
  });
  // Force kill if graceful shutdown takes more than 10 seconds
  setTimeout(() => {
    logger.error('Graceful shutdown timed out — forcing exit');
    process.exit(1);
  }, 10_000);
};

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT',  () => shutdown('SIGINT'));
```

AGENT-1 Stage 5 review must flag missing graceful shutdown as a rejection reason.

---

## Environment Configuration

- All secrets and environment-specific config must live in `.env` files.
- Never commit `.env` — always commit `.env.example` with placeholders and descriptions.
- Validate all required env vars at startup — crash immediately if any are missing (fail fast).
- Maintain separate configs for: `development`, `test`, `staging`, `production`.

```ts
// src/config/index.ts
const throwMissing = (key: string): never => {
  throw new Error(`Missing required environment variable: ${key}`);
};

export const config = {
  dbUrl:     process.env.DATABASE_URL  ?? throwMissing('DATABASE_URL'),
  jwtSecret: process.env.JWT_SECRET    ?? throwMissing('JWT_SECRET'),
  redisUrl:  process.env.REDIS_URL     ?? throwMissing('REDIS_URL'),
  port:      Number(process.env.PORT)  || 3000,
  nodeEnv:   process.env.NODE_ENV      ?? 'development',
  allowedOrigins: process.env.ALLOWED_ORIGINS ?? throwMissing('ALLOWED_ORIGINS'),
};
```

---

## Database Rules

- Normalize to at least 3NF unless read performance requires intentional denormalization.
- Index all columns used in `WHERE`, `JOIN`, and `ORDER BY`.
- Use migrations for all schema changes — never alter production schema manually.
- Configure connection pooling — size based on expected concurrency.
- Never store passwords, tokens, or secrets in plaintext.
- Use transactions for all operations that must be atomic (multi-table writes).
- Soft-delete records where history matters — use `deletedAt` timestamp column.

---

## Caching Strategy

| Data type               | Cache target     | TTL          | Invalidation trigger       |
|-------------------------|------------------|--------------|----------------------------|
| User session data       | Redis            | 15 min       | Logout or token rotation   |
| Frequently read config  | In-memory        | App lifetime | Service restart            |
| Public API responses    | CDN / Redis      | 5–60 min     | Content update event       |
| DB query results        | Redis            | 1–10 min     | Write to the same entity   |

- Use cache-aside: check cache → miss → query DB → populate cache.
- Always define and implement cache invalidation **before** implementing caching.
- Never cache user-specific data in a shared CDN layer.

---

## Frontend Error Boundaries

Every React application must wrap top-level and critical route sections:

```tsx
class ErrorBoundary extends React.Component<Props, { hasError: boolean }> {
  static getDerivedStateFromError() { return { hasError: true }; }
  componentDidCatch(error: Error, info: React.ErrorInfo) {
    logger.error('UI Error Boundary caught error', { error, info });
    Sentry.captureException(error, { extra: info });
  }
  render() {
    return this.state.hasError ? <FallbackUI /> : this.props.children;
  }
}
```

- Wrap each major route and each critical widget independently.
- Always render a meaningful fallback UI — never a blank screen.
- Log all caught errors to Sentry.

---

## State Management (Frontend)

| App size | Solution                          |
|----------|-----------------------------------|
| Small    | React Context + useReducer        |
| Medium   | Zustand                           |
| Large    | Redux Toolkit                     |

- Keep server state separate from UI state — use TanStack Query (React Query) for server state.
- Never store sensitive data (tokens, PII) in `localStorage` or `sessionStorage` — use `httpOnly` cookies.
- Never store derived data in state — compute it from source state.

---

## Monorepo vs Polyrepo

| Scenario                              | Use                         |
|---------------------------------------|-----------------------------|
| Shared types/utils across services    | Monorepo (Turborepo / Nx)   |
| Fully independent services            | Polyrepo                    |
| Frontend + Backend sharing types      | Monorepo                    |

If monorepo: define `packages/shared` for all cross-service types and utilities.
Never copy-paste shared code across packages — import from `@repo/shared`.

---

## Containerization (Docker)

Every service must ship with a multi-stage `Dockerfile`:

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production
COPY . .
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
USER node
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s CMD wget -qO- http://localhost:3000/health || exit 1
CMD ["node", "dist/server.js"]
```

- Multi-stage builds only — never ship dev dependencies in production image.
- Never run as root — always `USER node`.
- Always define `HEALTHCHECK` pointing to `/health`.
- No secrets in Docker images — inject via environment at runtime.

---

## CI/CD Pipeline

```
Push / PR opened
  → Lint (ESLint + Prettier check)
  → Type check (tsc --noEmit)
  → Unit tests
  → Integration tests
  → npm audit (fail on high/critical)
  → depcheck (fail on unused dependencies)
  → Build
  → Docker build (verify image compiles)
  → (on merge to main) → Deploy to staging
  → (manual approval gate) → Deploy to production
```

- All CI secrets stored in CI secrets manager — never in code.
- Merging to `main` requires: full CI pass + minimum 1 approved review.

---

## Scalability

- Design for 10× current expected load from day one.
- Prefer stateless services — store all session state externally (Redis, DB).
- Use message queues (BullMQ / RabbitMQ) for async non-blocking operations.
- Horizontally scale application servers — vertically scale DB only as last resort.
- Apply DB read replicas for heavy read workloads.
- Paginate all list endpoints — never return unbounded result sets.

---

## Observability

Every production service must instrument all three pillars:

| Pillar         | What to capture                                |
|----------------|------------------------------------------------|
| Logging        | All requests, errors, key business events      |
| Metrics        | Latency p50/p95/p99, error rate, throughput    |
| Error tracking | Stack traces with user context and frequency   |
| Tracing        | Distributed trace ID across all service hops   |

### Standards

- Log in structured JSON — never plain strings in production.
- Include `requestId` in every log line — generated at request entry, propagated downstream.
- Set up alerts for: error rate spike, p95 latency breach, disk/memory threshold.

### Recommended Stack

| Tool             | Purpose                         |
|------------------|---------------------------------|
| Sentry           | Error tracking                  |
| Prometheus       | Metrics collection              |
| Grafana          | Metrics dashboards and alerts   |
| Winston / Pino   | Structured application logging  |
| OpenTelemetry    | Distributed tracing             |

---

## AGENT-1 Stage 5 Review Checklist (Architecture)

AGENT-1 must check all of the following during the final review gate:

- [ ] Layer boundaries respected throughout — no forbidden cross-layer access
- [ ] All API routes follow response envelope format
- [ ] `/health` and `/ready` endpoints present and unauthenticated
- [ ] Graceful shutdown implemented with SIGTERM/SIGINT handling
- [ ] Rate limiting applied — standard endpoints and stricter on auth routes
- [ ] JWT stored in httpOnly cookies — not localStorage
- [ ] All env vars validated at startup via config module
- [ ] Docker multi-stage build present — runs as non-root user
- [ ] CI/CD pipeline covers lint, type check, test, audit, build
- [ ] Observability: logging, metrics, error tracking all instrumented
- [ ] No unbounded list endpoints — all paginated
- [ ] Database indexes defined for all query patterns
