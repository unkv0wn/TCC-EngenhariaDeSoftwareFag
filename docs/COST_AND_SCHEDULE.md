# COST & SCHEDULE ESTIMATE
> **Project:** RouteWise — Urban Route Optimizer (TCC)  
> **Version:** 1.0  
> **Date:** 2026-04-14  
> **Author:** Resource Analyst Agent  
> **Rate:** R$ 80,00/hr  

---

## EXECUTIVE SUMMARY

| Metric | Value |
|--------|-------|
| **Total Estimated Hours** | **118 hours** |
| **Complexity Multiplier Applied** | 1.5× (Real-time SSE + A* Algorithm) |
| **Total Estimated Cost** | **R$ 14.160,00** |
| **Project Start** | 2026-04-14 |
| **Development End** | 2026-05-26 |
| **Final Deadline (+ 20% QA buffer)** | **2026-06-09** |

---

## WORK BREAKDOWN STRUCTURE (WBS)

### MODULE 1 — Project Foundation & Infrastructure

| Task | Specialist | Base Hrs | Multiplier | Adj. Hrs | Cost (R$) |
|------|-----------|----------|------------|----------|-----------|
| Spring Boot project scaffold (Maven, Java 21, VT config) | Backend | 2 | 1.0× | 2 | 160,00 |
| React project scaffold (Vite, react-leaflet, TypeScript) | Frontend | 2 | 1.0× | 2 | 160,00 |
| CORS config + basic health endpoint | Backend | 1 | 1.0× | 1 | 80,00 |
| OpenAPI / API contract doc (`api.yaml`) | Backend | 2 | 1.0× | 2 | 160,00 |
| Global error handling (`@RestControllerAdvice`) | Backend | 1 | 1.0× | 1 | 80,00 |
| **Module Subtotal** | | **8** | | **8** | **R$ 640,00** |

---

### MODULE 2 — OSM Graph & A* Engine (Core Algorithm)

| Task | Specialist | Base Hrs | Multiplier | Adj. Hrs | Cost (R$) |
|------|-----------|----------|------------|----------|-----------|
| OSM data parser (PBF/XML → in-memory graph) | Backend | 6 | 2.0× | 12 | 960,00 |
| Graph data structures (Node, Edge, AdjacencyList) | Backend | 3 | 2.0× | 6 | 480,00 |
| A* algorithm implementation (priority queue + Haversine heuristic) | Backend | 8 | 2.0× | 16 | 1.280,00 |
| Graph region subsetting & memory configuration | Backend | 2 | 2.0× | 4 | 320,00 |
| A* unit tests (JUnit 5, correctness + edge cases) | Backend | 4 | 1.0× | 4 | 320,00 |
| **Module Subtotal** | | **23** | | **42** | **R$ 3.360,00** |

---

### MODULE 3 — TSP Heuristic & Route Optimizer

| Task | Specialist | Base Hrs | Multiplier | Adj. Hrs | Cost (R$) |
|------|-----------|----------|------------|----------|-----------|
| Nearest-neighbour TSP heuristic implementation | Backend | 3 | 1.5× | 5 | 400,00 |
| Round-trip vs Open-route mode logic | Backend | 2 | 1.5× | 3 | 240,00 |
| Route result aggregation (geometry merge, distance/duration sum) | Backend | 3 | 1.5× | 5 | 400,00 |
| TSP + route optimizer unit tests | Backend | 3 | 1.0× | 3 | 240,00 |
| **Module Subtotal** | | **11** | | **16** | **R$ 1.280,00** |

---

### MODULE 4 — SSE Event System

| Task | Specialist | Base Hrs | Multiplier | Adj. Hrs | Cost (R$) |
|------|-----------|----------|------------|----------|-----------|
| `SseEmitter` lifecycle management (`ConcurrentHashMap`) | Backend | 3 | 1.5× | 5 | 400,00 |
| SSE event types: `PROCESSING`, `COMPLETED`, `ERROR` | Backend | 2 | 1.5× | 3 | 240,00 |
| Virtual Thread async dispatch (`@Async` removal, VT executor) | Backend | 2 | 1.5× | 3 | 240,00 |
| SSE error handling (client disconnect, TTL expiry) | Backend | 2 | 1.5× | 3 | 240,00 |
| SSE integration tests | Backend | 2 | 1.0× | 2 | 160,00 |
| **Module Subtotal** | | **11** | | **16** | **R$ 1.280,00** |

---

### MODULE 5 — OSRM Integration (Validation Layer)

| Task | Specialist | Base Hrs | Multiplier | Adj. Hrs | Cost (R$) |
|------|-----------|----------|------------|----------|-----------|
| `IOsrmClient` interface + `OsrmClientService` implementation | Backend | 3 | 1.0× | 3 | 240,00 |
| OSRM response mapper (geometry + distance + duration) | Backend | 2 | 1.0× | 2 | 160,00 |
| Graceful degradation logic (timeout + error absorption) | Backend | 2 | 1.0× | 2 | 160,00 |
| OSRM mock/unit tests | Backend | 2 | 1.0× | 2 | 160,00 |
| **Module Subtotal** | | **9** | | **9** | **R$ 720,00** |

---

### MODULE 6 — REST API Layer

| Task | Specialist | Base Hrs | Multiplier | Adj. Hrs | Cost (R$) |
|------|-----------|----------|------------|----------|-----------|
| `RouteController` (POST /compute + GET /events/{id}) | Backend | 3 | 1.0× | 3 | 240,00 |
| Input validation (`@Valid`, `@RequestBody` DTOs) | Backend | 2 | 1.0× | 2 | 160,00 |
| Controller unit tests (MockMvc) | Backend | 2 | 1.0× | 2 | 160,00 |
| **Module Subtotal** | | **7** | | **7** | **R$ 560,00** |

---

### MODULE 7 — React Frontend

| Task | Specialist | Base Hrs | Multiplier | Adj. Hrs | Cost (R$) |
|------|-----------|----------|------------|----------|-----------|
| Leaflet map integration (react-leaflet) + tile config | Frontend | 3 | 1.0× | 3 | 240,00 |
| Waypoint placement + numbered marker system | Frontend | 4 | 1.0× | 4 | 320,00 |
| Waypoint list panel + remove functionality | Frontend | 2 | 1.0× | 2 | 160,00 |
| Route mode toggle (Round Trip / Open Route) | Frontend | 1 | 1.0× | 1 | 80,00 |
| `useSseListener` hook (EventSource wrapper + state) | Frontend | 3 | 1.5× | 5 | 400,00 |
| Route polyline renderer + viewport fit | Frontend | 3 | 1.0× | 3 | 240,00 |
| Summary panel (distance, duration, mode) | Frontend | 2 | 1.0× | 2 | 160,00 |
| Loading spinner + status snackbar | Frontend | 1 | 1.0× | 1 | 80,00 |
| Reset flow + SSE cleanup | Frontend | 2 | 1.0× | 2 | 160,00 |
| React component unit tests (Vitest) | Frontend | 3 | 1.0× | 3 | 240,00 |
| **Module Subtotal** | | **24** | | **26** | **R$ 2.080,00** |

---

### MODULE 8 — QA, Security Audit & Documentation

| Task | Specialist | Base Hrs | Multiplier | Adj. Hrs | Cost (R$) |
|------|-----------|----------|------------|----------|-----------|
| Security audit (CORS, input validation, OSRM exposure) | Security | 3 | 1.0× | 3 | 240,00 |
| E2E test scenario: 3–5 waypoints → compute → verify render | QA | 4 | 1.0× | 4 | 320,00 |
| Architecture documentation (`docs/ARCHITECTURE.md`) | Docs | 2 | 1.0× | 2 | 160,00 |
| Deployment guide (`docs/DEPLOYMENT.md`) | Docs | 2 | 1.0× | 2 | 160,00 |
| Changelog (`CHANGELOG.md`) | Docs | 1 | 1.0× | 1 | 80,00 |
| **Module Subtotal** | | **12** | | **12** | **R$ 960,00** |

---

## TOTAL PROJECT COST

| Module | Adjusted Hours | Cost (R$) |
|--------|---------------|-----------|
| 1 — Foundation | 8 | 640,00 |
| 2 — OSM Graph & A* | 42 | 3.360,00 |
| 3 — TSP & Route Optimizer | 16 | 1.280,00 |
| 4 — SSE Event System | 16 | 1.280,00 |
| 5 — OSRM Integration | 9 | 720,00 |
| 6 — REST API | 7 | 560,00 |
| 7 — React Frontend | 26 | 2.080,00 |
| 8 — QA, Security & Docs | 12 | 960,00 |
| **TOTAL** | **136** | **R$ 10.880,00** |
| **+ 20% QA Buffer** | **+27** | **+ R$ 2.160,00** |
| **GRAND TOTAL** | **163 hrs** | **R$ 13.040,00** |

> **Note:** Hours reflect specialist work effort, not calendar time. Modules 2–6 are backend-sequential; Module 7 can start in parallel with Modules 5–6.

---

## MILESTONE TIMELINE

```
PHASE 1 — PLANNING         [2026-04-14 → 2026-04-14]  ✅ Done
  └── Requirements, Use Cases, Cost Estimation

PHASE 2 — DEVELOPMENT      [2026-04-15 → 2026-05-20]
  ├── Week 1  (Apr 15–21):  Foundation + OSM Parser + Graph Structures
  ├── Week 2  (Apr 22–28):  A* Algorithm + Unit Tests
  ├── Week 3  (Apr 29–May 5): TSP + Route Optimizer + SSE System
  ├── Week 4  (May 6–12):   OSRM Integration + REST API Layer
  ├── Week 5  (May 13–19):  React Frontend (Map + Waypoints + SSE)
  └── Week 6  (May 20):     React Frontend (Render + Summary Panel)

PHASE 3 — QA & SECURITY    [2026-05-21 → 2026-05-26]
  ├── E2E Testing (3–5 waypoints → compute → render)
  └── Security Audit (CORS, validation, OSRM exposure)

PHASE 4 — DOCUMENTATION    [2026-05-27 → 2026-06-02]
  └── Architecture, Deployment Guide, Changelog

PHASE 5 — BUFFER & DELIVERY [2026-06-03 → 2026-06-09]
  └── Final integration, polish, TCC submission preparation
```

---

## RISK REGISTER

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| OSM parser complexity underestimated | Medium | High | Allocate 2× multiplier; use existing OSM4J library if needed |
| A* memory issues with large OSM files | Medium | High | Use regional extract; configure JVM heap flags |
| OSRM public API rate-limiting | Low | Low | Graceful degradation already designed in |
| React SSE cross-origin issues | Low | Medium | CORS pre-configured; prefix `X-Request-ID` header |
| Academic calendar deadline collision | Medium | High | 20% buffer + modular architecture allows partial submission |
