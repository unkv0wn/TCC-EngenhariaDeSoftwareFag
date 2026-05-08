package com.routewise.model;

/**
 * Defines how the computed route should terminate.
 *
 * <ul>
 *   <li>{@link #ROUND_TRIP} — the route returns to the origin after visiting
 *       all intermediate waypoints.</li>
 *   <li>{@link #OPEN_ROUTE} — the route ends at the last waypoint in the
 *       optimised order; no return to origin.</li>
 * </ul>
 */
public enum RouteMode {
  ROUND_TRIP,
  OPEN_ROUTE,
  FIXED_START_END
}
