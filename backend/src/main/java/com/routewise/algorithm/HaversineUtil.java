package com.routewise.algorithm;

/**
 * Haversine formula utility for computing straight-line distances between two
 * geographic coordinates.
 *
 * <p>The Haversine distance is an <em>admissible heuristic</em> for A* on road
 * graphs because road distance is always ≥ straight-line distance (the
 * shortest possible path between two points on a sphere).
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
