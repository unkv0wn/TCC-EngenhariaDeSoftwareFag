package com.routewise.validation;

import com.routewise.dto.WaypointDto;

/**
 * Pairs a real, named landmark's label with its (approximate) coordinates — used by
 * {@link ToledoRouteComparisonExample} and reused by scripts that rerun the exact same
 * fixed real-world scenario under a different cost assumption.
 */
public record NamedWaypoint(String name, double lat, double lng) {

  public WaypointDto toDto() {
    return new WaypointDto(lat, lng);
  }
}
