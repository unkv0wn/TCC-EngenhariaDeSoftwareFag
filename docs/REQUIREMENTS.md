# REQUIREMENTS — RouteWise Urban Route Optimizer

> **Type:** Academic Full-Stack Web Application (TCC)  
> **Domain:** Urban Mobility / Smart City  
> **Date:** 2026-04-14  
> **Status:** LOCKED  

---

## USER STORIES

| ID    | As a…  | I want to…                                              | So that…                                             |
|-------|--------|---------------------------------------------------------|------------------------------------------------------|
| US-01 | User   | Place 2–10 waypoints on an interactive map              | I can define the stops of my journey                 |
| US-02 | User   | Click "Estimate Route" and choose a route mode          | The system finds the optimal visitation order         |
| US-03 | User   | See the computed route drawn on the map with metrics    | I can understand the best path visually               |
| US-04 | User   | Receive real-time status updates during computation     | The app feels responsive and alive                    |

## DECISIONS LOCKED

| Decision                  | Value                                               |
|---------------------------|-----------------------------------------------------|
| Java version              | 21 LTS with Virtual Threads                         |
| OSM parsing library       | osm4j (core + xml + pbf)                            |
| Primary routing engine    | OSRM (public demo API for costs + geometry)         |
| Optimization algorithm    | A* on complete waypoint graph (OSRM matrix weights) |
| Route modes               | ROUND_TRIP / OPEN_ROUTE (user-selectable)           |
| Persistence               | None — fully stateless JSON output                  |
| Authentication            | None — anonymous access                             |
| Cost metric               | Travel duration (seconds) via OSRM                  |
| Map tiles                 | OpenStreetMap (free, no API key)                    |
| Frontend framework        | React (Vite)                                        |
| Map library (frontend)    | react-leaflet + leaflet.js                          |

## ACCEPTANCE CRITERIA SUMMARY

- [ ] 2–10 waypoints accepted; validated server-side
- [ ] User selects ROUND_TRIP or OPEN_ROUTE before submitting
- [ ] A* finds optimal waypoint visitation order using OSRM duration matrix
- [ ] OSRM provides full route geometry (GeoJSON LineString)
- [ ] SSE streams: PROCESSING → COMPLETED (or ERROR)
- [ ] Result returned as JSON: ordered waypoints + geometry + distance + duration
- [ ] Backend response ≤ 5 seconds for 10 waypoints under normal conditions
- [ ] No business logic on the frontend
