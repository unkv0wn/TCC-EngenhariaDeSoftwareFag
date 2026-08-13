package com.routewise.algorithm;

/**
 * Haversine formula utility for computing straight-line distances between two
 * geographic coordinates.
 *
 * <p><strong>Not used by the production A* heuristic.</strong>
 * {@link com.routewise.algorithm.AStarWaypointOptimizer} minimises OSRM travel
 * <em>duration</em> (seconds), not distance — a straight-line distance bound
 * is not directly comparable to a duration-based {@code g}/{@code h} without
 * an explicit distance→time conversion, which the optimizer does not
 * perform. Its heuristic instead sums each unvisited waypoint's minimum
 * incoming edge taken straight from the duration matrix itself (see that
 * class's Javadoc).
 *
 * <p>This utility is used elsewhere:
 * <ul>
 *   <li>{@code OsmGraphService}, to snap a requested coordinate to the
 *       nearest node in the loaded OSM graph.</li>
 *   <li>{@code RouteOptimizerServiceImpl}'s OSRM {@code /table} fallback, to
 *       approximate travel duration when OSRM is unavailable (distance ÷ a
 *       constant average speed).</li>
 *   <li>{@code com.routewise.validation}, to synthesise straight-line
 *       distance matrices for Monte Carlo scenarios where no real OSRM call
 *       is made ({@code ScenarioGenerator}, {@code ToledoRouteComparisonExample}
 *       and the other worked examples in that package).</li>
 * </ul>
 */
public final class HaversineUtil {

  private static final double EARTH_RADIUS_KM = 6371.0;

  private HaversineUtil() {}

  /**
   * Computes the great-circle distance between two points on Earth.
   *
   * @param lat1 latitude  of point 1 in decimal degrees
   * @param lng1 longitude of point 1 in decimal degrees
   * @param lat2 latitude  of point 2 in decimal degrees
   * @param lng2 longitude of point 2 in decimal degrees
   * @return distance in kilometres
   */
  public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
      + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
      * Math.sin(dLng / 2) * Math.sin(dLng / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS_KM * c;
  }

  /**
   * Converts kilometres to metres.
   *
   * @param km distance in kilometres
   * @return distance in metres
   */
  public static double toMetres(double km) {
    return km * 1000.0;
  }
}
