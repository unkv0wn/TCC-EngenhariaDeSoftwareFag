package com.routewise.service.impl;

import com.routewise.dto.WaypointDto;
import com.routewise.dto.osrm.OsrmRouteResponse;
import com.routewise.dto.osrm.OsrmTableResponse;
import com.routewise.exception.OsrmClientException;
import com.routewise.service.IOsrmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * OSRM HTTP client implementation.
 *
 * <p>Communicates with the OSRM routing engine using two endpoints:
 * <ul>
 *   <li>{@code /table/v1/driving} — returns an N×N duration/distance matrix.</li>
 *   <li>{@code /route/v1/driving} — returns the full route geometry.</li>
 * </ul>
 *
 * <p>OSRM coordinates use <strong>longitude-first</strong> order
 * ({@code lon,lat}) which is the opposite of our internal {@link WaypointDto}
 * order ({@code lat,lng}). The conversion is handled in
 * {@link #formatCoordinates(List)}.
 */
@Service
public class OsrmClientServiceImpl implements IOsrmClient {

  private static final Logger log = LoggerFactory.getLogger(OsrmClientServiceImpl.class);

  private final RestTemplate restTemplate;

  @Value("${routewise.osrm.base-url:https://router.project-osrm.org}")
  private String osrmBaseUrl;

  public OsrmClientServiceImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // IOsrmClient implementation
  // ─────────────────────────────────────────────────────────────────────────

  @Override
  public OsrmTableResponse fetchDurationMatrix(List<WaypointDto> waypoints) {
    String coords = formatCoordinates(waypoints);
    String url    = osrmBaseUrl
      + "/table/v1/driving/" + coords
      + "?annotations=duration";

    log.debug("OSRM table request: {}", url);

    OsrmTableResponse response = executeGet(url, OsrmTableResponse.class);
    validateOsrmCode(response.code(), "table");

    log.info(
      "OSRM table fetched: {}×{} duration matrix",
      response.durations().length, response.durations().length
    );
    return response;
  }

  @Override
  public OsrmRouteResponse fetchRoute(List<WaypointDto> orderedWaypoints) {
    String coords = formatCoordinates(orderedWaypoints);
    String url    = osrmBaseUrl
      + "/route/v1/driving/" + coords
      + "?overview=full&geometries=geojson&steps=true";

    log.debug("OSRM route request: {}", url);

    OsrmRouteResponse response = executeGet(url, OsrmRouteResponse.class);
    validateOsrmCode(response.code(), "route");

    if (response.routes() == null || response.routes().isEmpty()) {
      throw new OsrmClientException("OSRM returned no routes for the provided waypoints.");
    }

    OsrmRouteResponse.OsrmRoute route = response.routes().get(0);
    log.info(
      "OSRM route fetched: distance={}km, duration={}min",
      String.format("%.2f", route.distance() / 1000.0),
      String.format("%.1f", route.duration() / 60.0)
    );
    return response;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Private helpers
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Formats coordinates as OSRM expects them: {@code "lng,lat;lng,lat;..."}.
   *
   * <p><strong>IMPORTANT:</strong> {@link java.util.Locale#US} is passed explicitly to
   * {@link String#format} so that floating-point numbers always use a dot ({@code .})
   * as the decimal separator — regardless of the JVM's default locale.
   * On Brazilian systems ({@code pt_BR}) the default locale uses a comma ({@code ,})
   * as the decimal separator, which breaks the OSRM coordinate string.
   */
  private String formatCoordinates(List<WaypointDto> waypoints) {
    return waypoints.stream()
      .map(wp -> String.format(java.util.Locale.US, "%.6f,%.6f", wp.lng(), wp.lat()))
      .collect(Collectors.joining(";"));
  }

  /** Sends a GET request and deserialises the response. */
  private <T> T executeGet(String url, Class<T> responseType) {
    try {
      URI uri = URI.create(url);
      return restTemplate.getForObject(Objects.requireNonNull(uri), Objects.requireNonNull(responseType));
    } catch (ResourceAccessException ex) {
      throw new OsrmClientException(
        "OSRM service is unreachable (timeout or network error): " + ex.getMessage(), ex
      );
    } catch (RestClientResponseException ex) {
      throw new OsrmClientException(
        "OSRM returned HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString(),
        ex.getStatusCode().value()
      );
    }
  }

  /** Validates the OSRM status code field in the JSON body. */
  private void validateOsrmCode(String code, String endpoint) {
    if (!"Ok".equalsIgnoreCase(code)) {
      throw new OsrmClientException(
        "OSRM " + endpoint + " endpoint returned error code: " + code
      );
    }
  }
}
