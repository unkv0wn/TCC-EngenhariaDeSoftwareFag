package com.routewise.service;

import com.routewise.dto.WaypointDto;
import com.routewise.dto.osrm.OsrmRouteResponse;
import com.routewise.dto.osrm.OsrmTableResponse;

import java.util.List;

/**
 * Contract for the OSRM HTTP client.
 *
 * <p>Abstracts the OSRM routing engine behind an interface so that the
 * implementation can be swapped (e.g., self-hosted OSRM vs. public demo)
 * without touching business logic.
 */
public interface IOsrmClient {

  /**
   * Calls the OSRM {@code /table/v1/driving} endpoint to obtain a full
   * duration (and distance) matrix between all provided waypoints.
   *
   * @param waypoints list of 2–10 waypoints
   * @return OSRM table response containing the NxN duration and distance matrices
   * @throws com.routewise.exception.OsrmClientException if the request fails
   */
  OsrmTableResponse fetchDurationMatrix(List<WaypointDto> waypoints);

  /**
   * Calls the OSRM {@code /route/v1/driving} endpoint to obtain the full
   * route geometry and aggregated distance/duration for a given ordered
   * sequence of waypoints.
   *
   * @param orderedWaypoints waypoints in the desired visitation order
   * @return OSRM route response containing geometry and total metrics
   * @throws com.routewise.exception.OsrmClientException if the request fails
   */
  OsrmRouteResponse fetchRoute(List<WaypointDto> orderedWaypoints);
}
