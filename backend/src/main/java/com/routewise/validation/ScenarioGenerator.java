package com.routewise.validation;

import com.routewise.algorithm.HaversineUtil;
import com.routewise.dto.WaypointDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates synthetic trial scenarios for the empirical cost validation experiment.
 *
 * <p>Waypoints are scattered around the same São Paulo reference point used by the
 * legacy frontend's map default, within a ~50km radius — no real OSRM call is made.
 */
public final class ScenarioGenerator {

  private static final double CENTER_LAT = -23.5505;
  private static final double CENTER_LNG = -46.6333;
  private static final double SPREAD_DEGREES = 0.35; // ~ 35-40km around the center

  private static final int MIN_WAYPOINTS = 2;
  private static final int MAX_WAYPOINTS = 10;

  private final Random rng;

  public ScenarioGenerator(Random rng) {
    this.rng = rng;
  }

  public Scenario generate() {
    int n = MIN_WAYPOINTS + rng.nextInt(MAX_WAYPOINTS - MIN_WAYPOINTS + 1);
    return generate(n);
  }

  /** Generates a scenario with a fixed number of waypoints (must be in [2, 10]). */
  public Scenario generate(int n) {
    if (n < MIN_WAYPOINTS || n > MAX_WAYPOINTS) {
      throw new IllegalArgumentException(
        "n must be between " + MIN_WAYPOINTS + " and " + MAX_WAYPOINTS + ", got " + n
      );
    }

    List<WaypointDto> waypoints = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      double lat = CENTER_LAT + (rng.nextDouble() * 2 - 1) * SPREAD_DEGREES;
      double lng = CENTER_LNG + (rng.nextDouble() * 2 - 1) * SPREAD_DEGREES;
      waypoints.add(new WaypointDto(lat, lng));
    }

    double[][] distanceKm = new double[n][n];
    RoadType[][] roadType = new RoadType[n][n];
    RoadType[] roadTypes = RoadType.values();

    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          distanceKm[i][j] = 0.0;
          roadType[i][j] = RoadType.ARTERIAL;
          continue;
        }
        WaypointDto a = waypoints.get(i);
        WaypointDto b = waypoints.get(j);
        distanceKm[i][j] = HaversineUtil.distanceKm(a.lat(), a.lng(), b.lat(), b.lng());
        roadType[i][j] = roadTypes[rng.nextInt(roadTypes.length)];
      }
    }

    double loadFactor = rng.nextDouble();

    return new Scenario(waypoints, distanceKm, roadType, loadFactor);
  }
}
