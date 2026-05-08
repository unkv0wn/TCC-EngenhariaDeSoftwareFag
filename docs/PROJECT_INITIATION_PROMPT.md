# 🚀 PROJECT INITIATION PROMPT
> **To:** Orchestrator (Brain)
> **Role:** Product Owner → Systems Analyst → Project Manager
> **Date:** 2026-03-26

---

## 1. RAW IDEA (The Spark)

Build a full-stack web application for urban route optimization. Users can place multiple waypoints on an interactive map and request the system to compute the best route among them, optimizing for both speed and cost. The calculated route is rendered visually on the map in real time.

All business logic and routing calculations run exclusively on the backend. Initially, the focus should be on backend development; after validating the idea and once the system reaches a certain level of maturity, development can expand to the frontend.

On the backend, implement the A* (A-star) pathfinding algorithm to compute the optimal route between waypoints, ensuring efficient performance and high-quality results. You may use an OSRM (Open Source Routing Machine) service to generate routes before showing maps. In the initial stage, the system should return the result as a JSON response in the terminal.
---

## 2. PROJECT IDENTITY

| Field            | Value                             |
|------------------|-----------------------------------|
| **Project Name** | RouteWise (or TCC Route Optimizer)|
| **Domain**       | Urban Mobility / Smart City       |
| **Type**         | Academic Full-Stack Web App (TCC) |
| **Audience**     | Urban commuters, delivery planners, logistics teams |

---

## 3. TECHNOLOGY STACK (FROZEN — DO NOT CHANGE)

> These decisions are final and must be propagated to all specialist agents via `.agent/project_context.md`.

| Layer            | Technology                                      |
|------------------|-------------------------------------------------|
| **Frontend**     | React 18+ (Vite)                                |
| **Map Rendering**| react-leaflet + leaflet.js                      |
| **Backend**      | Java (Spring Boot)                              |
| **Routing Engine**| OSRM (Open Source Routing Machine) — self-hosted or public demo API |
| **Architecture** | Event-Driven (SSE or WebSockets)                |
| **Communication**| REST + Server-Sent Events (SSE) for async results |
| **Build Tool**   | Maven (backend) / Vite (frontend)               |

---

## 4. USER STORIES (The "What")

### US-01 — Waypoint Placement
**As a** user,
**I want to** click on a map to place multiple waypoints (N points),
**So that** I can define the origin, intermediate stops, and destination of my journey.

**Acceptance Criteria:**
- [ ] User can place a minimum of 2 and maximum of 10 waypoints on the map.
- [ ] Each waypoint is visually marked with a numbered pin.
- [ ] User can remove individual waypoints.
- [ ] Waypoint list is displayed alongside the map.

---

### US-02 — Route Estimation Request
**As a** user,
**I want to** click an "Estimate Route" button,
**So that** the system calculates and returns the most cost-efficient route between my waypoints.

**Acceptance Criteria:**
- [ ] The button is disabled if fewer than 2 waypoints are set.
- [ ] Clicking the button triggers an async event to the backend.
- [ ] The UI shows a loading/processing indicator while awaiting the result.
- [ ] The system must determine the optimal visitation order (TSP-like heuristic via OSRM).

---

### US-03 — Route Visualization
**As a** user,
**I want to** see the computed route drawn on the map,
**So that** I can visually understand the path and make informed travel decisions.

**Acceptance Criteria:**
- [ ] The route polyline is rendered on the map upon result arrival.
- [ ] A summary panel shows: total distance (km), estimated time (min), and relative cost score.
- [ ] Each waypoint on the route is highlighted in sequence.

---

### US-04 — Event-Driven Real-Time Feedback
**As a** user,
**I want to** receive feedback progressively as the route is computed,
**So that** the application feels responsive even during complex calculations.

**Acceptance Criteria:**
- [ ] The backend publishes route computation events (e.g., `PROCESSING`, `COMPLETED`, `ERROR`).
- [ ] The frontend consumes these events via SSE or WebSocket.
- [ ] Status messages are shown to the user in real time.

---

## 5. FUNCTIONAL REQUIREMENTS OVERVIEW

| ID   | Requirement                                                                 |
|------|-----------------------------------------------------------------------------|
| FR-01 | Accept N geographic coordinates (lat/lng) from the frontend                |
| FR-02 | Communicate with OSRM API to compute the optimal ordered route              |
| FR-03 | Apply a cost-efficiency heuristic (minimize distance + duration composite)  |
| FR-04 | Publish route computation lifecycle events (event-driven)                   |
| FR-05 | Return the ordered waypoint list + route geometry (GeoJSON/polyline)        |
| FR-06 | Frontend renders route on Leaflet map                                       |
| FR-07 | Frontend subscribes to backend events via SSE channel                       |

---

## 6. NON-FUNCTIONAL REQUIREMENTS OVERVIEW

| ID    | Requirement                                                                 |
|-------|-----------------------------------------------------------------------------|
| NFR-01 | Backend response for route computation ≤ 5 seconds for up to 10 waypoints |
| NFR-02 | No business logic on the frontend (pure presentation layer)                |
| NFR-03 | OSRM integration abstracted behind a backend service interface              |
| NFR-04 | Event channel must gracefully handle disconnections and retries             |
| NFR-05 | API must be stateless (RESTful contract)                                   |
| NFR-06 | Code must follow project global coding guidelines (2-space indent, typed)   |

---

## 7. SYSTEM ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────┐
│              REACT FRONTEND (Vite)            │
│  - Leaflet map (react-leaflet)               │
│  - Waypoint manager component                │
│  - SSE listener hook                         │
│  - Route display component                   │
└──────────────────┬──────────────────────────┘
                   │ REST (POST waypoints)
                   │ SSE  (event stream)
┌──────────────────▼──────────────────────────┐
│            SPRING BOOT BACKEND               │
│  - RouteController (REST API)                │
│  - RouteEventService (SSE Publisher)         │
│  - OsrmClientService (OSRM integration)      │
│  - RouteOptimizerService (business logic)    │
└──────────────────┬──────────────────────────┘
                   │ HTTP
┌──────────────────▼──────────────────────────┐
│          OSRM (Open Source Routing Machine)  │
│  - /route/v1/driving endpoint                │
│  - Returns geometry + duration + distance    │
└─────────────────────────────────────────────┘
```

---

## 8. ORCHESTRATOR PHASE INSTRUCTIONS

### PHASE 1 — Product Owner ✅ (Done — see above)
Stories US-01 through US-04 defined with acceptance criteria.

### PHASE 2 — Systems Analysis
> Call `/systems_analyst` to produce `docs/SYSTEM_REQUIREMENTS.md`.
> Ensure FR and NFR tables above are expanded with full technical precision.

### PHASE 3 — Use Case Analysis
> Call `/use_case_analyst` to produce `docs/USE_CASES.md`.
> Map use cases to each User Story. Flag dark corners:
> - What if OSRM returns no route? (islands / unreachable points)
> - What if the user drops a waypoint in the ocean?
> - What if the SSE connection drops mid-computation?

### PHASE 4 — Financial Estimation
> Call `/resource_analyst` to produce `docs/COST_AND_SCHEDULE.md`.
> Rate: R$ 80,00/hr. Present total hours, cost, and deadline for approval.

### PHASE 5 — Project Manager (Tech Stack Freeze + Build)
> Update `.agent/project_context.md` with the frozen stack from Section 3.
> Call `/design_system` → Custom React design tokens + CSS.
> Delegate:
> - `/dba` → (optional) persistence layer for route history
> - `/backend` → Spring Boot + OSRM client + SSE event publisher
> - `/web_dev` → React map UI + SSE consumer + route renderer

### PHASE 6 — Closure
> Call `/documentation` → update `CHANGELOG.md`, `docs/ARCHITECTURE.md`, `docs/DEPLOYMENT.md`.
> Call `/security_audit` → inspect OSRM endpoint exposure, CORS, input validation.
> Call `/quality_assurance` → E2E test: place 3–5 waypoints → estimate → verify route rendered.

---

## 9. OPEN QUESTIONS / DARK CORNERS FOR ORCHESTRATOR

These must be resolved before or during Systems Analysis:

1. **OSRM Hosting:** Use the public OSRM demo API (`router.project-osrm.org`) or self-host? *(Public is rate-limited and not for production.)*
2. **Optimization Strategy:** Pure shortest path, or a TSP heuristic over all N points? Should the user be able to lock waypoint order?
3. **Persistence:** Should computed routes be saved for later retrieval (requires a database)?
4. **Authentication:** Is user login required, or is the app anonymous/session-based?
5. **Cost Definition:** How is "cost" defined? Distance × fuel factor? Duration-based? A configurable weight?
6. **Map Tile Provider:** OpenStreetMap tiles (free) or a commercial provider (Mapbox)?

---

*Document generated by Senior Consultant role — 2026-03-26.*
*All phases must be executed sequentially unless Swarm mode is activated.*
