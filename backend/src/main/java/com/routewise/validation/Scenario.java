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
 * @param roadType      N×N road type per directed edge {@code (i, j)}, sampled
 *                      independently of {@code (j, i)}
 * @param cargoWeightKg total cargo weight for the whole trip (simplification: no
 *                      partial deliveries). Raw, vehicle-independent — how much of a
 *                      given vehicle's capacity this uses is computed separately by
 *                      {@link CargoOccupancy}, since the same scenario is reused
 *                      across vehicle profiles for comparison.
 * @param cargoVolumeM3 total cargo volume for the whole trip, same rationale as
 *                      {@code cargoWeightKg}
 */
public record Scenario(
  List<WaypointDto> waypoints,
  double[][] distanceKm,
  RoadType[][] roadType,
  double cargoWeightKg,
  double cargoVolumeM3
) {
  public int size() {
    return waypoints.size();
  }
}
