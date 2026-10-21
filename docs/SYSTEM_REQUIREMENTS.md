# SYSTEM REQUIREMENTS SPECIFICATION (SRS)

> **Project:** RouteWise — Urban Route Optimizer and Fleet Management
> **Version:** 2.0
> **First issued:** 2026-04-14 (v1.0)
> **Revised:** 2026-10-14
> **Status:** ACTIVE

---

## 1. INTRODUCTION

RouteWise is a full-stack academic application for urban delivery logistics. A manager
registers the operation's data (vehicles, drivers, products, customers), records orders,
invoices them, and generates a delivery route whose visiting order is computed by a
self-implemented **exact A\*** solver. The route is dispatched to a driver, who executes
it from an Android app and registers what happens on the street. Progress and incidents
flow back to the manager's tracking screen.

The route-computation pipeline itself remains stateless; everything else is persisted in
PostgreSQL.

### 1.1 Changes from v1.0

v1.0 described only the routing core, and several of its requirements were contradicted by
the implementation. They are corrected here rather than removed, because the differences
matter for how the work is evaluated:

| v1.0 said | Reality | Now |
|-----------|---------|-----|
| 2–10 waypoints | 2–15 | FR-09 |
| Two route modes | Three (`FIXED_START_END` exists and is tested) | FR-10 |
| A* over an in-memory OSM graph | A* over the OSRM duration matrix; the OSM graph is disabled by default | FR-11, FR-20 |
| Haversine heuristic | Sum of minimum incoming edges, plus a return bound for `ROUND_TRIP` | FR-13 |
| **Nearest-neighbour TSP heuristic** | **Exact A\*** with bitmask state — the result is optimal, not approximate | **FR-12** |

The last row is the substantive one. v1.0 claimed an approximation where the system
implements an exact solver with an admissible heuristic; the correction raises what the
project claims rather than lowering it.

---

## 2. FUNCTIONAL REQUIREMENTS (FR)

### 2.1 Registration

| ID | Category | Requirement | Source Story |
|----|----------|-------------|--------------|
| FR-01 | Vehicles | The system **shall** maintain vehicles with plate (old or Mercosul format), model, brand, year, colour, load capacity in kilograms and fuel type. | US-01 |
| FR-02 | Drivers | The system **shall** maintain drivers with full name, CPF (check-digit validated), phone, licence number, category and expiry, and an active/inactive status. | US-02 |
| FR-03 | Products | The system **shall** maintain products with SKU, name, unit of measure, unit price and weight; the unit **shall** be a registered entity, not a fixed enumeration. | US-03 |
| FR-04 | Customers | The system **shall** maintain customers and suppliers in a single register distinguished by a type field, with full address and geographic coordinate. | US-04 |
| FR-05 | Customers | The system **shall** offer automatic lookup of CEP, CNPJ and address geocoding from public third-party services, leaving every field editable afterwards. | US-04 |
| FR-06 | Refuellings | The system **shall** record refuellings per vehicle with odometer, litres and price per litre, deriving kilometres since the previous refuelling at creation time. | US-05 |
| FR-07 | Integrity | The system **shall** refuse to delete a registry still referenced by another (a unit used by a product, a vehicle or driver with refuellings), returning HTTP 409. | US-01, US-03 |
| FR-08 | Settings | The system **shall** maintain one company record whose address and coordinate are the origin and return point of every route. | US-06 |

### 2.2 Route computation

| ID | Category | Requirement | Source Story |
|----|----------|-------------|--------------|
| FR-09 | Input | The system **shall** accept 2–15 geographic coordinates via `POST /api/routes/compute` and reject other sizes with HTTP 400 and a problem-detail body. | US-10 |
| FR-10 | Input | The system **shall** accept a `routeMode` of `ROUND_TRIP`, `OPEN_ROUTE` or `FIXED_START_END`. | US-10 |
| FR-11 | Cost model | The system **shall** build the N×N cost matrix from the OSRM `/table` service, using travel duration in seconds. | US-10 |
| FR-12 | Algorithm | The system **shall** determine the visiting order with an **exact A\*** search over the state `(current waypoint, bitmask of visited waypoints)`, returning a provably optimal order for the given matrix — not an approximation. | US-10 |
| FR-13 | Algorithm | The A\* heuristic **shall** be the sum, over unvisited waypoints, of each one's minimum incoming edge, plus — for `ROUND_TRIP` — a lower bound on the return to origin. The heuristic **shall** be admissible. | US-10 |
| FR-14 | Algorithm | Among states with equal `f = g + h`, the search **shall** expand the larger `g` first. This **shall not** change the cost converged to, only the number of states expanded. | US-10 |
| FR-15 | Algorithm | In `FIXED_START_END`, the search **shall** forbid entering the final waypoint until every other waypoint has been visited. | US-10 |
| FR-16 | Geometry | The system **shall** call OSRM `/route` for the optimised order and return the resulting geometry as a GeoJSON LineString, per segment and for the whole route. | US-11 |
| FR-17 | Degradation | If OSRM `/table` is unavailable, the system **shall** approximate the matrix from Haversine distance at a fixed average speed and continue. This degradation changes the optimisation input and **shall** be logged. | US-10 |
| FR-18 | Degradation | If OSRM `/route` is unavailable, the system **shall** return the optimised order without geometry and mark `osrmValidation` as `UNAVAILABLE`. The request **shall not** fail. | US-11 |
| FR-19 | Events | The system **shall** stream computation lifecycle events over SSE at `GET /api/routes/events/{requestId}`, publishing `PROCESSING`, `COMPLETED` and `ERROR`. | US-12 |
| FR-20 | Graph stats | The system **may** load an OSM file into memory at startup and expose its statistics at `GET /api/routes/graph-stats`. This is **disabled by default** and does not feed the solver. | — |

### 2.3 Orders

| ID | Category | Requirement | Source Story |
|----|----------|-------------|--------------|
| FR-21 | Orders | The system **shall** maintain orders with a customer, items (product, quantity, unit price), discount, freight, notes and a timestamped status history. | US-07 |
| FR-22 | Lifecycle | The system **shall** enforce exactly these order transitions and reject any other with HTTP 409: `aguardando → faturado \| cancelado`; `faturado → em_rota \| cancelado`; `em_rota → entregue \| faturado` (the return to `faturado` representing goods brought back to the company); `entregue` and `cancelado` are terminal. Once goods are `em_rota` they are no longer cancellable. | US-08 |
| FR-23 | Invoicing | The system **shall** allow invoicing several pending orders at once, moving them to `faturado`. | US-09 |
| FR-24 | Eligibility | Only orders in `faturado` **shall** be selectable for route generation. | US-09, US-10 |
| FR-24a | Lifecycle | A route **shall** record which orders each of its stops delivers, so that a delivery can reach the orders it closed. | US-10, US-19 |
| FR-24b | Lifecycle | Creating a route **shall** move its orders from `faturado` to `em_rota`, so that the same order cannot be dispatched on a second route while the first is merely planned. | US-10 |
| FR-24c | Lifecycle | A delivery event **shall** move the orders of **that stop only** to `entregue`. | US-19 |
| FR-24d | Lifecycle | Concluding or deleting a route **shall** return its still-`em_rota` orders to `faturado`, so that an undelivered order re-enters the pool instead of remaining in a state with no route in progress. | US-13, US-19 |
| FR-24e | Lifecycle | Propagation **shall** skip any order not in the expected source state rather than failing the request: a delivery registered in the field **shall not** fail because the order changed in the panel meanwhile. Routes saved before FR-24a **shall** be skipped silently. | US-19, US-20 |

### 2.4 Route dispatch and tracking

| ID | Category | Requirement | Source Story |
|----|----------|-------------|--------------|
| FR-25 | Stops | The system **shall** group selected orders by delivery coordinate, so that several orders to one address become a single stop. | US-10 |
| FR-26 | Limits | The interface **shall** cap selection at 20 orders and refuse generation when the grouped stops exceed 14, the ceiling imposed by FR-09 once the depot is counted. | US-10 |
| FR-27 | Persistence | The system **shall** persist a generated route with its driver, vehicle, departure time, totals and the full algorithm snapshot, so the map can be redrawn later without recomputing. | US-13 |
| FR-28 | Lifecycle | A route **shall** allow `planejada → em_rota \| concluida` and `em_rota → concluida \| planejada` (the return covering a dispatch that did not leave); `concluida` is terminal. | US-13 |
| FR-29 | Sharing | The system **shall** generate an unguessable access token per route (32 random bytes, base64url) at creation, and expose it only to callers that can already see the route. | US-14 |
| FR-30 | Event log | The system **shall** record route events of type `saida`, `chegada`, `entrega`, `ocorrencia`, `abastecimento` and `retorno`, each with its author's role and name, an optional note and the instant it occurred. | US-15, US-19 |
| FR-31 | Event log | Events of type `chegada` and `entrega` **shall** reference the stop they belong to, by its index in the optimised order. | US-15, US-19 |
| FR-32 | Progress | A stop **shall** count as completed when a delivery event references it; progress **shall not** be read from a stored counter. | US-15 |
| FR-33 | Event log | Events **shall** be creatable and deletable but not editable. | US-15 |

### 2.5 Driver application

| ID | Category | Requirement | Source Story |
|----|----------|-------------|--------------|
| FR-34 | Access | The app **shall** open a route by code at `GET /api/mobile/routes/{code}`, carrying the driver session obtained at login. The code selects which route; it is not the credential. | US-16 |
| FR-34a | Authentication | The app **shall** authenticate at `POST /api/mobile/auth/login` with username, password and the device identifier, and **shall** receive a session token used on every subsequent call. | US-16 |
| FR-34b | Device binding | Login **shall** be refused unless the device identifier matches the one the manager registered for that account. An account with no registered device **shall** be refused from every device. | US-16 |
| FR-34c | Device identity | The device identifier **shall** be `Settings.Secure.ANDROID_ID`, **not** the IMEI: since Android 10 the IMEI requires a privileged permission unavailable to third-party applications, and Play policy forbids using it as a persistent identifier. The app **shall** display the identifier on its login screen, because the operating system exposes it on no screen of its own and the manager cannot otherwise obtain it. | US-16 |
| FR-34d | Refusal detail | Wrong credentials **shall** return HTTP 401 with one message that does not distinguish an unknown username from a wrong password; a correct credential refused for device or account status **shall** return HTTP 403 with a message directing the driver to the manager. | US-16 |
| FR-34e | Access management | The manager **shall** be able to create, block, unblock and delete app accounts and to register or release the device bound to each, at `/api/mobile-users`. | US-16 |
| FR-35 | Isolation | The mobile surface **shall** expose exactly one route and **shall not** return internal identifiers of the route, driver or vehicle. | US-16 |
| FR-36 | Isolation | An unknown token **shall** return HTTP 404, never 403, so that a response cannot confirm a token's existence. | US-16 |
| FR-37 | Authorship | The server **shall** set the author of a mobile event to the route's assigned driver; the app **shall not** be able to choose it. | US-19 |
| FR-38 | Stops | The server **shall** deliver stops already matched to customers and numbered in the optimised order, identically to the manager's map. | US-17 |
| FR-39 | Navigation | The app **shall** delegate turn-by-turn navigation to the device's map application. | US-18 |
| FR-40 | Offline | The app **shall** persist a registered event locally and reflect it in the interface before the server confirms it, synchronising when connectivity returns. | US-20 |
| FR-41 | Idempotency | Each event **shall** carry an identifier generated on the device; a repeated identifier **shall** return the stored event rather than creating a second one. | US-20 |
| FR-42 | Batch | Batch synchronisation **shall** report success or failure per item, and one rejected event **shall not** prevent the others from being stored. | US-20 |

### 2.6 Platform

| ID | Category | Requirement | Source Story |
|----|----------|-------------|--------------|
| FR-43 | Health | The system **shall** expose `GET /actuator/health` returning HTTP 200 when operational. | NFR-07 |
| FR-44 | Errors | All errors **shall** be returned as RFC 9457 problem details. | NFR-13 |
| FR-45 | CORS | Allowed origins **shall** be configurable; the wildcard `*` **shall not** be used. | NFR-14 |
| FR-46 | Observability | Inbound route requests **shall** be logged with a correlation identifier using SLF4J/Logback. | NFR-11 |

---

## 3. NON-FUNCTIONAL REQUIREMENTS (NFR)

### 3.1 Performance

| ID | Requirement |
|----|-------------|
| NFR-01 | Route computation **shall** complete within 5 seconds for the 15-waypoint ceiling under normal network conditions, the dominant cost being the two OSRM calls. |
| NFR-02 | The exact solver's state space is `N × 2^N` — about 490,000 states at N=15, which it **shall** explore in under one second. Beyond roughly 20 waypoints an exact solver is not viable and a heuristic one would be required. |
| NFR-03 | The backend **shall** use Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`), so the HTTP thread returns immediately and the computation runs asynchronously. |
| NFR-04 | The SSE emitter TTL **shall** be 180,000 ms (3 minutes). |
| NFR-05 | Route totals (distance, duration, value) **shall** be denormalised onto the route row so that listings do not parse the algorithm snapshot. |

### 3.2 Reliability

| ID | Requirement |
|----|-------------|
| NFR-06 | An OSRM failure **shall** degrade the result, never fail the request (FR-17, FR-18). |
| NFR-07 | If the SSE client disconnects before `COMPLETED`, the emitter **shall** be released. |
| NFR-08 | A driver's registration **shall** never be lost to a lack of connectivity: it is written to local storage before any network attempt (FR-40). |
| NFR-09 | A retry after an unacknowledged request **shall not** produce a duplicate delivery (FR-41). |
| NFR-10 | Batch synchronisation **shall not** run inside a single transaction, so that one invalid item cannot roll back the valid ones. |

### 3.3 Security

| ID | Requirement |
|----|-------------|
| NFR-11 | All input **shall** be validated with `jakarta.validation` before processing; rules that annotations express poorly (closed value sets, conditionally required fields) **shall** be validated in the service layer with messages that name what is accepted. |
| NFR-12 | The route access token **shall** be generated with `SecureRandom` and **shall** be treated as a bearer secret: it grants access to one route and is not authentication. |
| NFR-13 | External service URLs **shall** be configurable and never hardcoded. |
| NFR-14 | CORS origins **shall** be explicit. |
| NFR-15 | **Open gap:** the manager API requires no authentication. `/api/auth/login` issues a token for any e-mail without checking a password, there is no manager user table, and every endpoint outside `/api/mobile/**` is reachable unauthenticated — including `/api/mobile-users`, which grants app access. The login flow protects pages in the Next.js layer only. A production deployment would have to close this before exposure. |
| NFR-27 | The driver surface `/api/mobile/**` **shall** require an authenticated driver session, enforced by a servlet filter outside Spring Security (only `spring-security-crypto` is present, for BCrypt). Driver tokens **shall** be distinguishable from manager tokens so that one cannot be presented as the other. |
| NFR-28 | Driver passwords **shall** be stored only as BCrypt hashes, and **shall not** appear in any response or log. |
| NFR-29 | **Open gap:** login attempts against the driver surface are not retained. Only the last successful login is stored (`mobile_users.last_login_at`); refusals reach the application log only, so a refused device leaves no trace the manager can read. |
| NFR-16 | **Open gap:** the JWT signing secret is committed in `application.properties` and duplicated in `docker-compose.yml`. |

### 3.4 Code quality

| ID | Requirement |
|----|-------------|
| NFR-17 | Java service interfaces **shall** be prefixed with `I` and implemented by `<Name>ServiceImpl`. |
| NFR-18 | No `System.out.println()` and no bare `catch (Exception e) {}`. |
| NFR-19 | Indentation: 2 spaces in every language. |
| NFR-20 | Identifiers, file names and commit messages **shall** be in English; user-facing text **shall** be in Brazilian Portuguese. |
| NFR-21 | Flyway **shall** own the database schema (`ddl-auto=validate`); an applied migration **shall never** be edited. |
| NFR-22 | Unit tests **shall** cover the A\* solver, its tie-break behaviour, the Haversine utility, the empirical cost calculators and the mobile wire contract. |
| NFR-23 | **Open gap:** no automated tests exist for controllers, services or repositories, and no Spring context test exists. Changes to CRUD or status-transition logic are not covered by anything. |

### 3.5 Portability

| ID | Requirement |
|----|-------------|
| NFR-24 | The whole stack **shall** be runnable with `docker compose up --build`, requiring only Docker on the host. |
| NFR-25 | Database connection settings **shall** come from environment variables with local-development defaults. |
| NFR-26 | The backend endpoint the driver app targets **shall** be a build-time parameter of the app, not a hardcoded address. |

---

## 4. SYSTEM REQUIREMENTS (SR)

| ID | Category | Requirement |
|----|----------|-------------|
| SR-01 | Runtime | Java 21 LTS for the backend. |
| SR-02 | Build | Apache Maven 3.9+. |
| SR-03 | Database | PostgreSQL 16, reachable at the configured host and port. |
| SR-04 | Network | Outbound HTTPS to `router.project-osrm.org` for OSRM; to ViaCEP, BrasilAPI and Nominatim for the browser-side lookups. |
| SR-05 | Web frontend | Node.js 20+ and pnpm. |
| SR-06 | Browser | Latest stable Chrome, Firefox or Edge, with `EventSource` support. |
| SR-07 | Driver app | Flutter SDK 3.24+, a JDK between 17 and 25 (the project's Gradle refuses 11), and Android SDK 36. |
| SR-08 | Driver device | Android device or emulator; the app reaches the backend over the local network or through `adb reverse`. |
| SR-09 | OS | Linux, macOS or Windows. |

---

## 5. REQUIREMENT TRACEABILITY MATRIX

| User Story | FR Coverage | NFR Coverage |
|------------|-------------|--------------|
| US-01 Vehicles | FR-01, FR-07 | NFR-11, NFR-21 |
| US-02 Drivers | FR-02, FR-07 | NFR-11, NFR-21 |
| US-03 Products/units | FR-03, FR-07 | NFR-11, NFR-21 |
| US-04 Customers | FR-04, FR-05 | NFR-11, NFR-13 |
| US-05 Refuellings | FR-06, FR-07 | NFR-11 |
| US-06 Company settings | FR-08 | NFR-11 |
| US-07 Create order | FR-21 | NFR-11 |
| US-08 Order lifecycle | FR-22 | NFR-11, NFR-23 |
| US-09 Invoice batch | FR-23, FR-24 | NFR-23 |
| US-10 Generate route | FR-09 … FR-15, FR-17, FR-25, FR-26, FR-24a, FR-24b | NFR-01, NFR-02, NFR-03, NFR-06 |
| US-11 See route on map | FR-16, FR-18, FR-38 | NFR-05, NFR-06 |
| US-12 Live progress | FR-19 | NFR-04, NFR-07 |
| US-13 Save and dispatch | FR-27, FR-28, FR-24d | NFR-05, NFR-21 |
| US-14 Share with driver | FR-29 | NFR-12 |
| US-15 Track route | FR-30 … FR-33 | NFR-23 |
| US-16 Sign in and open route | FR-34, FR-34a … FR-34e, FR-35, FR-36 | NFR-12, NFR-15, NFR-27, NFR-28, NFR-29 |
| US-17 Ordered stops | FR-38 | NFR-05 |
| US-18 Navigate | FR-39 | — |
| US-19 Register events | FR-30, FR-31, FR-37, FR-24a, FR-24c, FR-24d, FR-24e | NFR-11 |
| US-20 Work offline | FR-40, FR-41, FR-42, FR-24e | NFR-08, NFR-09, NFR-10 |

---

## 6. CONSTRAINTS & ASSUMPTIONS

1. **The computation pipeline is stateless.** `POST /compute` holds the request in memory
   only until its SSE stream is opened. Everything else — registrations, orders, routes,
   events — is persisted.
2. **OSRM is a dependency, not a component.** The public demo instance is rate-limited and
   subject to community fair use. Both calls degrade gracefully (FR-17, FR-18), but the
   `/table` degradation changes the optimisation input and the result is no longer
   road-accurate.
3. **The exact solver bounds the product.** FR-09's ceiling of 15 waypoints is a
   consequence of FR-12, not a UI preference. Raising it means abandoning exactness.
4. **No vehicle telemetry.** Progress is derived from events the driver registers. The
   system never claims to know the vehicle's position.
5. **The route token is not authentication.** It is an unguessable URL granting access to
   one route. It travels over WhatsApp at the manager's discretion and can be regenerated.
6. **Authentication and CRUD test coverage are open gaps** (NFR-15, NFR-16, NFR-23),
   stated so that the system is not evaluated as if they were closed.
7. **Document attachment is demonstrative.** Both the web tracking screen and the app show
   the final shape of the attachment list without storing files anywhere.
