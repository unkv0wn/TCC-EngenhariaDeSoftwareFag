package com.routewise.validation;

import com.routewise.dto.WaypointDto;

import java.util.List;

/**
 * A single synthetic trial scenario: a set of waypoints, the distance between
 * every pair, the road type assigned to every directed edge, and the vehicle's
 * load for this trip.
 *
 * @param waypoints   synthetic lat/lng points
 * @param distanceKm  symmetric N×N straight-line distance matrix (simplification:
 *                    real road distance is not symmetric, but this is a synthetic
 *                    experiment, not a real routing engine)
 * @param roadType    N×N road type per directed edge {@code (i, j)}, sampled
 *                    independently of {@code (j, i)}
 * @param loadFactor  vehicle load as a fraction of capacity [0, 1], constant for
 *                    the whole trip (simplification: no partial deliveries)
 */
public record Scenario(
  List<WaypointDto> waypoints,
  double[][] distanceKm,
  RoadType[][] roadType,
  double loadFactor
) {
  public int size() {
    return waypoints.size();
  }
}
