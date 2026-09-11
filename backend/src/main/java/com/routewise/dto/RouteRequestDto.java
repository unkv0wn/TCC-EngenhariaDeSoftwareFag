package com.routewise.dto;

import com.routewise.model.RouteMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Inbound request DTO for the route computation endpoint.
 *
 * @param waypoints list of 2–15 geographic coordinates (validated). 15 is the practical
 *                  ceiling for the exact A* solver — state space is N × 2^N, so at 15 it's
 *                  still ~490k states (sub-second); past ~20 it'd need a heuristic instead.
 * @param routeMode whether the route should return to origin or not
 */
public record RouteRequestDto(

  @NotNull(message = "Waypoints list must not be null")
  @Size(min = 2, max = 15, message = "Between 2 and 15 waypoints are required")
  @Valid
  List<WaypointDto> waypoints,

  @NotNull(message = "Route mode must not be null (ROUND_TRIP or OPEN_ROUTE)")
  RouteMode routeMode

) {}
