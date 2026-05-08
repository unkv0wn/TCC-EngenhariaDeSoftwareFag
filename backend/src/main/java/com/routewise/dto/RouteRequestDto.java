package com.routewise.dto;

import com.routewise.model.RouteMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Inbound request DTO for the route computation endpoint.
 *
 * @param waypoints list of 2–10 geographic coordinates (validated)
 * @param routeMode whether the route should return to origin or not
 */
public record RouteRequestDto(

  @NotNull(message = "Waypoints list must not be null")
  @Size(min = 2, max = 10, message = "Between 2 and 10 waypoints are required")
  @Valid
  List<WaypointDto> waypoints,

  @NotNull(message = "Route mode must not be null (ROUND_TRIP or OPEN_ROUTE)")
  RouteMode routeMode

) {}
