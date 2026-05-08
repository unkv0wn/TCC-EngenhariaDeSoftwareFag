# SYSTEM REQUIREMENTS SPECIFICATION (SRS)
> **Project:** RouteWise — Urban Route Optimizer  
> **Version:** 1.0  
> **Date:** 2026-04-14  
> **Status:** APPROVED  
> **Author:** Systems Analyst Agent  

---

## 1. INTRODUCTION

RouteWise is a stateless, full-stack academic web application for urban route optimization. It accepts N geographic waypoints from a user via an interactive map, computes the optimal visitation order using a self-hosted A* algorithm on OSM graph data, validates results optionally against the OSRM public API, and streams computation lifecycle events to the frontend via Server-Sent Events (SSE). All results are returned as JSON. No data is persisted.

---

## 2. FUNCTIONAL REQUIREMENTS (FR)

| ID     | Category          | Requirement                                                                                                                                              | Source Story |
|--------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|-------------|
| FR-01  | Input             | The system **shall** accept an ordered list of 2–10 geographic coordinates (latitude/longitude) via a REST endpoint `POST /api/routes/compute`.         | US-01, US-02 |
| FR-02  | Input             | The system **shall** accept a `routeMode` parameter with two valid values: `ROUND_TRIP` (route returns to origin) and `OPEN_ROUTE` (ends at last point). | US-02        |
| FR-03  | Input Validation  | The system **shall** reject requests with fewer than 2 or more than 10 waypoints with HTTP 400 and a descriptive error JSON body.                        | US-01        |
| FR-04  | Input Validation  | The system **shall** validate that each coordinate has `lat ∈ [-90, 90]` and `lng ∈ [-180, 180]`; invalid values return HTTP 400.                       | US-01        |
| FR-05  | Algorithm         | The system **shall** implement an A* pathfinding algorithm operating on an in-memory graph loaded from OSM data (OpenStreetMap PBF or XML format).       | US-02        |
| FR-06  | Algorithm         | The A* implementation **shall** use the Haversine formula as its heuristic function for geographic distance estimation.                                   | US-02        |
| FR-07  | Algorithm         | The system **shall** solve the inter-waypoint ordering problem using a nearest-neighbour TSP heuristic to determine the optimal visitation sequence.      | US-02        |
| FR-08  | Algorithm         | When `routeMode = ROUND_TRIP`, the system **shall** append the origin as the final destination in the computed route.                                     | US-02        |
| FR-09  | Algorithm         | When `routeMode = OPEN_ROUTE`, the system **shall** end the route at the last waypoint in the optimized order (no return to origin).                     | US-02        |
| FR-10  | Validation        | The system **shall** expose a secondary validation call to the OSRM public API for the same route and include the OSRM response in the output JSON.       | US-03        |
| FR-11  | Event Streaming   | The system **shall** publish route computation lifecycle events via SSE on endpoint `GET /api/routes/events/{requestId}`.                                 | US-04        |
| FR-12  | Event Streaming   | The SSE channel **shall** publish at minimum three event types: `PROCESSING`, `COMPLETED`, and `ERROR`.                                                  | US-04        |
| FR-13  | Event Streaming   | The `COMPLETED` event payload **shall** contain: ordered waypoint list, total distance (km), estimated duration (min), route geometry (GeoJSON LineString). | US-03        |
| FR-14  | Output            | The system **shall** return the full computation result as a well-formed JSON document in the `COMPLETED` SSE event and in the HTTP response body.        | US-03        |
| FR-15  | Health            | The system **shall** expose a `GET /actuator/health` endpoint returning HTTP 200 when operational.                                                        | NFR-04       |
| FR-16  | CORS              | The system **shall** allow CORS from `http://localhost:5173` in development and from configurable origins in production.                                  | NFR-05       |
| FR-17  | Observability     | The system **shall** log all inbound requests with a `requestId` correlation header (`X-Request-ID`) using structured SLF4J/Logback logging.             | NFR-06       |

---

## 3. NON-FUNCTIONAL REQUIREMENTS (NFR)

### 3.1 Performance

| ID      | Requirement                                                                                                                                              |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-01  | Total route computation (A* + TSP heuristic) for up to 10 waypoints **shall** complete within **5 seconds** under normal network conditions.             |
| NFR-02  | The backend **shall** use Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`) for all HTTP handlers to avoid thread-pool exhaustion.         |
| NFR-03  | The OSM graph **shall** be loaded into memory once at application startup; subsequent requests **shall not** reload graph data from disk.                |
| NFR-04  | The SSE emitter TTL **shall** be set to **180,000 ms** (3 minutes) to accommodate worst-case computation time with a generous buffer.                    |

### 3.2 Reliability

| ID      | Requirement                                                                                                                                              |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-05  | If OSRM validation call fails (timeout, HTTP error, network unreachable), the system **shall** return only the A* result and log the degraded state. The overall request **shall not** fail. |
| NFR-06  | If the SSE client disconnects before `COMPLETED`, the corresponding `SseEmitter` **shall** be removed from the in-flight map and resources released.     |
| NFR-07  | The system **shall** handle `SseEmitter.completeWithError()` gracefully without propagating exceptions to the HTTP thread.                               |

### 3.3 Security

| ID      | Requirement                                                                                                                                              |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-08  | All JSON input **shall** be validated via `jakarta.validation` annotations before processing begins.                                                     |
| NFR-09  | The OSRM base URL **shall** be configurable via `application.properties` and **never** hardcoded in source.                                              |
| NFR-10  | No secrets, API keys, or credentials exist in this system (anonymous, key-free). CORS must be explicitly configured — wildcard `*` is **not** permitted. |

### 3.4 Code Quality

| ID      | Requirement                                                                                                                                              |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-11  | All Java classes **shall** be annotated with appropriate Spring stereotypes (`@Service`, `@Controller`, `@Component`).                                   |
| NFR-12  | Service interfaces **shall** be prefixed with `I` (e.g., `IRouteOptimizerService`, `IOsrmClient`).                                                      |
| NFR-13  | No `System.out.println()` or bare `catch (Exception e) {}` blocks are permitted.                                                                        |
| NFR-14  | Code indentation: **2 spaces** in all Java, TypeScript, JSX/TSX, HTML, CSS, and YAML files.                                                             |
| NFR-15  | Unit tests **shall** exist for: A* algorithm correctness, TSP ordering, coordinate validation, and SSE event emission.                                   |

### 3.5 Portability

| ID      | Requirement                                                                                                                                              |
|---------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| NFR-16  | The backend **shall** be buildable and runnable via `mvn spring-boot:run` on any machine with Java 21+ installed.                                        |
| NFR-17  | OSM data file path **shall** be configurable via `application.properties` (e.g., `routewise.osm.data-path`).                                            |

---

## 4. SYSTEM REQUIREMENTS (SR)

| ID     | Category      | Requirement                                                                                                                          |
|--------|---------------|--------------------------------------------------------------------------------------------------------------------------------------|
| SR-01  | Runtime       | Java 21 LTS (minimum) must be installed on the host machine.                                                                        |
| SR-02  | Build         | Apache Maven 3.9+ must be available on the development machine.                                                                     |
| SR-03  | Memory        | Minimum **512 MB RAM** allocated to the JVM for in-memory OSM graph. Recommended: 1 GB for urban area graphs.                      |
| SR-04  | OSM Data      | An OSM data file (`.pbf` or `.osm.xml`) for the target geographic area must be present and accessible at the configured path.       |
| SR-05  | Network       | Outbound HTTPS to `router.project-osrm.org:443` required for OSRM validation (gracefully degraded if unavailable).                  |
| SR-06  | OS            | Linux, macOS, or Windows — JVM-agnostic. No native OS dependencies.                                                                 |
| SR-07  | Frontend      | Node.js 20+ and Vite required for React frontend development and build.                                                             |
| SR-08  | Browser       | Frontend **shall** run on latest stable Chrome, Firefox, and Edge. `EventSource` SSE API must be supported by the target browser.   |

---

## 5. REQUIREMENT TRACEABILITY MATRIX

| User Story | FR Coverage                               | NFR Coverage                   |
|------------|-------------------------------------------|--------------------------------|
| US-01      | FR-01, FR-03, FR-04                       | NFR-08, NFR-14                 |
| US-02      | FR-01, FR-02, FR-05, FR-06, FR-07, FR-08, FR-09 | NFR-01, NFR-02, NFR-03  |
| US-03      | FR-10, FR-13, FR-14                       | NFR-05, NFR-06                 |
| US-04      | FR-11, FR-12                              | NFR-04, NFR-06, NFR-07         |

---

## 6. CONSTRAINTS & ASSUMPTIONS

1. **No database:** The system is entirely stateless. Each request is independent.
2. **OSRM validation is optional:** If the OSRM API is down, the A* result is returned alone without failure.
3. **OSM graph scope:** The graph is pre-loaded for a specific geographic region defined by the operator (not dynamic).
4. **Cost metric:** No monetary cost model is implemented in Phase 1. "Cost" is represented by composite: distance (km) + duration (min). Phase 2 may add configurable weights.
5. **Authentication:** None. The system is anonymous and stateless.
6. **Rate limiting:** Not implemented in Phase 1. OSRM demo API usage must remain within community fair-use limits.
