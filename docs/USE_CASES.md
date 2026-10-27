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
route → dispatch → execute → track. UC-11 steps outside that line: it is how a system other
than RouteWise reads the result.

---

## ACTORS

| Actor | Type | Description |
|-------|------|-------------|
| **Manager** (gestor) | Human | Runs the operation from the web dashboard: registrations, orders, routes, tracking. |
| **Driver** (motorista) | Human | Executes the route in the field with the Android app. |
| **External system** (integração) | System | Third-party client that reads orders, routes and deliveries with a credentialed token. |
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
| 6a | The manager reopens an order already past `aguardando` | The form opens read-only, with a banner saying why, and the backend refuses an update anyway: from `faturado` on the order is committed to a route, and changing its items would silently disagree with what the driver is carrying. |
| 6b | The manager tries to select a delivered or cancelled order for a new route | The selection checkbox is not offered — the order is in a terminal state and composing a route from it would mean nothing. |

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
2. On "Salvar rota", the frontend posts the route with its algorithm snapshot and labelled stops — each stop carrying the orders it delivers.
3. The backend persists it in status `planejada`, denormalises the totals and generates an access code.
3a. The backend moves the route's orders from `faturado` to `em_rota`. This happens at creation, not at departure: the generation screen offers exactly the `faturado` orders, so leaving them there would let the same order be dispatched on a second route.
4. The route appears in `/dashboard/rotas`, where its card shows driver, vehicle and load capacity.
5. The manager uses "Enviar" to share the route with the driver over WhatsApp.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 5a | The driver has no phone registered | The share button is disabled with an explanatory tooltip. |

**Post-conditions:**
- The route is persisted, dispatched, and reachable by the driver through its code.
- Its orders read `em_rota` and no longer appear as eligible for another route.
- Concluding or deleting the route later returns any still-`em_rota` order to `faturado`.

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
- The manager created an app account for the driver and registered this phone's identifier on it.
- A route is assigned to the driver. He does **not** need to have received anything: the home screen lists his routes.

**Main Flow:**
1. The driver opens the app and signs in with username and password; the app sends this phone's identifier along with them.
2. The backend checks the password (BCrypt) and that the identifier is the one registered for the account, and returns a session token. The app stores the session, so later launches skip this step.
3. The app lands on the home screen and lists the driver's own routes (`GET /api/mobile/routes`): the open ones first, a short history of concluded ones below, each with date, departure, vehicle, order count, distance and delivery progress.
4. The driver taps the route he is starting. He may instead use "Abrir por código" and scan the QR, follow the link or type the code — the path for a route just assigned that the list has not picked up yet.
5. The app requests `GET /api/mobile/routes/{code}`, carrying the session.
6. The backend returns the route, the stops already matched to customers and numbered, and the events so far — with no internal identifiers.
7. The app shows the day's route: departure, vehicle with capacity, progress, next stop highlighted and the full stop list. Going back returns to the home screen, where the list is refreshed.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 1a | Wrong username or password | HTTP 401 and one message that does not say which of the two was wrong. |
| 1b | Correct credentials, phone not the registered one | HTTP 403 telling the driver to contact the manager; the app shows this phone's identifier, with a copy button, so he can send it. |
| 1c | Account has no registered device yet | HTTP 403 with the same instruction — no phone can sign in to that account until the manager registers one. |
| 1d | Account blocked by the manager | HTTP 403 saying the access is blocked. |
| 3a | No route assigned to him | The home screen says so and points at the code button, instead of showing an empty list that reads like a failure. |
| 3b | The list cannot be loaded | The error is shown with a "try again", and pulling down retries. |
| 4a | Unknown or revoked code | HTTP 404 and a message asking for a new link. |
| 4b | **Code of another driver's route** | HTTP 404 — identical to an unknown code. Telling him the route exists but is not his would hand any app an oracle for discovering valid codes. The manager has to reassign the route first. |
| 4c | No connectivity, with a route previously loaded | The app keeps showing the loaded route rather than an empty screen. |
| 4d | Session expired or revoked mid-day | HTTP 401; the app returns to the login screen. The offline queue is **not** cleared — unsent field records survive the sign-out. |

**Post-conditions:**
- The driver sees his own routes, and only his own.
- The account's last login is recorded.

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
7. The backend moves the orders of **that stop** from `em_rota` to `entregue` — the delivery is what closes the order.
8. The stop turns green, the progress advances and the next stop is highlighted.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 4a | Delivery not possible | The driver registers an `ocorrencia`, choosing a reason and describing what happened; a note is required. |
| 5a | No connectivity | The event stays queued, the screen already reflects it, and a badge shows how many registrations are pending. |
| 6a | Connectivity returns | The queue is sent as a batch; each item is answered individually and only the confirmed ones leave the queue. |
| 6b | A request is retried after an unacknowledged response | The repeated event identifier makes the backend return the stored event — the delivery is not counted twice. |
| 6c | One item in the batch is invalid | It is reported as rejected with its reason; the others are stored. |
| 7a | An order of that stop is no longer `em_rota` (cancelled in the panel, already delivered by an earlier event) | It is skipped and logged; the delivery itself is still recorded. A field registration never fails because of a status side-effect. |
| 7b | The route was saved before stops recorded their orders | No order is closed; the event is recorded normally. The link was never stored and cannot be recovered. |

**Post-conditions:**
- The event is recorded once, attributed to the driver, and visible on the manager's tracking screen.
- The orders of that stop read `entregue`, with the transition in their own status history and the
  delivery instant taken from the event itself, not from when the server processed it.

---

## UC-10 — Register a Refuelling

**ID:** UC-10
**Actors:** Manager, Web frontend, Backend
**Source Stories:** US-05

**Main Flow:**
1. Manager opens `/dashboard/abastecimentos` and starts a record.
2. Manager selects vehicle and driver, and enters date, odometer, litres and price per litre.
3. The screen shows the derived total and the kilometres run since that vehicle's previous refuelling — the reading immediately below the one being entered, not the vehicle's highest.
4. The record is saved with its odometer reading. The distance since the previous refuelling is **not** stored — it is derived from the odometer history whenever it is needed, both on this screen and by the consumption model behind the dashboard.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 3a | First refuelling for the vehicle | The screen states so and no distance is derived. |
| 3b | The reading does not fit the vehicle's timeline | Refused, naming the neighbouring reading: it must be above the previous refuelling's and below the next one's. Checking only against the highest reading would make an older record impossible to correct, since every plausible value for it is lower. |
| 4a | An earlier record is edited afterwards | Every derived distance that depended on it changes with it, because none of them were stored. `V7` chose this deliberately: a frozen value would go stale the moment a mistyped odometer was corrected. |
| 4b | An odometer reading lower than the previous one | The pair is skipped when consumption is computed — a negative or zero distance is a typo, and dividing by it would produce a consumption figure that poisons the fleet average. |

**Post-conditions:**
- The refuelling history feeds the empirical fuel-consumption model, which is what the
  dashboard uses to cost a route in reais (see UC-13).
- The screen totals what is on it: money spent, litres, average consumption and average
  price paid, for whatever filter is applied. The average divides total distance by total
  litres rather than averaging each row's figure, so a tank that covered 650 km weighs more
  than one that covered 80 — the same method the backend's model uses.

---

## UC-11 — Give an External System Read Access

**ID:** UC-11
**Actors:** Manager, Web frontend, External system, Backend
**Source Stories:** US-21

**Pre-conditions:**
- The manager knows which data the external system needs. Scope is granted per area, not all at once.

**Main Flow:**
1. The manager opens `/dashboard/integracoes` and creates a credential, naming the system and ticking the scopes it may read (orders, routes, deliveries).
2. The backend generates the client identifier and the secret, stores the secret only as a BCrypt hash, and returns it in clear in that one response.
3. The screen shows the secret once, with a copy button and a warning that it will not be shown again. Closing the dialog is the end of its existence in readable form.
4. The manager sends the pair to whoever operates the external system.
5. The external system posts the pair to `/api/integration/token` and receives a short-lived token carrying its scopes.
6. It calls `GET /api/integration/orders`, `/routes` or `/deliveries` with that token.
7. The backend checks the token, checks that it holds the scope the path requires, and answers with the integration's own contract — not the panel's shapes.
8. Every call, accepted or refused, is written to the access log the manager reads on the same screen.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 3a | The manager loses the secret | "Gerar novo segredo" issues another and invalidates the previous one on the spot; whoever was using it stops working until they receive the new one. There is no recovery path — the secret is not stored. |
| 5a | Wrong secret, or a client identifier that does not exist | HTTP 401 with the same response in both cases: distinguishing them would confirm which identifiers exist. The attempt is logged with the identifier that was tried. |
| 6a | Token carries a scope the path does not require, or none of it | HTTP 403. The data is not served and the refusal is logged. |
| 6b | The path has no scope mapped on the server | HTTP 403 — denied by omission. An endpoint nobody mapped shows up as a defect instead of serving data with no scope at all. |
| 6c | A driver or panel token is presented | HTTP 401. All three token kinds are signed with the same secret, so each carries a `typ` claim and each parser refuses the others. |
| 6d | The credential was revoked or deleted after the token was issued | HTTP 403 on the next call, reported the same way in the response and in the log. |
| 8a | The manager deletes the credential | Access is cut immediately; the log of what it already accessed stays, now ownerless, because the record of a past access is the part worth keeping. |

**Post-conditions:**
- The external system reads only the areas it was granted, and only for reading.
- The manager can see, on one screen, every access and every refusal — including attempts with an identifier matching no credential.

**Out of scope:** external writes. An ERP creating an order brings validation and idempotency questions that deserve their own decision, and were deliberately left out.

---

## UC-12 — Grant and Revoke Panel Access

**ID:** UC-12
**Actors:** Manager, Web frontend, Backend
**Source Stories:** US-22

**Pre-conditions:**
- The manager is signed in. Until this version there was nothing to sign in to: the backend issued a token for any e-mail and the only credential check was a hardcoded pair in the web client.

**Main Flow:**
1. The manager signs in with e-mail and password; the backend checks the password against a BCrypt hash in `panel_users` and returns a session token.
2. The Next.js route handler stores the token in an `httpOnly` cookie, which the page's own JavaScript cannot read — and therefore neither can a cross-site scripting flaw.
3. Every subsequent call to the panel API carries that cookie, and the server refuses anything without a valid session.
4. The manager opens `/dashboard/usuarios` and creates an access: name, e-mail, password and status.
5. The new person signs in with that credential and reaches the same screens.
6. When someone leaves, the manager blocks or deletes their access; the next login attempt is refused.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 1a | Wrong password, or an e-mail with no account | HTTP 401 with one message for both cases: distinguishing them would tell whoever is guessing that the account exists. |
| 1b | Correct password, access blocked | HTTP 403 with its own message — and only after the password is checked, so that the status is not revealed to someone who does not hold the credential. |
| 3a | The session expires while the manager is working | The next call answers 401 and the browser is sent back to the login. The page guard alone would not catch this: someone sitting on one screen never navigates again. |
| 4a | E-mail already registered | Refused, naming the field — the database constraint alone would surface as a generic conflict. |
| 6a | Blocking or deleting the last active access | Refused. It would leave the panel with nobody able to enter, and the only way back would be an `UPDATE` straight into the database. |
| 6b | Deleting one's own access | Refused. It would drop the person mid-action while their session kept working until the token expired. |
| 6c | The blocked person still has a session open | It keeps working until the token expires — blocking stops the **next** login, not the one in progress. Revoking a live session would need server-side session state, which was not built. |

**Post-conditions:**
- Only people with a registered credential reach the panel, and with it the screens that grant access to the driver app and to integrations.
- The account's last login is recorded.

**Out of scope:** roles. Everyone who signs in reaches every screen; splitting admin from operator is authorisation, a separate concern.

---

## UC-13 — Read the Operation's Cost

**ID:** UC-13
**Actors:** Manager, Web frontend, Backend, Cost model
**Source Stories:** US-23

**Pre-conditions:**
- Routes exist in the chosen window. Refuellings make the estimate better but are not required.

**Main Flow:**
1. The manager signs in and lands on the dashboard, which asks the backend for a summary of the last 30 days.
2. The backend counts the routes created and concluded, the distance, the stops, the deliveries registered in the field and the orders still waiting for a route.
3. For each vehicle, it derives the distance between refuellings from the odometer history and measures consumption over a distance-weighted rolling window of the last five.
4. For each route in the window, it applies the project's cost model to the route's totals — distance, duration and cargo weight — producing fuel, tyre wear and driver hours in reais.
5. It sums what was actually spent on fuel in the window, from the refuelling records.
6. The screen shows the operation, the estimated cost split by component, the fuel actually purchased beside it, and consumption per vehicle marked as measured or as the profile's reference.
7. The manager switches the window to 7 or 90 days and the whole summary is recomputed.

**Alternative Flows:**

| Code | Trigger | System Response |
|------|---------|-----------------|
| 3a | A vehicle has fewer than two refuellings | Its profile's documented constant is used, and the row says so — "sem abastecimento" or "1 abastecimento — insuficiente". Two vehicles showing the same figure mean different things, and the table has to say which is which. |
| 3b | An odometer reading is lower than the previous one | That interval is skipped. It is a typo, and dividing by a negative or zero distance would produce a consumption figure that poisons the fleet average. |
| 4a | A route's orders carry no weight, or predate the stop↔order link | The load factor is zero and the truck is costed as empty — understated, not failed. |
| 4b | The route's vehicle was deleted afterwards | The lightest profile is assumed rather than failing: one missing vehicle should not take the whole screen down. |
| 5a | Nobody refuelled during the window | The purchased figure is R$ 0,00, stated as such, and the estimate uses the model's reference price — labelled "preço de referência" rather than passed off as what the fleet paid. |
| 6a | The two figures do not reconcile | Expected. The estimate covers routes planned here; the fuel purchased covers everything the trucks did, planned or not. The screen shows them side by side instead of subtracting one from the other. |

**Post-conditions:**
- The manager can see what the operation cost in reais, and which parts of that figure are measured and which are modelled.

**Out of scope:** cost per individual route on the route screen, and planned-versus-realised comparison for one route — the latter needs expenses recorded against a route, which the system does not do.
