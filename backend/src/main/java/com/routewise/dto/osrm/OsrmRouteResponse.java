package com.routewise.dto.osrm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Deserialised response from the OSRM {@code /route/v1/driving} endpoint.
 *
 * @param code      "Ok" on success
 * @param routes    list of computed routes (we use the first one)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OsrmRouteResponse(
  String code,
  List<OsrmRoute> routes
) {

  /**
   * A single OSRM route alternative.
   *
   * <p>When the request is made with {@code steps=true}, the {@code legs} list
   * contains one entry per waypoint-to-waypoint segment, each with its own
   * distance, duration, and ordered list of manoeuvre steps (with per-step geometry).
   *
   * @param distance total road distance in metres
   * @param duration total travel duration in seconds
   * @param geometry GeoJSON LineString of the full route
   * @param legs     per-segment breakdown (one per consecutive waypoint pair)
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OsrmRoute(
    double distance,
    double duration,
    OsrmGeometry geometry,
    List<OsrmLeg> legs
  ) {}

  /**
   * A single leg of the route — corresponds to one waypoint-to-waypoint segment.
   *
   * @param distance aggregated road distance of this leg in metres
   * @param duration aggregated travel duration of this leg in seconds
   * @param steps    ordered list of manoeuvre steps that make up this leg;
   *                 populated only when the route was requested with {@code steps=true}
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OsrmLeg(
    double distance,
    double duration,
    List<OsrmStep> steps
  ) {}

  /**
   * A single manoeuvre step within a leg.
   *
   * <p>Each step has its own GeoJSON geometry covering the road segment
   * from one manoeuvre point to the next.
   *
   * @param distance road distance of this step in metres
   * @param duration travel duration of this step in seconds
   * @param geometry GeoJSON LineString for this step's road segment
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OsrmStep(
    double distance,
    double duration,
    OsrmGeometry geometry
  ) {}

  /**
   * GeoJSON geometry returned by OSRM.
   *
   * <p>Coordinates are in {@code [longitude, latitude]} order (GeoJSON spec).
   *
   * @param type        always {@code "LineString"} when using {@code geometries=geojson}
   * @param coordinates list of [lng, lat] pairs
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record OsrmGeometry(
    String type,
    List<List<Double>> coordinates
  ) {}
}
