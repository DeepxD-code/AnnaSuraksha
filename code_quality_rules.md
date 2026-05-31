# Code Quality Standards
Version: 6.0 — FINAL

---

## SYSTEM CONTEXT (READ FIRST)

This file is part of a 5-file system. See SYSTEM_INDEX.md for the full map.

| Agent   | Sections they own and enforce                               |
|---------|-------------------------------------------------------------|
| AGENT-3 | All sections except Testing and Documentation               |
| AGENT-4 | Error Handling section — use as fix standard                |
| AGENT-5 | Testing section — governs all test output                   |
| AGENT-6 | Documentation (JSDoc) section — governs all doc output      |

Do not mix responsibilities — each agent enforces only its owned sections.

---

## Clean Code Principles

- **DRY** — Don't Repeat Yourself
- **KISS** — Keep It Simple, Stupid
- **SOLID** — Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
- **YAGNI** — You Aren't Gonna Need It — do not build for hypothetical future requirements

---

## Naming Conventions

| Element           | Convention        | Example                          |
|-------------------|-------------------|----------------------------------|
| Variables         | camelCase         | `userBalance`                    |
| Functions         | camelCase         | `fetchUserData()`                |
| Components        | PascalCase        | `UserDashboard`                  |
| Folders           | kebab-case        | `user-profile/`                  |
| Constants         | UPPER_CASE        | `MAX_RETRY_COUNT`                |
| Types/Interfaces  | PascalCase        | `UserPayload`, `ApiResponse`     |
| Enums             | PascalCase values | `UserRole.ADMIN`                 |
| Boolean variables | is/has/can prefix | `isLoading`, `hasAccess`         |
| Event handlers    | handle prefix     | `handleSubmit`, `handleChange`   |
| Custom hooks      | use prefix        | `useAuthUser`, `useDebounce`     |

---

## TypeScript Rules

- Always define explicit return types on all exported functions — no inferred returns.
- Never use `any` — use `unknown` and narrow, or define a proper interface.
- Use `interface` for object shapes; use `type` for unions, intersections, and aliases.
- Enable `strict: true` in `tsconfig.json` — non-negotiable.
- Use `readonly` for properties that must not be mutated after construction.
- Use optional chaining (`?.`) and nullish coalescing (`??`) — no manual null checks.
- Use `satisfies` operator to validate object literals against a type without widening.
- Avoid type assertions (`as`) — if needed, add a comment explaining why.

---

## Function Design

- One function = one responsibility.
- Functions must be ≤ 30 lines — split into helpers if longer.
- Maximum 3 parameters — if more needed, group into a typed options object.
- Maximum 2 levels of nesting — use guard clauses (early returns) to flatten.
- Prefer pure functions — no hidden side effects or mutations of external state.
- Async functions must always be awaited or have their promise explicitly handled.

---

## File Size Limits

| File type            | Soft limit | Hard limit | Action when exceeded          |
|----------------------|------------|------------|-------------------------------|
| Service / logic file | 200 lines  | 300 lines  | Split into focused sub-files  |
| Component file       | 150 lines  | 250 lines  | Extract child components      |
| Test file            | 400 lines  | 600 lines  | Split by feature group        |
| Config file          | 100 lines  | 150 lines  | Split by domain               |

---

## Documentation (JSDoc) — AGENT-6 enforces this section

Every exported function, class, interface, and type must have JSDoc:

```ts
/**
 * Fetches a user by ID from the database.
 * @param userId - UUID of the target user
 * @returns User object or null if not found
 * @throws {DatabaseError} if the DB connection fails
 * @throws {NotFoundError} if no user exists with that ID
 * @example
 * const user = await getUserById('abc-123');
 */
async function getUserById(userId: string): Promise<User | null> { ... }
```

- Comment on **why**, not **what** — code shows what; comments explain intent.
- Every `@throws` must be documented — callers need to know what to catch.
- Include `@example` on all public-facing service methods.

---

## Import Organization

Enforce via ESLint `import/order` rule in this sequence:

1. Node built-ins (`fs`, `path`, `crypto`)
2. External packages (`express`, `zod`, `react`)
3. Internal absolute imports (`@/modules/auth`, `@/shared/logger`)
4. Relative imports (`./utils`, `../models`)
5. Type-only imports (`import type { User } from './types'`)

Separate each group with a blank line. Never mix groups.

---

## Error Handling — AGENT-4 uses this section as its fix standard

- Always use `try/catch` on all async operations.
- Log with structured context: `logger.error('getUserById failed', { userId, error })`.
- Never swallow errors silently — `catch (e) {}` is forbidden.
- Never expose stack traces to API clients — log internally, return a safe message.
- Use centralized custom error hierarchy:

```ts
class AppError extends Error {
  constructor(
    public message: string,
    public statusCode: number,
    public code: string
  ) { super(message); }
}

class NotFoundError extends AppError {
  constructor(resource: string) {
    super(`${resource} not found`, 404, 'NOT_FOUND');
  }
}
class ValidationError extends AppError {
  constructor(message: string) { super(message, 422, 'VALIDATION_ERROR'); }
}
class AuthError extends AppError {
  constructor() { super('Unauthorized', 401, 'UNAUTHORIZED'); }
}
```

All API error responses must use the standard envelope:
`{ success: false, data: null, error: "Human-readable message", meta: { timestamp, requestId, code } }`

---

## Security Coding Rules

- Never hardcode secrets — always use `process.env` via the config module.
- Validate and sanitize all user input at the API boundary using Zod.
- Parameterize all database queries — string interpolation in SQL is forbidden.
- Use `helmet` middleware and explicit CORS config in all Express/Node APIs.
- Avoid `eval()`, `new Function()`, `innerHTML` (use `textContent`), dynamic `require()`.
- Hash all passwords with bcrypt, minimum cost factor 12 — never store or log plaintext.
- Sanitize all user-supplied content before HTML rendering to prevent XSS.
- Rate-limit all auth endpoints — see architecture_rules.md for limits.

---

## Accessibility (Frontend) — a11y

- All interactive elements must have accessible labels (`aria-label` or visible text).
- Use semantic HTML — `<button>` for actions, `<a>` for navigation, `<nav>`, `<main>`, `<section>`.
- Every image must have a descriptive `alt` (or `alt=""` for decorative images).
- Keyboard navigation must work for all interactive elements — no click-only interactions.
- Color contrast ratio must meet WCAG AA: 4.5:1 for body text, 3:1 for large text.
- Use `aria-*` attributes only when semantic HTML is insufficient.

---

## Linting & Formatting Tools

| Tool        | Purpose                          | Config file          |
|-------------|----------------------------------|----------------------|
| ESLint      | Code quality & rule enforcement  | `.eslintrc.json`     |
| Prettier    | Consistent formatting            | `.prettierrc`        |
| Husky       | Pre-commit hook enforcement      | `.husky/`            |
| lint-staged | Run linters on staged files only | in `package.json`    |
| tsc         | Type checking (`--noEmit`)       | `tsconfig.json`      |

- All configs must live in the repo root.
- Pre-commit hooks must block commits failing lint, formatting, or type checks.
- CI must also run lint + type check independently of pre-commit hooks.

---

## Dependency Management

- Always commit the lockfile — never `.gitignore` `package-lock.json` or `yarn.lock`.
- Never install deprecated or unmaintained packages — check npm warnings.
- Pin major versions — use `^` for minor/patch, never `*` or `latest`.
- Run `npm audit` in CI — fail on high or critical vulnerabilities.
- Run `depcheck` in CI — fail on unused dependencies.
- Keep `dependencies` and `devDependencies` correctly separated.

---

## Git Workflow

- Branch naming: `feat/`, `fix/`, `refactor/`, `chore/`, `docs/`, `test/` prefixes required.
- PR size limit: ≤ 400 lines changed — split larger PRs by module.
- Every PR must reference a task or issue ID in the description.
- Merging to `main` requires: passing CI + minimum 1 approved review.
- Never force-push to `main` or `develop`.
- Delete branches after merge.

---

## Commit Message Convention (Conventional Commits)

Format: `<type>(<scope>): <short description>`

| Type       | When to use                              |
|------------|------------------------------------------|
| `feat`     | New feature                              |
| `fix`      | Bug fix                                  |
| `refactor` | Code change that isn't fix or feature    |
| `chore`    | Build, tooling, config, dependency       |
| `docs`     | Documentation only                       |
| `test`     | Adding or modifying tests                |
| `perf`     | Performance improvement                  |
| `ci`       | CI/CD pipeline changes                   |
| `revert`   | Reverting a previous commit              |

Examples:
```
feat(auth): add JWT refresh token rotation
fix(api): handle null userId in getUserById
refactor(billing): extract invoice generator to service layer
ci: add depcheck step to PR pipeline
```

---

## Performance

Avoid:
- Unnecessary re-renders — use `React.memo`, `useMemo`, `useCallback` where profiling justifies it
- Expensive synchronous operations in render paths or request handlers
- N+1 database query patterns — batch queries or use eager loading
- Blocking the event loop with synchronous file/network operations
- Unbounded arrays returned from any endpoint — always paginate

Use:
- Memoization for pure, expensive functions
- Async/await for all I/O — never `sync` variants of Node APIs in production
- Caching at the service layer — see architecture_rules.md caching strategy
- Lazy loading for large modules and routes (`React.lazy`, dynamic `import()`)

---

## Testing — AGENT-5 owns and enforces this section

- Minimum coverage: **70%** (target **80%+** for service/domain layer)
- Unit tests: every exported function and service method
- Integration tests: every API route
- E2E tests: every critical user-facing flow

| Tool       | Use case                      |
|------------|-------------------------------|
| Jest       | Unit & integration tests      |
| Vitest     | Unit tests in Vite projects   |
| Playwright | E2E browser testing           |

- Test files live next to source files: `user.service.test.ts`
- Follow AAA: **Arrange → Act → Assert**
- Mock all external dependencies in unit tests (DB, Redis, third-party APIs)
- After every AGENT-4 bug fix: a regression test must be added proving that bug cannot recur

---

## Refactoring Triggers

Refactor immediately when any of these are true:
- A function exceeds 30 lines
- A file exceeds 300 lines
- The same logic appears in 2+ places
- Cyclomatic complexity exceeds 10
- A reviewer flags readability or maintainability

---

## Code Review Checklist

- [ ] No unused variables or dead code
- [ ] No hardcoded secrets or credentials
- [ ] All async functions have error handling
- [ ] Input validation at all API boundaries
- [ ] No `any` types in TypeScript
- [ ] No `as` assertions without explanatory comment
- [ ] Test coverage maintained or improved
- [ ] No duplicate logic
- [ ] Performance regressions checked
- [ ] JSDoc on all exported symbols
- [ ] All interactive UI elements are accessible
- [ ] No new deprecated dependencies added
- [ ] Lockfile committed and up to date
- [ ] Branch name follows naming convention
- [ ] Commits follow Conventional Commits format
