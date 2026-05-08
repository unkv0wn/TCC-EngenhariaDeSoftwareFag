package com.routewise.service;

import com.routewise.dto.RouteRequestDto;

/**
 * Contract for the main route optimisation orchestrator.
 *
 * <p>Implementations coordinate the full pipeline:
 * <ol>
 *   <li>Fetch OSRM duration matrix.</li>
 *   <li>Run A* to determine optimal waypoint order.</li>
 *   <li>Fetch OSRM route geometry for the optimised sequence.</li>
 *   <li>Emit SSE lifecycle events (PROCESSING → COMPLETED | ERROR).</li>
 * </ol>
 */
public interface IRouteOptimizerService {

  /**
   * Executes the full route optimisation pipeline for the given request,
   * streaming progress events to the SSE channel identified by
   * {@code requestId}.
   *
   * <p>This method is intended to run inside a Java 21 Virtual Thread started
   * by the SSE controller endpoint.
   *
   * @param request   validated route request (waypoints + routeMode)
   * @param requestId correlation ID — must match an active SSE emitter
   */
  void compute(RouteRequestDto request, String requestId);
}
