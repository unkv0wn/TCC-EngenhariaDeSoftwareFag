# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RouteWise — a TCC (Trabalho de Conclusão de Curso) project: a stateless urban route optimizer. The user places 2–10 waypoints on a map; the backend computes the optimal visiting order with a self-implemented A* over an OSM-derived graph, cross-validates/geometrizes the result via the public OSRM API, and streams progress back over SSE.

The repo contains **three independently-versioned pieces** — know which one you're in before editing:

- `backend/` — Java 21 + Spring Boot 3.2.5. No auth. The functional core. Persists to PostgreSQL (Spring Data JPA + Flyway) as of the `feat-connect-database` work — see "Database" below; the request pipeline itself (`RouteController` and everything under it) remains stateless.
- `FrontEnd-Next/` — Next.js 16 + React 19 + Tailwind 4. **This is the production frontend** — everything shipped ends up here. Adds auth (login) and a dashboard the original app never had. **Not wired to the RouteWise backend yet** — `services/auth.ts` and all cadastro data (vehicles, units, products, customers) are mocked in per-page hooks (`useVehicles`, `useUnits`, `useProducts`, `useCustomers`), state resets on reload/navigation. Do not assume RouteWise backend endpoints exist for login/dashboard/cadastros unless you've checked `backend/src/main/java/com/routewise/controller/` first. The customer form does call two real third-party public APIs (`services/viaCep.ts`, `services/brasilApi.ts`) to autofill CEP/CNPJ — those are unrelated to the RouteWise backend and not affected by the "not wired" note above.
- `frontend/` — React 18 + Vite + Leaflet. A test-only prototype used to validate the routing core end-to-end against the backend. **Does not ship to production.** Treat as frozen/reference; don't build new features here.

## Commands

### Backend (`backend/`)

```bash
docker compose -f backend/docker-compose.yml up -d   # start local Postgres (:5434), needed before the two below
mvn clean install                 # compile + run all tests
mvn spring-boot:run               # run on :8080
mvn test                          # run tests only
mvn test -Dtest=AStarWaypointOptimizerTest        # single test class
mvn test -Dtest=AStarWaypointOptimizerTest#methodName  # single test method
mvn package                       # build jar -> target/routewise-backend-*.jar
```

Standalone validation/experiment scripts under `com.routewise.validation` (not part of the request pipeline) are run individually, e.g.:
```bash
mvn compile exec:java -Dexec.mainClass=com.routewise.validation.EmpiricalCostValidationExperiment
```

### Frontend test prototype (`frontend/`, does not ship)

```bash
npm install
npm run dev        # :5173, proxies /api -> :8080 (backend must be running)
npm run build       # tsc -b && vite build
npx vitest run       # run tests (no `test` script defined in package.json — invoke vitest directly)
npx vitest run src/__tests__/useWaypoints.test.ts   # single test file
npm run lint
```

### FrontEnd-Next (`FrontEnd-Next/`, production frontend)

```bash
pnpm install
pnpm dev             # :3000, no backend dependency today
pnpm build
pnpm lint
```

## Architecture

### Request flow (backend)

Two-phase REST + SSE handshake, all in `RouteController`:
1. `POST /api/routes/compute` validates the `RouteRequestDto`, stores it in an in-memory `ConcurrentHashMap<requestId, RouteRequestDto>`, and returns `{ requestId }` immediately (202).
2. `GET /api/routes/events/{requestId}` pulls the pending request, opens an `SseEmitter` (`ISseEventService`), and dispatches the actual computation to a `TaskExecutor` backed by Java 21 virtual threads (`spring.threads.virtual.enabled=true`) — the HTTP thread returns immediately, computation runs async.
3. The computation (`IRouteOptimizerService`) runs A*, then calls OSRM (`IOsrmClient`) to validate/geometrize the route, and pushes `PROCESSING` → `COMPLETED`/`ERROR` events through the emitter.
4. If OSRM fails, the service degrades gracefully and returns the raw A* result instead of erroring out.

There's no persistent state anywhere — the `pendingRequests` map is the only server-side memory, and it self-cleans on lookup (`.remove`).

### A* algorithm

`AStarWaypointOptimizer` solves the multi-waypoint TSP subproblem via A* with a bitmask state: `(currentWaypointIndex, visitedMask)`. Cost is OSRM-matrix travel duration in seconds; the heuristic is Haversine distance to the farthest unvisited waypoint (`HaversineUtil`). Complexity is `O(n² · 2ⁿ)`, bounded by the 10-waypoint UI limit. Three modes live in `RouteMode`: `ROUND_TRIP`, `OPEN_ROUTE`, `FIXED_START_END`.

### OSM graph

`OsmGraphService` loads OSM data into an in-memory graph at startup (used by the A* phase and exposed read-only via `GET /api/routes/graph-stats`).

### `com.routewise.validation` package

A separate, standalone family of experiment/analysis classes (cost-weight sensitivity, empirical cost validation by vehicle profile, marginal contribution analysis, etc.) used to produce the reports under `docs/validation-reports/`. These are TCC research artifacts, not part of the production request pipeline — they reuse `AStarWaypointOptimizer` unmodified rather than being called by it. Each has a `mvn compile exec:java -Dexec.mainClass=...` entry point documented in its class Javadoc. Design rationale for these lives in `docs/superpowers/specs/`.

### Frontend test prototype data flow (`frontend/`)

`useWaypoints` manages map state; `routeApi.ts` posts to `/api/routes/compute`; `useSseListener` opens the `EventSource` against `/api/routes/events/{requestId}` and drives UI state through the `PROCESSING`/`COMPLETED`/`ERROR` events.

### Database (`backend/`)

PostgreSQL, via Spring Data JPA + Flyway. `backend/docker-compose.yml` runs a local Postgres 16 on host port **5434** (dev credentials `routewise`/`routewise`, db `routewise`) — start it before `mvn spring-boot:run`. Connection settings in `application.properties` read from `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` env vars, defaulting to that local compose setup.

Flyway owns the schema — migrations live in `backend/src/main/resources/db/migration/` (`V1__init.sql` is the baseline, currently empty). `spring.jpa.hibernate.ddl-auto=validate`: Hibernate checks entities against the schema but never generates DDL itself; every schema change goes through a new Flyway migration file.

No entities/repositories exist yet — this is connection infrastructure only, added ahead of the first Cadastro (Vehicles, per `TODO.md`). Context-loading Spring tests (`@SpringBootTest`) will need a running Postgres (or Testcontainers, not yet wired in) once they're introduced.

## Coding conventions

These are enforced project-wide:

- Indentation: 2 spaces, all languages.
- TypeScript interfaces prefixed `I` (`IOsrmClient`-style pattern also used in Java), types prefixed `T` (`TWaypoint`, `TRouteEvent`) — this convention is only followed in `frontend/`. `FrontEnd-Next/` never adopted it (`interface Vehicle`, `type ListView`, etc., no prefix) — match the file you're in, don't mix the two styles within `FrontEnd-Next/`.
- Strict equality only (`===`/`!==`); no TypeScript `any`.
- Java: typed `catch` blocks (no bare exception swallowing), SLF4J/Logback for logging — never `System.out.println`.
- No auth on the `backend`/`frontend` pair by design — don't add session state there without an explicit decision to unfreeze that constraint. Persistence (PostgreSQL) was unfrozen for `backend` — see "Database" below; `frontend` stays stateless.
- `frontend/` is a frozen test prototype — don't add new features to it; new UI work goes in `FrontEnd-Next/`.
- **Language:** code is always in English — variable/function/class names, file names, comments, commit messages. Only user-facing text (UI labels, button text, error messages shown in the UI) is PT-BR.

## Color palette (`FrontEnd-Next`)

Source of truth: `FrontEnd-Next/app/globals.css` (CSS custom properties, each with a 50–900 shade scale, exposed to Tailwind 4 via `@theme inline` as `bg-primary-500`, `text-gray-700`, etc.). Don't hardcode hex values in components — always reference the token.

| Token | Role | Representative shade (500/600) |
|-------|------|-------------------------------|
| `gray` | Neutral — text/background/borders | `#6b7280` / `#4b5563` |
| `primary` | Brand accent (purple) | `#8b5cf6` / `#7c3aed` |
| `secondary` | Support to primary (light/baby blue) | `#0ea5e9` / `#0284c7` |
| `success` | Positive state (green) | `#10b981` / `#059669` |
| `danger` | Error/destructive state (red) | `#ef4444` / `#dc2626` |
| `warning` | Caution state (yellow) | `#f59e0b` / `#d97706` |

`--background`/`--foreground` flip between `gray-50`/`gray-900` (light) and `gray-950`/`gray-50` (dark) via `prefers-color-scheme: dark`.

## Component style patterns (`FrontEnd-Next`)

Raw Tailwind utility classes composed with a `cn()` helper (clsx/tailwind-merge) — no component library (no shadcn/Radix). Primitives live in `components/ui/`: `Button`, `Input`, `Select`, `Textarea`, `Checkbox`, `Divider`, `SocialButton`, `toast/*` — plus the cadastro-page primitives extracted from Vehicles when Products/Units/Customers were added: `Modal`, `ConfirmDialog` (built on `Modal`), `PageHeader`, `CreateButton`, `SearchInput`, `EmptyState`, `ViewToggle`. Every cadastro page (`VehiclesPageContent`, `ProductsPageContent`, `UnitsPageContent`, `CustomersPageContent`) is built from these — extend them rather than reintroducing a one-off overlay/header/search/empty-state per screen.

- **Buttons** (`Button.tsx`): 3 variants — `primary` (`bg-primary-600 text-white hover:bg-primary-700`), `secondary` (`bg-white border border-gray-200 hover:bg-gray-50`), `ghost` (`bg-transparent hover:bg-gray-100`). Shared base: `rounded-lg px-4 py-2.5 text-sm font-medium transition-all duration-150 ease-out`, focus via `focus-visible:ring-2 focus-visible:ring-primary-500`. No formal "danger" variant — produced ad hoc by overriding classes (see `ConfirmDialog.tsx`). Icon-only buttons: `rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600`.
- **Cards**: `rounded-xl border border-gray-200 bg-white`, no shadow by default (`hover:shadow-md` only on hover, e.g. `VehicleCard.tsx`).
- **Modals/toasts** ("floating surfaces"): `rounded-2xl border border-gray-100 bg-white shadow-xl shadow-gray-900/10` — this pairing is the convention for anything overlaid on the page. `components/ui/Modal.tsx` is the shared primitive (`size="md"|"lg"`, `Esc` closes, initial focus moves into the panel, `role="dialog"`); `ConfirmDialog.tsx` builds a delete-style confirmation on top of it. Still no enter/exit animation on either.
- **Page header / create action / search / empty state**: `PageHeader` (title + subtitle + action slot), `CreateButton` (the "+ Novo X" button — keeps the ad hoc `text-[13.5px] font-bold` styling documented below in one place instead of copied per page), `SearchInput` (icon + `type="search"` input, no visible `<label>`), `EmptyState` (dashed box, distinguishes "nothing cadastrado yet" from "no results for this search" — each `PageContent` passes the right message). `ViewToggle` (cards/table switch, exports the `ListView` type) lives here too, shared by every cadastro that offers both views; Units doesn't (list-only, no toggle).
- **Inputs** (`Input.tsx`/`Select.tsx`/`Textarea.tsx`): field base `rounded-lg border border-gray-200 bg-white px-3.5 py-2.5 text-sm`, focus ring `focus:ring-2 focus:ring-primary-500/15 focus:border-primary-500` (translucent, unlike buttons' solid ring). Error state: `border-danger-300` + `focus:ring-danger-500/10`. `Textarea` mirrors `Input`'s label/error handling with `min-h-[80px] resize-y`, no `startIcon`/`endAdornment`.
- **`rounded-*` scale by hierarchy**: `rounded-md` (small icon buttons) → `rounded-lg` (default: buttons/inputs) → `rounded-xl` (cards) → `rounded-2xl` (modals/toasts) → `rounded-full` (pill badges only).
- **Icons**: `lucide-react` everywhere except the hand-drawn Google/Microsoft SVGs in `SocialButton.tsx`. Default size `h-4 w-4`; decorative icons get `aria-hidden="true"`.
- **Dark mode**: components do **not** use Tailwind `dark:` classes anywhere — dark mode only repaints `body` background/foreground via the CSS variables above. Cards/modals/inputs/toasts hardcode light-mode classes (`bg-white`, `text-gray-900`) and will not adapt. Treat this as an open gap, not an intentional constraint, when touching these components.
- Known inconsistencies worth knowing about before extending: `CreateButton` bypasses the shared `Button` component with a slightly different type style (`font-bold text-[13.5px]` vs. the component's `font-medium text-sm`) — deliberately isolated there rather than fixed, so aligning it to `Button` is a one-file change if it's ever done; input error borders use the `danger-*` token but error text uses Tailwind's native `text-red-600` instead of `text-danger-600`.

## Documentation map

- `REQUIREMENTS.md` — user stories and acceptance criteria.
- `docs/SYSTEM_REQUIREMENTS.md` — full SRS (functional/non-functional requirements, traceability).
- `docs/USE_CASES.md` — detailed use case specs.
- `docs/COST_AND_SCHEDULE.md` — schedule and cost analysis.
- `docs/superpowers/specs/` — design specs for in-progress features (e.g. dashboard, vehicle registration, empirical cost validation experiment).
- `docs/superpowers/plans/` — implementation plans derived from the specs above, broken into verifiable phases (e.g. the products/units/customers cadastro build-out).
- `docs/validation-reports/` — generated output of the `com.routewise.validation` experiments.
