# REQUIREMENTS — RouteWise

> **Type:** Academic Full-Stack Application (TCC)
> **Domain:** Urban Logistics / Fleet Management
> **First issued:** 2026-04-14
> **Revised:** 2026-10-24 — rewritten against the implemented system
> **Status:** ACTIVE

---

## SCOPE NOTE

The 2026-04-14 version of this document described a single-purpose, stateless route
optimizer: place 2–10 pins on a map, get the best visiting order back. That core still
exists and is still the heart of the project, but the system around it grew into a small
fleet-management application with three distinct consumers — the **manager**, who runs the
operation from a browser, the **driver**, who executes the route from a phone, and an
**external system**, which reads data through a credentialed integration API.

This revision describes what is actually built. Requirements that the implementation
contradicted were corrected, not quietly dropped; the most consequential is the ordering
algorithm, which is **exact**, not the nearest-neighbour heuristic the earlier document
claimed (see `docs/formulas-a-star-e-custo.md` and FR-12 in the SRS).

---

## ACTORS

| Actor | Where | Authenticated |
|-------|-------|---------------|
| **Manager** (gestor) | `FrontEnd-Next/` web dashboard | Yes: e-mail + password (BCrypt), and every panel endpoint requires the session |
| **Driver** (motorista) | `mobile/` Android app | Yes: username + password (BCrypt) and the phone must be the one the manager enrolled. The route itself is then opened by a per-route code |
| **External system** (integração) | any HTTP client | Yes: a client credential traded for a short-lived token, which carries the scopes it may read |

---

## USER STORIES

### Registration (manager)

| ID    | As a…    | I want to…                                                      | So that…                                                        |
|-------|----------|-----------------------------------------------------------------|-----------------------------------------------------------------|
| US-01 | Manager  | Register vehicles with plate, model, load capacity and fuel type | I can assign the right truck to a route                         |
| US-02 | Manager  | Register drivers with CPF, phone and licence data                | I know who can be dispatched                                    |
| US-03 | Manager  | Register products and their units of measure                     | Orders can be composed of real items with weight and price      |
| US-04 | Manager  | Register customers with address and geographic coordinate        | Their address can become a route stop                           |
| US-05 | Manager  | Register refuellings per vehicle                                  | Fuel consumption can be measured from real operation            |
| US-06 | Manager  | Set the company address once                                     | Every route departs from and returns to the real depot          |

### Orders and invoicing (manager)

| ID    | As a…    | I want to…                                                      | So that…                                                        |
|-------|----------|-----------------------------------------------------------------|-----------------------------------------------------------------|
| US-07 | Manager  | Create an order for a customer with items, discount and freight | The delivery to be made is recorded with its value               |
| US-08 | Manager  | Move an order through its lifecycle                              | The state of each delivery is unambiguous at any moment          |
| US-09 | Manager  | Invoice a batch of pending orders                                | Only orders ready to leave enter a route                         |

### Routing (manager)

| ID    | As a…    | I want to…                                                      | So that…                                                        |
|-------|----------|-----------------------------------------------------------------|-----------------------------------------------------------------|
| US-10 | Manager  | Select invoiced orders and generate an optimised route           | The truck drives the cheapest sequence instead of a guessed one  |
| US-11 | Manager  | See the computed route drawn on a map with numbered stops        | I can read the sequence without cross-referencing a list         |
| US-12 | Manager  | Receive progress updates while the route is being computed       | The screen does not look frozen during the computation           |
| US-13 | Manager  | Save a route against a driver and a vehicle                      | The dispatch is recorded and can be consulted later              |
| US-14 | Manager  | Share the route with the driver                                  | He receives it on his phone without me dictating addresses       |

### Execution and tracking

| ID    | As a…    | I want to…                                                      | So that…                                                        |
|-------|----------|-----------------------------------------------------------------|-----------------------------------------------------------------|
| US-15 | Manager  | Follow a route in progress: stops, events and documents          | I know what has been delivered and what went wrong               |
| US-16 | Driver   | Sign in on my own phone and open the day's route from the link I received | Only I, on the phone the company registered, can register work in my name |
| US-17 | Driver   | See my stops in order and which one is next                      | I do not have to decide the sequence myself                      |
| US-18 | Driver   | Be guided to the next stop                                       | I use the navigation app I already know                          |
| US-19 | Driver   | Register arrival, delivery and incidents where they happen       | The office sees what happened without me phoning in              |
| US-20 | Driver   | Register events with no signal                                   | Delivering in a shed or basement does not lose the record        |

### Integration (manager)

| ID    | As a…    | I want to…                                                      | So that…                                                        |
|-------|----------|-----------------------------------------------------------------|-----------------------------------------------------------------|
| US-21 | Manager  | Give an external system a credential that reads my orders, routes and deliveries | The company's other systems stop depending on someone exporting data by hand |

### Operational overview (manager)

| ID    | As a…    | I want to…                                                      | So that…                                                        |
|-------|----------|-----------------------------------------------------------------|-----------------------------------------------------------------|
| US-23 | Manager  | Open the system and see what the operation did and what it cost | I can tell whether the optimised routes are actually cheaper, instead of only knowing they were shorter |

### Panel access (manager)

| ID    | As a…    | I want to…                                                      | So that…                                                        |
|-------|----------|-----------------------------------------------------------------|-----------------------------------------------------------------|
| US-22 | Manager  | Sign in with my own password and control who else can enter the panel | Nobody reaches the operation's data — or the screens that grant access to the app and to integrations — just by reaching the server |

---

## DECISIONS LOCKED

| Decision                   | Value                                                                       |
|----------------------------|-----------------------------------------------------------------------------|
| Java version               | 21 LTS with Virtual Threads                                                 |
| Backend framework          | Spring Boot 3.2.5                                                           |
| Persistence                | PostgreSQL 16, Spring Data JPA, **Flyway owns the schema** (`ddl-auto=validate`) |
| Production frontend        | Next.js 16 + React 19 + Tailwind 4 (`FrontEnd-Next/`)                       |
| Driver app                 | Flutter (Dart 3.5+), Android first (`mobile/`)                              |
| Routing engine             | OSRM public API — `/table` for costs, `/route` for geometry                 |
| Optimization algorithm     | **Exact A\*** over the complete waypoint graph, bitmask state               |
| Cost metric                | Travel duration (seconds), from the OSRM duration matrix                    |
| Waypoint bounds            | 2–15 (the exact solver's practical ceiling)                                 |
| Route modes                | ROUND_TRIP / OPEN_ROUTE / FIXED_START_END                                   |
| Map tiles                  | OpenStreetMap (free, no API key)                                            |
| Map library (web)          | react-leaflet + leaflet.js                                                  |
| Third-party lookups        | ViaCEP (CEP), BrasilAPI (CNPJ, CEP v2), FIPE (vehicle models), Nominatim (geocoding) — all free and key-less |
| Driver authentication      | Username + BCrypt password + enrolled device (`ANDROID_ID`, not IMEI); enforced on `/api/mobile/**` by a servlet filter. The per-route code selects *which* route, it is not the credential |
| Manager authentication     | Login screen + JWT cookie; **not enforced by the backend** (known gap)      |
| Osm graph (`osm4j`)        | Present but **disabled by default**; feeds only `GET /api/routes/graph-stats` |

---

## ACCEPTANCE CRITERIA SUMMARY

### Core routing
- [x] 2–15 waypoints accepted; validated server-side with HTTP 400 on violation
- [x] Three route modes selectable, all three implemented in the solver
- [x] A* returns the **optimal** visiting order, not an approximation
- [x] Cost comes from the OSRM duration matrix; Haversine at a fixed average speed is the documented fallback
- [x] OSRM supplies route geometry (GeoJSON LineString)
- [x] SSE streams PROCESSING → COMPLETED (or ERROR)
- [x] An OSRM failure degrades the result instead of failing the request

### Fleet management
- [x] Eight registration screens, each backed by a real table
- [x] A refuelling's distance is derived from the reading below it, so no row shows negative kilometres
- [x] An older refuelling can be corrected: the reading must sit between its neighbours, not above the highest
- [x] The refuellings screen totals spend, litres, average consumption and average price for the current filter
- [x] Orders carry items, discount, freight, history and a validated status machine
- [x] Orders sharing an address collapse into one route stop
- [x] Routes persist with their algorithm snapshot and can be listed and reopened
- [x] A tracking screen shows stops, the event log and (mocked) documents
- [x] Selecting a stop highlights it on the map, and clicking a marker highlights it in the list

### Order lifecycle
- [x] A route records which orders each stop delivers
- [x] Creating a route moves its orders out of `faturado`, so none can be dispatched twice
- [x] A delivery in the field closes the orders of that stop, and only of that stop
- [x] Concluding or deleting a route returns undelivered orders to the pool
- [x] The automatic transitions appear in the order's own status history
- [x] An order is editable only while `aguardando` — from `faturado` on it is committed to a route
- [x] A delivered order cannot be edited, nor selected to compose a new route
- [x] The order records when it was delivered, taken from the field event itself

### Driver app
- [x] Sign-in with username, password and a device the manager registered
- [x] The login screen shows this phone's identifier, so the manager can register it
- [x] An unregistered phone cannot sign in, even with the right password
- [x] `/api/mobile/**` refuses calls without a driver session
- [x] Home screen listing the driver's own routes, open ones first, with delivery progress
- [x] Tapping a route opens it; the QR/code path remains for a route not yet listed
- [x] A driver cannot read or write another driver's route, even holding its code
- [x] Stops are numbered identically to the manager's map
- [x] Arrival, delivery and incident are registered from the stop screen
- [x] Events survive being registered with no connectivity and sync afterwards
- [x] A retried event cannot register a second delivery
- [x] Signing out keeps the offline queue — unsent field records are not lost
- [ ] Route map screen — **not done**

### Integration
- [x] The manager creates a credential, picks its scopes, and sees the secret exactly once
- [x] The secret is stored only hashed; losing it means generating another, never recovering it
- [x] A client trades its credential for a short-lived token and reads with that token
- [x] A scope that was not granted is refused, and a path with no scope mapped is refused too
- [x] An integration token does not work on the driver surface, nor a driver token on the integration
- [x] Revoking or deleting a credential cuts access on the next request
- [x] Every call, accepted or refused, appears in the manager's access log — including attempts with
      an identifier that matches no credential
- [x] The log survives deletion of the credential it refers to
- [ ] External writes (an ERP creating an order) — **out of scope by decision**

### Operational overview
- [x] The home screen shows real numbers for a window of 7, 30 or 90 days
- [x] Deliveries are counted by when the driver registered them in the field
- [x] The estimated cost of the window is split into fuel, tyre wear and driver hours
- [x] Fuel consumption is measured from each vehicle's own refuellings when there are enough
- [x] A vehicle with too little history is shown as using the reference figure, not as measured
- [x] The fuel actually purchased in the window is shown next to the estimate, not in place of it
- [x] The screen states that it is an estimate, and which components are model constants
- [ ] Cost per route on the route screen itself — **not done**
- [ ] Planned versus realised cost for a single route — **not done**, it needs expenses recorded per route

### Panel access
- [x] Sign-in with e-mail and password, checked against a BCrypt hash in the database
- [x] A wrong password and an unknown e-mail give the same answer
- [x] A blocked access is refused with its own message, after the password is checked
- [x] Every panel endpoint refuses a request with no session — including the cadastros that grant access to the app and to integrations
- [x] A token from the app or from an integration is refused on the panel, and vice versa
- [x] The manager creates, edits, blocks and deletes panel accesses from a screen
- [x] The last active access cannot be blocked or deleted, and nobody can delete their own
- [x] A session that expires sends the browser back to the login instead of failing screen by screen
- [ ] Roles (admin/operator) — **out of scope by decision**
- [ ] Revoking a session before its token expires — **not done**

### Quality
- [x] No business logic in the frontends — both consume the same API
- [x] Backend response under 5 s for the practical waypoint ceiling
- [x] Automated tests for the order lifecycle (stop↔order matching and propagation)
- [x] Automated test pinning that a scope nobody granted cannot be read out of a similar one
- [x] Automated test pinning that a token of one surface is refused by the other two
- [ ] Automated tests for controllers and repositories — **not done**
- [x] Manager authentication enforced on the backend, not only in the page guard
- [ ] A screen showing app login attempts — **not done** (see below)

---

## KNOWN GAPS

These are stated here rather than hidden, because they bound what the system can claim.

1. **Every user of the panel shares one level of access.** Authentication is enforced, but
   there are no roles: whoever signs in reaches every screen, including the two that
   grant access to the other surfaces. Splitting admin from operator is authorisation,
   a separate concern, and was not built.
2. **Sessions cannot be revoked before they expire.** A token is valid for an hour by its
   own signature; blocking or deleting an access stops the *next* login, not the session
   already in someone's browser. A server-side session list would close this, at the cost
   of a database read on every request.
3. **The route code is no longer a bearer secret.** It selects which route, and the
   server additionally requires a driver session *and* that the route belong to that
   driver. Holding the link reaches nothing on its own. The trade-off is that a driver
   can no longer cover an absent colleague's route with the code alone — the manager has
   to reassign it.
4. **Login attempts are not retained.** Only the last successful login is stored; refusals
   reach the application log only. A refused device — the event the binding exists to
   prevent — leaves no trace the manager can read.
5. **Document attachment is mocked** on both the web tracking screen and the app. Real
   upload needs decisions on storage, size limits and retention that have not been made;
   deferred by choice, not pending work.
6. **No vehicle telemetry.** Progress comes from events the driver registers, never from
   GPS — the system never claims to know where the truck is.
7. **Thin automated coverage.** The mathematical core, the mobile wire contract and the
   order lifecycle are tested; controllers and repositories are not.
8. **The order↔stop link is not retroactive.** Routes saved before stops recorded their
   orders cannot close an order: the link was never stored and cannot be reconstructed.
