package com.routewise.dto;

import com.routewise.model.RouteMode;

import java.util.List;

/**
 * Full route computation result returned in the {@code COMPLETED} SSE event.
 *
 * @param requestId           correlation identifier for the request
 * @param routeMode           whether this is a round-trip or open route
 * @param orderedWaypoints    waypoints in their optimised visitation order
 * @param totalDistanceKm     total road distance in kilometres
 * @param totalDurationMin    total estimated travel time in minutes
 * @param geometry            GeoJSON LineString of the full route
 * @param segments            per-segment breakdown with individual geometry,
 *                            metrics, and a dynamically assigned colour
 * @param osrmValidation      cross-validation data from OSRM (may be null)
 */
public record RouteResultDto(
  String requestId,
  RouteMode routeMode,
  List<OrderedWaypointDto> orderedWaypoints,
  double totalDistanceKm,
  double totalDurationMin,
  GeoJsonLineString geometry,
  List<RouteSegmentDto> segments,
  OsrmValidationDto osrmValidation
) {

  // ── Nested types ─────────────────────────────────────────────────────────

  /**
   * A single waypoint in the optimised route, annotated with its sequence index.
   *
   * @param sequenceIndex  0-based position in the final route
   * @param lat            latitude in degrees
   * @param lng            longitude in degrees
   */
  public record OrderedWaypointDto(int sequenceIndex, double lat, double lng) {}

  /**
   * GeoJSON LineString geometry.
   *
   * @param type        always {@code "LineString"}
   * @param coordinates list of [longitude, latitude] pairs (GeoJSON order)
   */
  public record GeoJsonLineString(String type, List<List<Double>> coordinates) {
    /** Factory for creating a LineString from a coordinate list. */
    public static GeoJsonLineString of(List<List<Double>> coordinates) {
      return new GeoJsonLineString("LineString", coordinates);
    }
  }

  /**
   * Per-segment breakdown of the route.
   *
   * <p>Each segment corresponds to one consecutive waypoint pair in the
   * optimised visitation order (e.g. A→B, B→C, C→D). The {@code color}
   * field is a hex colour string (e.g. {@code "#4E79A7"}) assigned from a
   * fixed 10-colour palette and is used by the frontend to render each
   * segment as a distinct polyline.
   *
   * @param segmentIndex  0-based position of this segment in the full route
   * @param fromWpIndex   index of the origin waypoint in {@code orderedWaypoints}
   * @param toWpIndex     index of the destination waypoint in {@code orderedWaypoints}
   * @param distanceKm    road distance of this segment in kilometres
   * @param durationMin   estimated travel time of this segment in minutes
   * @param geometry      GeoJSON LineString covering only this segment
   * @param color         hex colour string assigned to this segment (e.g. {@code "#4E79A7"})
   */
  public record RouteSegmentDto(
    int segmentIndex,
    int fromWpIndex,
    int toWpIndex,
    double distanceKm,
    double durationMin,
    GeoJsonLineString geometry,
    String color
  ) {}

  /**
   * Summary of OSRM's own route calculation, used for cross-validation.
   *
   * @param distanceKm   OSRM-computed distance in kilometres
   * @param durationMin  OSRM-computed duration in minutes
   * @param status       {@code "SUCCESS"} or {@code "UNAVAILABLE"}
   */
  public record OsrmValidationDto(double distanceKm, double durationMin, String status) {
    public static OsrmValidationDto unavailable() {
      return new OsrmValidationDto(0, 0, "UNAVAILABLE");
    }
  }
}
