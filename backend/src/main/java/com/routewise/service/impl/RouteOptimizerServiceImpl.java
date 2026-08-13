package com.routewise.service.impl;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.algorithm.HaversineUtil;
import com.routewise.dto.RouteRequestDto;
import com.routewise.dto.RouteResultDto;
import com.routewise.dto.RouteResultDto.GeoJsonLineString;
import com.routewise.dto.RouteResultDto.OrderedWaypointDto;
import com.routewise.dto.RouteResultDto.OsrmValidationDto;
import com.routewise.dto.RouteResultDto.RouteSegmentDto;
import com.routewise.dto.SseEventDto;
import com.routewise.dto.WaypointDto;
import com.routewise.dto.osrm.OsrmRouteResponse;
import com.routewise.exception.OsrmClientException;
import com.routewise.exception.RouteComputationException;
import com.routewise.service.IOsrmClient;
import com.routewise.service.IRouteOptimizerService;
import com.routewise.service.ISseEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Main route optimisation pipeline — orchestrates A* + OSRM.
 *
 * <h2>Computation Pipeline</h2>
 * <ol>
 *   <li>Emit {@code PROCESSING} event via SSE.</li>
 *   <li>Call OSRM {@code /table} to build an N×N duration matrix; falls back to a
 *       {@link HaversineUtil}-distance estimate at a constant average speed if OSRM
 *       is unavailable (see below).</li>
 *   <li>Run A* on the complete waypoint graph to find optimal visitation order.</li>
 *   <li>Build the ordered waypoint list; append origin if {@code ROUND_TRIP}.</li>
 *   <li>Call OSRM {@code /route} with the ordered waypoints to get geometry.</li>
 *   <li>Emit {@code COMPLETED} event with {@link RouteResultDto}.</li>
 * </ol>
 *
 * <h2>Graceful Degradation</h2>
 * Both OSRM calls degrade instead of aborting the whole request, but not the same way:
 * <ul>
 *   <li><strong>Step 2</strong> ({@code /table}): if the call fails, the duration
 *       matrix is approximated as straight-line distance ({@link HaversineUtil})
 *       divided by {@value #FALLBACK_AVG_SPEED_KMH} km/h. A* still runs and returns
 *       a valid visitation order — just one computed against a coarser matrix, since
 *       straight-line distance at a constant speed ignores real road geometry and
 *       traffic. This is the one degradation path that changes the optimisation
 *       input itself, not just what's rendered afterwards.</li>
 *   <li><strong>Step 5</strong> ({@code /route}): if the call fails, the result
 *       still contains the A*-optimised order but no route geometry from OSRM
 *       and the {@code osrmValidation} field is set to {@code UNAVAILABLE}.</li>
 * </ul>
 */
@Service
public class RouteOptimizerServiceImpl implements IRouteOptimizerService {

  private static final Logger log = LoggerFactory.getLogger(RouteOptimizerServiceImpl.class);

  /**
   * Average speed (km/h) assumed when approximating travel duration from
   * straight-line distance, used only when OSRM {@code /table} is unreachable.
   * A generic urban/mixed-road figure — not tuned per road type, since the
   * fallback has no road-type information to work with.
   */
  private static final double FALLBACK_AVG_SPEED_KMH = 40.0;

  /**
   * Colour palette for route segments. Each segment is assigned a colour by
   * cycling through this array ({@code index % length}), so the palette works
   * for any number of waypoints up to the system maximum of 10.
   */
  private static final String[] SEGMENT_COLORS = {
    "#4E79A7", "#F28E2B", "#E15759", "#76B7B2",
    "#59A14F", "#EDC948", "#B07AA1", "#FF9DA7",
    "#9C755F", "#BAB0AC"
  };

  private final IOsrmClient           osrmClient;
  private final AStarWaypointOptimizer aStarOptimizer;
  private final ISseEventService       sseEventService;

  public RouteOptimizerServiceImpl(
    IOsrmClient osrmClient,
    AStarWaypointOptimizer aStarOptimizer,
    ISseEventService sseEventService
  ) {
    this.osrmClient       = osrmClient;
    this.aStarOptimizer   = aStarOptimizer;
    this.sseEventService  = sseEventService;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // IRouteOptimizerService implementation
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  public void compute(RouteRequestDto request, String requestId) {
    log.info(
      "Route computation started: requestId={}, waypoints={}, mode={}",
      requestId, request.waypoints().size(), request.routeMode()
    );

    try {
      // ── Step 1: Notify client that computation is in progress ─────────
      sseEventService.emit(requestId, SseEventDto.processing(requestId));

      // ── Step 2: Fetch OSRM duration matrix (falls back to Haversine on failure) ──
      double[][] durationMatrix = fetchDurationMatrixWithFallback(request.waypoints(), requestId);

      validateMatrix(durationMatrix, request.waypoints().size(), requestId);

      // ── Step 3: Run A* to find optimal visitation order ───────────────
      List<Integer> optimalIndices = aStarOptimizer.optimize(durationMatrix, request.routeMode());
      log.info("A* optimal order: {} for requestId={}", optimalIndices, requestId);

      // ── Step 4: Build ordered waypoint list for OSRM route call ──────
      // For ROUND_TRIP, A* already appended origin (index 0) at the end.
      List<WaypointDto> orderedWaypoints = optimalIndices.stream()
        .map(i -> request.waypoints().get((int) i))
        .toList();

      // ── Step 5: Fetch route geometry from OSRM ────────────────────────
      RouteResultDto result = fetchOsrmRouteAndBuildResult(
        requestId, request, optimalIndices, orderedWaypoints
      );

      // ── Step 6: Emit completion event ─────────────────────────────────
      sseEventService.emit(requestId, SseEventDto.completed(requestId, result));
      sseEventService.complete(requestId);

      log.info(
        "Route computation completed: requestId={}, distance={}km, duration={}min",
        requestId,
        String.format("%.2f", result.totalDistanceKm()),
        String.format("%.1f", result.totalDurationMin())
      );

    } catch (RouteComputationException ex) {
      log.error("Route computation failed for requestId={}: {}", requestId, ex.getMessage());
      sseEventService.emit(requestId, SseEventDto.error(requestId, ex.getMessage()));
      sseEventService.complete(requestId);

    } catch (Exception ex) {
      log.error("Unexpected error during route computation for requestId={}", requestId, ex);
      sseEventService.emit(
        requestId,
        SseEventDto.error(requestId, "An unexpected error occurred: " + ex.getMessage())
      );
      sseEventService.complete(requestId);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Private helpers
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Calls OSRM {@code /route} to get geometry and builds the final
   * {@link RouteResultDto}. Degrades gracefully if OSRM is unavailable.
   */
  private RouteResultDto fetchOsrmRouteAndBuildResult(
    String requestId,
    RouteRequestDto request,
    List<Integer> optimalIndices,
    List<WaypointDto> orderedWaypoints
  ) {
    GeoJsonLineString      geometry       = null;
    List<RouteSegmentDto>  segments       = List.of();
    double                 totalDistKm    = 0;
    double                 totalDurMin    = 0;
    OsrmValidationDto      osrmValidation = OsrmValidationDto.unavailable();

    try {
      OsrmRouteResponse routeResponse = osrmClient.fetchRoute(orderedWaypoints);
      OsrmRouteResponse.OsrmRoute bestRoute = routeResponse.routes().get(0);

      totalDistKm    = bestRoute.distance() / 1000.0;
      totalDurMin    = bestRoute.duration() / 60.0;
      geometry       = GeoJsonLineString.of(bestRoute.geometry().coordinates());
      osrmValidation = new OsrmValidationDto(totalDistKm, totalDurMin, "SUCCESS");

      // Build per-segment breakdown from OSRM legs
      segments = buildSegments(bestRoute.legs(), optimalIndices);

    } catch (OsrmClientException ex) {
      log.warn(
        "OSRM route call failed for requestId={}; returning without geometry: {}",
        requestId, ex.getMessage()
      );
      // Graceful degradation: geometry and segments remain empty, osrmValidation = UNAVAILABLE
    }

    // Build OrderedWaypointDto list (A* output, excluding the duplicated
    // round-trip origin at the end, which is kept for the OSRM call)
    List<OrderedWaypointDto> annotatedWaypoints = buildAnnotatedWaypoints(
      optimalIndices, request, orderedWaypoints
    );

    return new RouteResultDto(
      requestId,
      request.routeMode(),
      annotatedWaypoints,
      totalDistKm,
      totalDurMin,
      geometry,
      segments,
      osrmValidation
    );
  }

  /**
   * Converts the OSRM {@code legs} list into a {@link RouteSegmentDto} list.
   *
   * <p>Each leg maps to one waypoint-to-waypoint segment. The geometry for
   * each segment is constructed by concatenating the coordinate lists of all
   * manoeuvre steps within that leg — this gives an exact per-segment polyline
   * rather than an approximation.
   *
   * <p>If a leg has no steps (e.g. OSRM degraded response), the segment
   * geometry will be an empty LineString and the metrics will still be populated
   * from the leg-level distance and duration.
   *
   * @param legs          OSRM per-leg data from the route response
   * @param optimalIndices A*-ordered waypoint indices used to derive fromWpIndex / toWpIndex
   * @return immutable list of {@link RouteSegmentDto}, one per leg
   */
  private List<RouteSegmentDto> buildSegments(
    List<OsrmRouteResponse.OsrmLeg> legs,
    List<Integer> optimalIndices
  ) {
    if (legs == null || legs.isEmpty()) {
      return List.of();
    }

    List<RouteSegmentDto> result = new ArrayList<>();

    for (int i = 0; i < legs.size(); i++) {
      OsrmRouteResponse.OsrmLeg leg = legs.get(i);

      // Merge all step coordinates into a single per-leg coordinate list.
      // Adjacent steps share a boundary point; we de-duplicate by skipping the
      // first coordinate of each step after the first one.
      List<List<Double>> legCoords = new ArrayList<>();
      List<OsrmRouteResponse.OsrmStep> steps = leg.steps();
      if (steps != null && !steps.isEmpty()) {
        for (int s = 0; s < steps.size(); s++) {
          OsrmRouteResponse.OsrmGeometry stepGeom = steps.get(s).geometry();
          if (stepGeom == null || stepGeom.coordinates() == null) continue;
          List<List<Double>> stepCoords = stepGeom.coordinates();
          // Skip the first coordinate of subsequent steps to avoid duplicates
          int startIdx = (s == 0) ? 0 : 1;
          for (int c = startIdx; c < stepCoords.size(); c++) {
            legCoords.add(stepCoords.get(c));
          }
        }
      }

      int fromWp = (i     < optimalIndices.size()) ? optimalIndices.get(i)     : i;
      int toWp   = (i + 1 < optimalIndices.size()) ? optimalIndices.get(i + 1) : i + 1;

      result.add(new RouteSegmentDto(
        i,
        fromWp,
        toWp,
        leg.distance() / 1000.0,
        leg.duration() / 60.0,
        GeoJsonLineString.of(legCoords),
        SEGMENT_COLORS[i % SEGMENT_COLORS.length]
      ));
    }

    log.debug("Built {} route segments from {} OSRM legs", result.size(), legs.size());
    return result;
  }

  /**
   * Converts the A* index ordering to an annotated {@link OrderedWaypointDto}
   * list. For ROUND_TRIP, the return-to-origin entry (last element = index 0)
   * is included so the frontend can render the closed loop.
   */
  private List<OrderedWaypointDto> buildAnnotatedWaypoints(
    List<Integer> optimalIndices,
    RouteRequestDto request,
    List<WaypointDto> orderedWaypoints
  ) {
    List<OrderedWaypointDto> result = new ArrayList<>();
    for (int seq = 0; seq < orderedWaypoints.size(); seq++) {
      WaypointDto wp = orderedWaypoints.get(seq);
      result.add(new OrderedWaypointDto(seq, wp.lat(), wp.lng()));
    }
    return result;
  }

  /**
   * Fetches the OSRM duration matrix, falling back to a Haversine-distance-based
   * estimate if the {@code /table} call fails. This is the fallback described in
   * the class Javadoc — the only one that feeds the A* optimiser itself, as
   * opposed to the {@code /route} fallback in {@link #fetchOsrmRouteAndBuildResult}
   * which only affects rendered geometry.
   */
  private double[][] fetchDurationMatrixWithFallback(List<WaypointDto> waypoints, String requestId) {
    try {
      return osrmClient.fetchDurationMatrix(waypoints).durations();
    } catch (OsrmClientException ex) {
      log.warn(
        "OSRM /table call failed for requestId={}; falling back to Haversine-based "
          + "duration estimate at {} km/h: {}",
        requestId, FALLBACK_AVG_SPEED_KMH, ex.getMessage()
      );
      return buildFallbackDurationMatrix(waypoints);
    }
  }

  /**
   * Builds an approximate duration matrix from straight-line distance at a
   * constant average speed. Coarser than OSRM (ignores road geometry, traffic,
   * and road type), but keeps the computation running instead of aborting.
   */
  private double[][] buildFallbackDurationMatrix(List<WaypointDto> waypoints) {
    int n = waypoints.size();
    double[][] matrix = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          continue;
        }
        WaypointDto from = waypoints.get(i);
        WaypointDto to = waypoints.get(j);
        double distanceKm = HaversineUtil.distanceKm(from.lat(), from.lng(), to.lat(), to.lng());
        matrix[i][j] = (distanceKm / FALLBACK_AVG_SPEED_KMH) * 3600.0;
      }
    }
    return matrix;
  }

  /** Validates that the OSRM duration matrix is non-null and square. */
  private void validateMatrix(double[][] matrix, int expectedSize, String requestId) {
    if (matrix == null || matrix.length != expectedSize) {
      throw new RouteComputationException(
        "OSRM duration matrix is invalid or has unexpected dimensions for requestId=" + requestId
      );
    }
    for (double[] row : matrix) {
      if (row == null || row.length != expectedSize) {
        throw new RouteComputationException(
          "OSRM duration matrix contains a malformed row for requestId=" + requestId
        );
      }
    }
  }
}
