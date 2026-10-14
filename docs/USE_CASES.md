# USE CASES SPECIFICATION

> **Project:** RouteWise — Urban Route Optimizer and Fleet Management
> **Version:** 2.0
> **First issued:** 2026-04-14 (v1.0)
> **Revised:** 2026-10-14 — rewritten against the implemented system
> **Status:** ACTIVE

---

## SCOPE NOTE

v1.0 specified five use cases, all of them inside a single screen: drop pins, compute,
watch the result. That flow survives as UC-05 and UC-06, but it is no longer the whole
system. The operation now starts at a registration screen and ends on a driver's phone.

The use cases below follow the real path of a delivery: register → order → invoice →
route → dispatch → execute → track.

---

## ACTORS

| Actor | Type | Description |
|-------|------|-------------|
| **Manager** (gestor) | Human | Runs the operation from the web dashboard: registrations, orders, routes, tracking. |
| **Driver** (motorista) | Human | Executes the route in the field with the Android app. |
| **Web frontend** | System | Next.js application; the manager's entire interface. |
| **Driver app** | System | Flutter application; opens one route by token and registers field events. |
| **Backend** | System | Spring Boot; owns the data, the solver and the API. |
| **A\* solver** | System | In-process exact optimiser over the waypoint cost matrix. |
| **OSRM API** | External | Public routing service: duration matrix and route geometry. |
| **Lookup services** | External | ViaCEP, BrasilAPI and Nominatim, called from the browser to fill in addresses. |

---

## UC-01 — Register a Customer with a Mappable Address

**ID:** UC-01
**Actors:** Manager, Web frontend, Lookup services
**Source Stories:** US-04

**Pre-conditions:**
- The manager is on `/dashboard/clientes`.

**Main Flow:**
1. Manager opens the registration form and chooses the person type and the register type (customer, supplier or both).
2. Manager types the CNPJ; the frontend queries BrasilAPI and fills in company name, trade name and address.
3. Manager types the CEP; the frontend queries ViaCEP and fills in street, district, city and state.
4. Manager completes number and complement.
5. On save, the frontend geocodes the address through Nominatim and presents the resulting point on a small map for confirmation.
6. Manager confirms the point, or drags the pin to the correct spot.
7. Backend persists the customer with its coordinate.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 2a | CNPJ not found | The lookup fails silently; every field stays editable and the manager types them. |
| 5a | Nominatim returns only a city-level match | The system rejects the low-confidence result and opens the map centred on the city for manual placement. |
| 5b | Both geocoding attempts fail | The modal opens at the city centre and the manager positions the pin by hand. |

**Post-conditions:**
- The customer exists with a coordinate precise enough to become a route stop.

**Note:** without a coordinate the customer still saves, but route generation will fall
back to an approximate city point for it (see UC-05, alternative 2a).

---

## UC-02 — Record an Order

**ID:** UC-02
**Actors:** Manager, Web frontend, Backend
**Source Stories:** US-07

**Pre-conditions:**
- At least one customer and one product are registered.

**Main Flow:**
1. Manager opens `/dashboard/pedidos` and starts a new order.
2. Manager selects the customer through the search modal.
3. Manager adds items: product, quantity and unit price, the price defaulting to the product's registered one.
4. Manager optionally sets a discount, a freight cost and notes.
5. The screen shows the running total and, when a vehicle is chosen, warns if the order's weight exceeds its capacity.
6. Manager saves; the backend creates the order in status `aguardando` and opens its history with that first entry.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 3a | Quantity or price invalid | Zod validation blocks the submission and marks the field. |
| 5a | Items exceed the selected vehicle's capacity | The screen warns but does not block: the manager may split the load later. |

**Post-conditions:**
- An order exists in `aguardando`, invisible to route generation until invoiced.

---

## UC-03 — Invoice Pending Orders

**ID:** UC-03
**Actors:** Manager, Web frontend, Backend
**Source Stories:** US-09

**Main Flow:**
1. Manager filters the order list by status `aguardando`.
2. Manager selects the orders that will leave.
3. Manager triggers the bulk action to invoice them.
4. For each order, the backend validates the transition `aguardando → faturado` and records a history entry.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 4a | An order is no longer in `aguardando` | The backend rejects that transition with HTTP 409 and an explanatory message. |

**Post-conditions:**
- The selected orders are in `faturado` and eligible for a route.

---

## UC-04 — Generate an Optimised Route

**ID:** UC-04
**Actors:** Manager, Web frontend, Backend, A\* solver, OSRM API
**Source Stories:** US-10, US-11, US-12

**Pre-conditions:**
- At least two invoiced orders exist.
- The company address is registered (otherwise a default origin is used).

**Main Flow:**
1. Manager opens `/dashboard/rotas/gerar`; the screen lists invoiced orders.
2. Manager selects the orders to dispatch. The counter shows both selected orders and the stops they resolve to.
3. Manager picks a driver and a vehicle.
4. On "Gerar rota", the frontend groups the orders by delivery coordinate, so that several orders to one address become one stop.
5. The frontend builds the waypoint list — company depot first, then the grouped stops — and posts it to `/api/routes/compute`.
6. The backend returns a `requestId`; the frontend opens the SSE stream and shows the progress state.
7. The backend fetches the OSRM duration matrix, runs the exact A\* search, requests the route geometry and emits `COMPLETED`.
8. The map draws the route with numbered stops; the panel lists distance, duration and the leg breakdown.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 2a | A selected customer has no coordinate | Its position falls back to an approximate point for the city, seeded by the customer so that all their orders land on the same point. |
| 2b | The selection resolves to more than 14 distinct addresses | Generation is blocked with a message naming the number of addresses and the ceiling, before any request is sent. |
| 4a | Two orders share an address | They collapse into one stop whose popup shows how many orders it covers. |
| 7a | OSRM `/table` unavailable | The matrix is approximated from straight-line distance at a fixed average speed; the route is still produced, with a coarser cost model. |
| 7b | OSRM `/route` unavailable | The optimised order is returned without geometry; the map draws straight segments and the screen flags the degraded result. |
| 7c | Computation fails | The stream emits `ERROR` and the screen shows the reason. |

**Post-conditions:**
- An optimal visiting order exists for the selected stops, drawn on the map, not yet saved.

---

## UC-05 — Compute the Optimal Order (Backend)

**ID:** UC-05
**Actors:** Backend, A\* solver, OSRM API
**Source Stories:** US-10

**Pre-conditions:**
- A validated request of 2–15 waypoints is pending for the given `requestId`.

**Main Flow:**
1. The SSE stream opens; the computation is dispatched to a virtual thread and the HTTP thread returns.
2. `PROCESSING` is emitted.
3. OSRM `/table` returns the N×N duration matrix.
4. The solver initialises the state `(waypoint 0, bitmask 0b1)` and the open set ordered by `f = g + h`.
5. For each expansion, the heuristic sums the minimum incoming edge of every unvisited waypoint, adding a return bound in `ROUND_TRIP`. Ties in `f` expand the larger `g` first.
6. In `FIXED_START_END`, the final waypoint is not enterable until all others are visited.
7. On reaching the goal state, the path is reconstructed through the parent table, giving the optimal order.
8. OSRM `/route` is called for that order; per-leg geometry, distance and duration are assembled.
9. `COMPLETED` is emitted with the ordered waypoints, segments, totals and geometry.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 3a | `/table` fails | The matrix is built from Haversine distance at the configured average speed; the degradation is logged as affecting the optimisation input. |
| 8a | `/route` fails | `osrmValidation` is set to `UNAVAILABLE` and the result goes out without geometry. |
| 4a | A single waypoint | The trivial order is returned without searching. |

**Post-conditions:**
- The emitted order is optimal for the matrix used — not an approximation.

---

## UC-06 — Save and Dispatch the Route

**ID:** UC-06
**Actors:** Manager, Web frontend, Backend
**Source Stories:** US-13, US-14

**Main Flow:**
1. With a computed route on screen, the manager confirms driver, vehicle and departure time.
2. On "Salvar rota", the frontend posts the route with its algorithm snapshot and labelled stops.
3. The backend persists it in status `planejada`, denormalises the totals and generates an access token.
4. The route appears in `/dashboard/rotas`, where its card shows driver, vehicle and load capacity.
5. The manager uses "Enviar" to share the route with the driver over WhatsApp.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 5a | The driver has no phone registered | The share button is disabled with an explanatory tooltip. |

**Post-conditions:**
- The route is persisted, dispatched, and reachable by the driver through its token.

---

## UC-07 — Track a Route in Progress

**ID:** UC-07
**Actors:** Manager, Web frontend, Backend
**Source Stories:** US-15

**Main Flow:**
1. From a route card, the manager opens `/dashboard/rotas/[id]`.
2. The screen shows the map with numbered stops and a side panel switched by a top bar.
3. **Paradas** lists the stops, marking as completed those with a delivery event, and offers a shortcut to register a delivery.
4. **Eventos** lists the log chronologically, with type, author, referenced stop and note.
5. **Documentos** shows the attachment list (demonstrative — no file is stored).
6. The manager may register an event himself, choosing the author role.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 3a | The manager registers a delivery for an already delivered stop | The shortcut is not offered for completed stops. |
| 6a | A delivery or arrival is registered without a stop | The backend rejects it with HTTP 400, naming the missing field. |
| 6b | An event is deleted | The progress recomputes, since it derives from the events. |

**Post-conditions:**
- The route's progress reflects the events recorded, by either party.

---

## UC-08 — Open the Route on the Phone

**ID:** UC-08
**Actors:** Driver, Driver app, Backend
**Source Stories:** US-16, US-17

**Pre-conditions:**
- The driver received the link, or the route's code.

**Main Flow:**
1. The driver opens the app and pastes the code (or follows the link, once deep linking is implemented).
2. The app requests `GET /api/mobile/routes/{token}`.
3. The backend returns the route, the stops already matched to customers and numbered, and the events so far — with no internal identifiers.
4. The app shows the day's route: departure, vehicle with capacity, progress, next stop highlighted and the full stop list.
5. The token is stored, so the next launch opens the route directly.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 2a | Unknown or revoked token | HTTP 404 and a message asking for a new link; the stored token is cleared. |
| 2b | No connectivity, with a route previously loaded | The app keeps showing the loaded route rather than an empty screen. |

**Post-conditions:**
- The driver sees exactly one route — his own.

---

## UC-09 — Register a Delivery in the Field

**ID:** UC-09
**Actors:** Driver, Driver app, Backend
**Source Stories:** US-18, US-19, US-20

**Main Flow:**
1. The driver opens the next stop and sees the customer, the orders and the actions.
2. "Navegar até aqui" hands the coordinate to the device's map application.
3. On arrival, the driver registers `chegada`.
4. After handing over the goods, the driver taps "Entreguei" and confirms.
5. The app generates an event identifier, writes the event to its local queue and updates the screen immediately.
6. The app posts the queue; the backend stores the event, setting the author to the route's driver.
7. The stop turns green, the progress advances and the next stop is highlighted.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 4a | Delivery not possible | The driver registers an `ocorrencia`, choosing a reason and describing what happened; a note is required. |
| 5a | No connectivity | The event stays queued, the screen already reflects it, and a badge shows how many registrations are pending. |
| 6a | Connectivity returns | The queue is sent as a batch; each item is answered individually and only the confirmed ones leave the queue. |
| 6b | A request is retried after an unacknowledged response | The repeated event identifier makes the backend return the stored event — the delivery is not counted twice. |
| 6c | One item in the batch is invalid | It is reported as rejected with its reason; the others are stored. |

**Post-conditions:**
- The event is recorded once, attributed to the driver, and visible on the manager's tracking screen.

---

## UC-10 — Register a Refuelling

**ID:** UC-10
**Actors:** Manager, Web frontend, Backend
**Source Stories:** US-05

**Main Flow:**
1. Manager opens `/dashboard/abastecimentos` and starts a record.
2. Manager selects vehicle and driver, and enters date, odometer, litres and price per litre.
3. The screen shows the derived total and the kilometres run since that vehicle's previous refuelling.
4. On save, the kilometres since the previous record are frozen onto the row.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 3a | First refuelling for the vehicle | The screen states so and no distance is derived. |
| 4a | An earlier record is edited afterwards | Later records are **not** recomputed; the stored value is a snapshot, matching the backend's empirical consumption model. |

**Post-conditions:**
- The refuelling history can feed the empirical fuel-consumption analysis under
  `com.routewise.validation`.
