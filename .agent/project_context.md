# PROJECT TECH STACK (LIVING DOCUMENT)
> **Status:** FROZEN 🔒  
> **Date Frozen:** 2026-05-07 (revised — Angular → React migration)  
> **Project:** RouteWise — Urban Route Optimizer (TCC)  

---

## FRONTEND

- **Framework:** React 18+ (Vite)
- **Language:** TypeScript (strict mode, no `any`)
- **Map Library:** react-leaflet + leaflet.js (latest stable)
- **UI Components:** Custom React components (CSS modules or vanilla CSS)
- **State / Reactivity:** React hooks (`useState`, `useEffect`, `useCallback`, `useRef`) + Context API for SSE event bus
- **HTTP Client:** Fetch API (native) or Axios
- **SSE Consumer:** Native `EventSource` Web API (wrapped in custom hook `useSseListener`)
- **Styling:** Vanilla CSS (custom design tokens)
- **Build Tool:** Vite (`npm run dev` / `npm run build`)
- **Map Tiles:** OpenStreetMap (free, no API key) — `https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`

---

## BACKEND

- **Language:** Java 21 LTS
- **Framework:** Spring Boot 3.x
- **Build Tool:** Apache Maven
- **API Style:** REST (synchronous handshake) + SSE (async streaming)
- **Concurrency:** Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`)
- **HTTP Client (OSRM):** Spring `RestTemplate` (with 5s timeout config)
- **SSE Mechanism:** Spring `SseEmitter` (TTL: 180,000ms)
- **Input Validation:** Spring Boot Validation (`jakarta.validation`)
- **Error Handling:** `@RestControllerAdvice` global exception handler
- **JSON:** Jackson (default with Spring Boot)
- **Database:** ❌ NONE — system is fully stateless; results returned as JSON

---

## ROUTING ALGORITHM

- **Primary:** Self-implemented A* (A-star) on a graph built from OSM data
  - Phase 1: Haversine heuristic on graph nodes
  - Uses priority queue (min-heap) for open set
  - Graph stored in memory after first load
- **Validation layer:** OSRM Public Demo API (for cross-checking computed routes)
  - Base URL: `https://router.project-osrm.org` (configurable via `application.properties`)
  - Endpoint: `/route/v1/driving/{coordinates}?overview=full&geometries=geojson`
  - Abstracted behind `IOsrmClient` interface
- **TSP Strategy:** User-configurable:
  - `ROUND_TRIP` — returns to origin after visiting all waypoints
  - `OPEN_ROUTE` — ends at the last placed waypoint (no return)

---

## EXTERNAL SERVICES

- **Routing Validation:** OSRM Public Demo API (rate-limited; for validation only)
- **Map Tiles:** OpenStreetMap (no key required)
- **OSM Data:** OpenStreetMap raw data (downloaded for self-hosted A* graph construction)

---

## ARCHITECTURE

- **Pattern:** Event-Driven (SSE Publisher/Consumer)
- **Communication:** REST (POST to initiate) + SSE (GET to stream results)
- **In-flight state:** `requestId` (UUID) keyed `ConcurrentHashMap<String, SseEmitter>` — TTL 180s
- **Persistent state:** ❌ NONE — stateless by design
- **Output format:** JSON (route geometry + ordered waypoints + metadata)
- **No Auth:** Anonymous access. No JWT, no session cookies.
- **CORS:** Configured to allow React dev (`http://localhost:5173`) and prod origins.

---

## AGREEMENTS

- **Auth Strategy:** None (anonymous, stateless)
- **Database:** None (stateless — JSON output only)
- **Testing Framework:** JUnit 5 + Mockito (backend) / Vitest + React Testing Library (frontend)
- **Indentation:** 2 spaces (all files)
- **Interfaces:** Prefixed with `I` (e.g., `IOsrmClient`, `IRouteOptimizerService`)
- **Types (TS):** Prefixed with `T` (e.g., `TWaypoint`, `TRouteEvent`)
- **Equality:** Strict (`===` / `!==`) in TypeScript
- **No `any`:** Strict TypeScript only
- **No bare `except`:** Typed Java `catch` blocks only
- **Logging:** SLF4J + Logback (structured, no `System.out.println`)

---

## PORT MAP

| Service | Port |
|---------|------|
| React Dev Server (Vite) | 5173 |
| Spring Boot Backend | 8080 |
| OSRM (external, validation only) | 443 (HTTPS) |