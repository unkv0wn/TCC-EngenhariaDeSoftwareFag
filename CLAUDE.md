# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RouteWise — a TCC (Trabalho de Conclusão de Curso) project. It started as a pure route optimizer (place waypoints on a map, get the optimal visiting order) and grew into a small fleet-management system: cadastros (vehicles, drivers, products, customers, …), orders, and routes generated from those orders.

The repo contains **three independently-developed pieces** — know which one you're in before editing:

- `backend/` — Java 21 + Spring Boot 3.2.5. The functional core plus the CRUD API. Persists to PostgreSQL (Spring Data JPA + Flyway). The route-computation pipeline (`RouteController` → `RouteOptimizerServiceImpl`) is still stateless; everything else (cadastros, orders, saved routes, settings) is database-backed.
- `FrontEnd-Next/` — Next.js 16 + React 19 + Tailwind 4. **This is the production frontend** — everything shipped ends up here. It **is wired to the backend**: `lib/apiClient.ts` + `services/*.ts` call the real REST API, and the per-page hooks (`useVehicles`, `useOrders`, …) own loading/error state. It also calls four third-party public APIs directly from the browser (ViaCEP, BrasilAPI, FIPE via BrasilAPI, Nominatim) — those are unrelated to the RouteWise backend.
- `frontend/` — React 18 + Vite + Leaflet. A test-only prototype that validated the routing core end-to-end. **Does not ship.** Treat as frozen/reference; don't build new features here.

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

### FrontEnd-Next (`FrontEnd-Next/`, production frontend)

```bash
pnpm install
pnpm dev             # :3000 — needs the backend on :8080 for every screen except the login form itself
pnpm build
pnpm lint            # eslint
```

Requires `NEXT_PUBLIC_API_URL` (browser → backend, e.g. `http://localhost:8080`), `BACKEND_URL` (Next server → backend, used only by the login route) and `JWT_SECRET` (must match `routewise.jwt.secret` in the backend) in `.env.local`. No test runner is configured — there are no tests here.

### Frontend test prototype (`frontend/`, does not ship)

```bash
npm install
npm run dev        # :5173, proxies /api -> :8080 (backend must be running)
npm run build      # tsc -b && vite build
npx vitest run     # run tests (no `test` script defined in package.json — invoke vitest directly)
npx vitest run src/__tests__/useWaypoints.test.ts   # single test file
npm run lint
```

### Full stack via Docker

The root `docker-compose.yml` builds and runs db + backend + frontend together (`docker compose up --build`) — see `README-DOCKER.md`. This is a *different* compose file from `backend/docker-compose.yml`, which only brings up Postgres for local dev.

## Architecture

### API surface (backend)

All under `/api`. Every CRUD controller follows the same shape — `GET` (list), `POST` (create), `PUT /{id}`, `DELETE /{id}`:

| Base path | Controller | Notes |
|-----------|-----------|-------|
| `/api/auth/login` | `AuthController` | `POST` only — see "Authentication" below |
| `/api/vehicles`, `/api/drivers`, `/api/units`, `/api/products`, `/api/customers`, `/api/refuelings`, `/api/payment-methods`, `/api/payment-conditions` | one controller each | plain CRUD |
| `/api/orders` | `OrderController` | CRUD + `PATCH /{id}/status` |
| `/api/routes` | `SavedRouteController` | `GET`, `GET /{id}`, `POST`, `PATCH /{id}/status`, `DELETE /{id}` |
| `/api/routes/compute`, `/api/routes/events/{requestId}`, `/api/routes/graph-stats` | `RouteController` | the computation pipeline |
| `/api/settings` | `CompanySettingsController` | `GET` + `PUT`, single row |

`RouteController` and `SavedRouteController` **share the `/api/routes` base path** — the computation endpoints are fixed sub-paths (`/compute`, `/events/…`, `/graph-stats`) while the saved-route ones are the root and `/{id}`. Adding another fixed sub-path to either is fine; adding a second `/{something}` path variable is not.

Errors come back as RFC 9457 problem details from `GlobalExceptionHandler`; the frontend's `apiClient.ts` reads `detail ?? title`.

### Authentication (read this before touching login)

Auth exists but is **not enforced anywhere on the backend**:

- `AuthController.login` issues a JWT for **any** email it receives — it never checks a password, and there is no user table.
- `JwtService` only *generates* tokens. There is no filter, no `spring-boot-starter-security` dependency and no `@PreAuthorize` — every `/api/**` endpoint is reachable unauthenticated.
- The real credential check lives in the frontend: `FrontEnd-Next/app/api/login/route.ts` compares against a hardcoded `DataMock` pair, then asks the backend for a token and stores it in an `httpOnly` cookie named `routewise`.
- `FrontEnd-Next/proxy.ts` (Next 16 renamed `middleware.ts` → `proxy.ts`) guards `matcher: ["/dashboard/:path*"]`, verifying the cookie's signature and expiry with `jose` against `JWT_SECRET`.
- `lib/apiClient.ts` sends **no** `Authorization` header and no credentials — the token never reaches the backend, which wouldn't check it anyway.

So the guard is navigational only. Don't describe this as a working auth system, and don't assume a request carries an authenticated user. The JWT secret is currently hardcoded in `application.properties` and duplicated in the root `docker-compose.yml`.

### Route computation flow (backend)

Two-phase REST + SSE handshake, all in `RouteController`:
1. `POST /api/routes/compute` validates the `RouteRequestDto`, stores it in an in-memory `ConcurrentHashMap<requestId, RouteRequestDto>` and returns `{ requestId }` immediately (202).
2. `GET /api/routes/events/{requestId}` pulls the pending request, opens an `SseEmitter` (`ISseEventService`) and dispatches the computation to a `TaskExecutor` backed by Java 21 virtual threads (`spring.threads.virtual.enabled=true`) — the HTTP thread returns immediately.
3. `RouteOptimizerServiceImpl` emits `PROCESSING`, fetches the OSRM `/table` duration matrix, runs A*, calls OSRM `/route` for geometry, then emits `COMPLETED` (or `ERROR`).

This pipeline touches no database. `pendingRequests` is the only server-side memory and self-cleans on lookup (`.remove`) — an entry whose SSE stream is never opened is never reclaimed.

Two distinct degradation paths, both deliberate:
- OSRM `/table` fails → the matrix is approximated as Haversine distance at a constant `FALLBACK_AVG_SPEED_KMH = 40.0`. This changes the *optimisation input*, not just the rendering.
- OSRM `/route` fails → the A*-optimised order is still returned, without geometry, with `osrmValidation` set to `UNAVAILABLE`.

### A* algorithm

`AStarWaypointOptimizer` solves the multi-waypoint TSP variant via A* with a bitmask state: `(currentWaypointIndex, visitedMask)`. Cost is travel duration in seconds from the OSRM matrix.

- **Heuristic:** for every unvisited node, the minimum *incoming* edge in the cost matrix (`computeMinIncoming`), plus — for `ROUND_TRIP` — a lower bound on the eventual return to origin. Admissible: each unvisited node must be entered at least once, and its cheapest possible entry is that minimum. It is **not** a Haversine-based heuristic; `HaversineUtil` backs the fallback matrix and the `validation` package, not the heuristic.
- **Tie-break:** among equal `f = g + h`, the larger `g` is expanded first. This never changes the cost converged to, only how many states get expanded. `AStarTieBreakBenchmarkTest` measures that, using the package-private `optimize(costs, mode, comparator)` overload that exists solely for tests.
- **Bounds:** `RouteRequestDto` validates 2–15 waypoints. State space is `N × 2^N` — ~490k states at 15, still sub-second; past ~20 it would need a heuristic solver instead.
- **Modes** (`RouteMode`), all three implemented: `ROUND_TRIP` (origin appended at the end), `OPEN_ROUTE`, `FIXED_START_END` (starts at 0, ends at `n-1`; an expansion guard forbids entering `n-1` until every other node is visited).

### OSM graph

`OsmGraphService` can load an OSM file into an in-memory graph at startup, but it is **off by default** — `routewise.osm.data-path` is blank in `application.properties`. It does not feed the A* phase (the cost matrix comes from OSRM); it only backs the read-only `GET /api/routes/graph-stats`.

### `com.routewise.validation` package

~50 standalone experiment/analysis classes (cost-weight sensitivity, empirical fuel consumption, tire wear by axle position, cargo occupancy, marginal contribution, fleet simulations) that produce the reports under `docs/validation-reports/`. These are TCC research artifacts, **not** part of the request pipeline — they reuse `AStarWaypointOptimizer` unmodified rather than being called by it. Each has a `mvn compile exec:java -Dexec.mainClass=...` entry point documented in its class Javadoc. Design rationale lives in `docs/superpowers/specs/`.

### Database (`backend/`)

PostgreSQL via Spring Data JPA + Flyway. `backend/docker-compose.yml` runs Postgres 16 on host port **5434** (dev credentials `routewise`/`routewise`, db `routewise`). Connection settings read `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`, defaulting to that compose setup.

Flyway owns the schema — `backend/src/main/resources/db/migration/`, currently `V1__init.sql` (empty baseline) through `V14__create_company_settings.sql`. `spring.jpa.hibernate.ddl-auto=validate`: Hibernate checks entities against the schema but never generates DDL. **Every schema change goes through a new migration file** — never edit an applied one.

13 entities / 11 repositories exist today: `Vehicle`, `Driver`, `Unit`, `Product`, `Customer`, `Refueling`, `PaymentMethod`, `PaymentCondition`, `Order` (+ `OrderItem`, `OrderHistoryEntry`), `SavedRoute`, `CompanySettings`. Services are `I<Name>Service` + `<Name>ServiceImpl`, all annotated `@Transactional` at class level with `@Transactional(readOnly = true)` on reads.

Referential deletes are guarded in the service layer, not by FK cascade: deleting a Unit in use by a Product, or a Vehicle/Driver with refuelings, is blocked.

`Order.status` and `SavedRoute.status` are plain `String`s, and the valid-transition maps are **duplicated**: `VALID_STATUS_TRANSITIONS` in `OrderServiceImpl` mirrors the same constant in `hooks/useOrders.ts`. Change one, change the other. Illegal transitions raise `InvalidOrderStatusTransitionException` / `InvalidRouteStatusTransitionException`.

### FrontEnd-Next data flow

`lib/apiClient.ts` is the thin fetch wrapper (reads `NEXT_PUBLIC_API_URL`, throws `ApiError` carrying the parsed problem-detail message). `services/<resource>.ts` wraps the endpoints per resource; `hooks/use<Resource>.ts` holds the list plus `isLoading`/`error` and exposes create/update/delete that refresh from the server. Login is the exception: it goes through the Next route handler `app/api/login/route.ts` because of the `httpOnly` cookie.

Route generation (`components/rotas/GerarRotaScreen.tsx`) is the one screen that composes several of these: it pulls orders, customers, drivers, vehicles and settings, builds the waypoint list, and drives `useRouteComputation`, which POSTs to `/api/routes/compute` and consumes the SSE stream via `EventSource`. The route origin is the company address from `useSettings`, falling back to the hardcoded `DEPOT_COORD` in `lib/routeConfig.ts` when settings haven't loaded.

`lib/routePrototype.ts` is leftover prototype data (city coordinates, jitter) still imported by `GerarRotaScreen` and `GeocodeConfirmModal` for approximate positioning — its nearest-neighbour "fake route" helpers are no longer the computation path.

Third-party browser-side APIs, all free and key-less: `services/viaCep.ts` (CEP), `services/brasilApi.ts` (CNPJ), `services/fipe.ts` (vehicle brands/models), `services/geocoding.ts` (Nominatim, with BrasilAPI CEP v2 as fallback and manual pin-drop as last resort).

Form validation is Zod schemas under `lib/validations/`, one file per resource; the `<Resource>FormData` types they infer are what the hooks and services pass around.

### Frontend test prototype data flow (`frontend/`)

`useWaypoints` manages map state; `routeApi.ts` posts to `/api/routes/compute`; `useSseListener` opens the `EventSource` and drives UI state through `PROCESSING`/`COMPLETED`/`ERROR`.

## Testing

Backend has 10 JUnit 5 classes, **all** under `algorithm/` and `validation/` — the A* optimizer, its tie-break benchmark, Haversine, and the empirical cost calculators. There are **no tests for any controller, service or repository**, and no `@SpringBootTest` context test (which would need a running Postgres or Testcontainers — not wired in). `FrontEnd-Next/` has no tests and no runner. The only frontend tests (`useWaypoints`, `useSseListener`, Vitest) live in the frozen `frontend/` prototype.

Keep this in mind when changing CRUD or status-transition logic: nothing will catch a regression for you.

## Coding conventions

These are enforced project-wide:

- Indentation: 2 spaces, all languages.
- TypeScript interfaces prefixed `I`, types prefixed `T` (`TWaypoint`, `TRouteEvent`) — this convention is only followed in `frontend/`. `FrontEnd-Next/` never adopted it (`interface Vehicle`, `type ListView`, no prefix) — match the file you're in. The `I` prefix *is* the rule for Java service interfaces (`IOsrmClient`, `IOrderService`).
- Strict equality only (`===`/`!==`); no TypeScript `any`.
- Java: typed `catch` blocks (no bare exception swallowing), SLF4J/Logback for logging — never `System.out.println`.
- `frontend/` is a frozen test prototype — don't add features there; new UI work goes in `FrontEnd-Next/`.
- **Language:** code is always in English — variable/function/class names, file names, commit messages. Comments are mixed in practice (the older backend core and `algorithm/` are English; newer services and hooks carry PT-BR comments) — match the file you're in. User-facing text (UI labels, buttons, error messages shown in the UI) is always PT-BR.

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

Raw Tailwind utility classes composed with a `cn()` helper (clsx/tailwind-merge) — no component library (no shadcn/Radix). Primitives live in `components/ui/`:

`Button`, `Input`, `Textarea`, `Checkbox`, `Divider`, `SocialButton`, `Modal`, `ConfirmDialog`, `PageHeader`, `CreateButton`, `SearchInput`, `EmptyState`, `LoadingState`, `ViewToggle`, `Pagination`, `ActionsMenu`, `SearchableSelect`, `PreventPageZoomGesture`, `toast/*`.

There is **no `Select` primitive** — searchable dropdowns use `SearchableSelect`, and simple filters use a styled native `<select>` inside the feature folder (`OrderStatusFilter`, `RouteStatusFilter`, `VehicleFilterSelect`, `CustomerTypeFilter`).

Every cadastro page (`VehiclesPageContent`, `DriversPageContent`, `ProductsPageContent`, `UnitsPageContent`, `CustomersPageContent`, `RefuelingsPageContent`, `PaymentMethodsPageContent`, `PaymentConditionsPageContent`, `OrdersPageContent`) is built from these — extend them rather than reintroducing a one-off overlay/header/search/empty-state per screen. The recurring shape is `<Feature>PageContent` + `<Feature>FormModal` + `<Feature>Table` (+ `<Feature>Card`/`<Feature>Grid` where a card view exists).

- **Buttons** (`Button.tsx`): 3 variants — `primary` (`bg-primary-600 text-white hover:bg-primary-700`), `secondary` (`bg-white border border-gray-200 hover:bg-gray-50`), `ghost` (`bg-transparent hover:bg-gray-100`). Shared base: `rounded-lg px-4 py-2.5 text-sm font-medium transition-all duration-150 ease-out`, focus via `focus-visible:ring-2 focus-visible:ring-primary-500`. No formal "danger" variant — produced ad hoc by overriding classes (see `ConfirmDialog.tsx`). Icon-only buttons: `rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600`.
- **Cards**: `rounded-xl border border-gray-200 bg-white`, no shadow by default (`hover:shadow-md` only on hover, e.g. `VehicleCard.tsx`).
- **Modals/toasts** ("floating surfaces"): `rounded-2xl border border-gray-100 bg-white shadow-xl shadow-gray-900/10`. `Modal.tsx` is the shared primitive (`size="md"|"lg"`, `Esc` closes, initial focus moves into the panel, `role="dialog"`); `ConfirmDialog.tsx` builds a delete-style confirmation on top of it. Still no enter/exit animation on either.
- **Inputs** (`Input.tsx`/`Textarea.tsx`): field base `rounded-lg border border-gray-200 bg-white px-3.5 py-2.5 text-sm`, focus ring `focus:ring-2 focus:ring-primary-500/15 focus:border-primary-500` (translucent, unlike buttons' solid ring). Error state: `border-danger-300` + `focus:ring-danger-500/10`. `Textarea` mirrors `Input`'s label/error handling with `min-h-[80px] resize-y`.
- **`rounded-*` scale by hierarchy**: `rounded-md` (small icon buttons) → `rounded-lg` (default: buttons/inputs) → `rounded-xl` (cards) → `rounded-2xl` (modals/toasts) → `rounded-full` (pill badges only).
- **Icons**: `lucide-react` everywhere except the hand-drawn Google/Microsoft SVGs in `SocialButton.tsx`. Default size `h-4 w-4`; decorative icons get `aria-hidden="true"`.
- **Leaflet**: map components (`RouteMap`, `GeocodePinMap`) are imported with `next/dynamic` and `ssr: false` — Leaflet touches `window` at module load.
- **Dark mode**: components do **not** use Tailwind `dark:` classes anywhere. Dark mode only repaints `body` background/foreground via the CSS variables above; cards/modals/inputs/toasts hardcode light-mode classes (`bg-white`, `text-gray-900`) and will not adapt. Treat this as an open gap, not an intentional constraint.
- Known inconsistencies worth knowing before extending: `CreateButton` bypasses the shared `Button` component with a slightly different type style (`font-bold text-[13.5px]` vs. `font-medium text-sm`) — deliberately isolated there, so aligning it is a one-file change; input error borders use the `danger-*` token but error text uses Tailwind's native `text-red-600` instead of `text-danger-600`.

## Documentation map

Everything lives under `docs/` — there is no `REQUIREMENTS.md` at the repo root:

- `docs/REQUIREMENTS.md` — user stories and acceptance criteria.
- `docs/SYSTEM_REQUIREMENTS.md` — full SRS (functional/non-functional requirements, traceability).
- `docs/USE_CASES.md` — detailed use case specs.
- `docs/COST_AND_SCHEDULE.md` — schedule and cost analysis.
- `docs/ESTADO-ATUAL.md` — current-state snapshot of the build.
- `docs/formulas-a-star-e-custo.md`, `docs/MATH_VALIDATION_REVIEW.md`, `docs/refactor-math-result/` — the maths behind A* and the cost model, plus its review.
- `docs/superpowers/specs/` — design specs per feature (dashboard, toast, vehicle/unit/product/customer/driver/refueling cadastros, empirical cost experiments).
- `docs/superpowers/plans/` — implementation plans derived from those specs, broken into verifiable phases.
- `docs/validation-reports/` — generated output of the `com.routewise.validation` experiments (Markdown + GeoJSON).
- `TODO.md` — module roadmap (cadastros → orders → invoicing → routes) and the decisions already taken.
- `README-DOCKER.md` — running the whole stack with Docker.
