package com.routewise.controller;

import com.routewise.dto.RouteRequestDto;
import com.routewise.service.IRouteOptimizerService;
import com.routewise.service.ISseEventService;
import com.routewise.service.osm.OsmGraphService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * REST controller for the RouteWise API.
 *
 * <h2>Endpoint Flow</h2>
 * <pre>
 *   1. POST /api/routes/compute  →  validates request, stores it,
 *                                    returns { requestId }
 *
 *   2. GET  /api/routes/events/{requestId}  →  creates SSE emitter,
 *                                                spawns Virtual Thread,
 *                                                streams events
 * </pre>
 *
 * <p>The client-side flow:
 * <ol>
 *   <li>Client sends {@code POST /api/routes/compute} with waypoints.</li>
 *   <li>Server validates, stores the request, and returns the
 *       {@code requestId}.</li>
 *   <li>Client immediately opens {@code GET /api/routes/events/{requestId}}
 *       to receive the SSE stream.</li>
 *   <li>Server creates the emitter and spawns a Java 21 Virtual Thread that
 *       runs the full computation pipeline.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

  private static final Logger log = LoggerFactory.getLogger(RouteController.class);

  private final IRouteOptimizerService routeOptimizerService;
  private final ISseEventService       sseEventService;
  private final OsmGraphService        osmGraphService;

  /**
   * Spring Boot 3.2 automatically configures this as a virtual-thread-backed
   * {@link org.springframework.scheduling.concurrent.SimpleAsyncTaskExecutor}
   * when {@code spring.threads.virtual.enabled=true}.
   */
  private final TaskExecutor taskExecutor;

  /** Pending requests awaiting SSE connection: requestId → RouteRequestDto */
  private final ConcurrentMap<String, RouteRequestDto> pendingRequests = new ConcurrentHashMap<>();

  public RouteController(
    IRouteOptimizerService routeOptimizerService,
    ISseEventService sseEventService,
    OsmGraphService osmGraphService,
    TaskExecutor taskExecutor
  ) {
    this.routeOptimizerService = routeOptimizerService;
    this.sseEventService       = sseEventService;
    this.osmGraphService       = osmGraphService;
    this.taskExecutor          = taskExecutor;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // POST /api/routes/compute
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Validates and queues a route computation request.
   *
   * @param request validated DTO (waypoints + routeMode)
   * @return 202 Accepted with a {@code requestId} for SSE subscription
   */
  @PostMapping("/compute")
  public ResponseEntity<Map<String, String>> submitRoute(
    @Valid @RequestBody RouteRequestDto request,
    @RequestHeader(value = "X-Request-ID", required = false) String correlationHeader
  ) {
    String requestId = UUID.randomUUID().toString();
    pendingRequests.put(requestId, request);

    log.info(
      "Route request queued: requestId={}, waypoints={}, mode={}, correlationHeader={}",
      requestId, request.waypoints().size(), request.routeMode(), correlationHeader
    );

    return ResponseEntity
      .accepted()
      .body(Map.of(
        "requestId", requestId,
        "message",   "Request accepted. Open /api/routes/events/" + requestId + " to stream results."
      ));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // GET /api/routes/events/{requestId}
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Opens an SSE stream for the given {@code requestId} and starts computation
   * in a Java 21 Virtual Thread.
   *
   * @param requestId must match a previously submitted compute request
   * @return SSE emitter that streams PROCESSING → COMPLETED | ERROR events
   */
  @GetMapping(value = "/events/{requestId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents(@PathVariable String requestId) {
    RouteRequestDto request = pendingRequests.remove(requestId);

    if (request == null) {
      log.warn("SSE stream requested for unknown requestId={}", requestId);
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "No pending route request found for requestId=" + requestId
        + ". Submit a POST to /api/routes/compute first."
      );
    }

    SseEmitter emitter = sseEventService.createEmitter(requestId);

    // Submit to the Spring-managed TaskExecutor.
    // With spring.threads.virtual.enabled=true, Spring Boot 3.2 automatically
    // configures this executor to use virtual threads (Loom), so no
    // Java 21-specific API calls are needed here — it compiles on any JDK.
    final String capturedId      = requestId;
    final RouteRequestDto capturedReq = request;
    taskExecutor.execute(() -> routeOptimizerService.compute(capturedReq, capturedId));

    log.info("SSE stream opened and computation dispatched: requestId={}", requestId);
    return emitter;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // GET /api/routes/graph-stats
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Returns OSM graph statistics (enabled only when an OSM data file is
   * configured).  Useful for TCC dashboard and debugging.
   *
   * @return graph stats JSON
   */
  @GetMapping("/graph-stats")
  public ResponseEntity<OsmGraphService.GraphStats> getGraphStats() {
    return ResponseEntity.ok(osmGraphService.getStats());
  }
}
