package com.routewise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routewise.dto.MobileEventBatchResultDto;
import com.routewise.dto.MobileEventRequestDto;
import com.routewise.dto.MobileRouteDto;
import com.routewise.dto.MobileStopDto;
import com.routewise.dto.RouteEventDto;
import com.routewise.dto.RouteEventRequestDto;
import com.routewise.entity.Driver;
import com.routewise.entity.SavedRoute;
import com.routewise.entity.Vehicle;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.DriverRepository;
import com.routewise.repository.SavedRouteRepository;
import com.routewise.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Superfície do app do motorista. Tudo aqui entra por token, nunca por id.
 *
 * <p>O papel do autor é fixado em {@code motorista} e o nome é lido do cadastro do
 * motorista escalado — nenhum dos dois vem do corpo da requisição. Um aparelho em campo
 * não escolhe em nome de quem está falando.
 */
@Service
@Transactional
public class MobileRouteServiceImpl implements IMobileRouteService {

  private static final Logger log = LoggerFactory.getLogger(MobileRouteServiceImpl.class);

  private static final String AUTHOR_ROLE = "motorista";
  private static final String FALLBACK_AUTHOR_NAME = "Motorista";

  /** Tolerância pra reconhecer o waypoint de retorno como sendo o próprio depósito. */
  private static final double COORD_EPSILON = 1e-7;

  private final SavedRouteRepository routeRepository;
  private final DriverRepository driverRepository;
  private final VehicleRepository vehicleRepository;
  private final IRouteEventService routeEventService;
  private final ObjectMapper objectMapper;

  public MobileRouteServiceImpl(
    SavedRouteRepository routeRepository,
    DriverRepository driverRepository,
    VehicleRepository vehicleRepository,
    IRouteEventService routeEventService,
    ObjectMapper objectMapper
  ) {
    this.routeRepository   = routeRepository;
    this.driverRepository  = driverRepository;
    this.vehicleRepository = vehicleRepository;
    this.routeEventService = routeEventService;
    this.objectMapper      = objectMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public MobileRouteDto findByToken(String accessToken) {
    SavedRoute route = requireRoute(accessToken);

    Optional<Driver> driver = driverRepository.findById(route.getDriverId());
    Optional<Vehicle> vehicle = vehicleRepository.findById(route.getVehicleId());
    List<RouteEventDto> events = routeEventService.findByRoute(route.getId());
    JsonNode result = readJson(route.getResult());

    return new MobileRouteDto(
      route.getAccessToken(),
      route.getCreatedAt(),
      route.getStatus(),
      route.getDepartureTime(),
      route.getOrdersCount(),
      route.getTotalValue(),
      route.getTotalDistanceKm(),
      route.getTotalDurationMin(),
      driver.map(Driver::getFullName).orElse(FALLBACK_AUTHOR_NAME),
      vehicle.map(Vehicle::getPlate).orElse(null),
      vehicle.map(Vehicle::getModel).orElse(null),
      vehicle.map(Vehicle::getCapacityKg).orElse(0.0),
      buildStops(route, events),
      result,
      events
    );
  }

  /**
   * Casa cada waypoint otimizado com a parada de cliente mais próxima — mesma regra de
   * {@code routeMarkers} em {@code FrontEnd-Next/lib/routeResult.ts}.
   *
   * <p>O {@code orderedWaypoints} do A* não referencia a parada de origem: só traz
   * {@code lat/lng}. Por isso o rótulo é resolvido por proximidade, e não por posição.
   * O índice 0 é o depósito; em {@code ROUND_TRIP} o último waypoint é o retorno a ele
   * e também não vira parada.
   */
  private List<MobileStopDto> buildStops(SavedRoute route, List<RouteEventDto> events) {
    JsonNode waypoints = readJson(route.getResult()).path("orderedWaypoints");
    JsonNode stops = readJson(route.getStops());

    if (!waypoints.isArray() || waypoints.isEmpty()) {
      return List.of();
    }

    Set<Integer> delivered = events.stream()
      .filter(event -> "entrega".equals(event.type()) && event.stopIndex() != null)
      .map(RouteEventDto::stopIndex)
      .collect(Collectors.toSet());

    double depotLat = waypoints.get(0).path("lat").asDouble();
    double depotLng = waypoints.get(0).path("lng").asDouble();

    int last = waypoints.size() - 1;
    boolean lastIsReturn = waypoints.size() > 1 && isSameCoord(waypoints.get(last), depotLat, depotLng);
    int end = lastIsReturn ? last : waypoints.size();

    List<MobileStopDto> result = new ArrayList<>();
    for (int index = 1; index < end; index++) {
      JsonNode waypoint = waypoints.get(index);
      double lat = waypoint.path("lat").asDouble();
      double lng = waypoint.path("lng").asDouble();

      JsonNode nearest = nearestStop(stops, lat, lng);
      result.add(new MobileStopDto(
        index,
        nearest != null ? nearest.path("label").asText("") : "",
        lat,
        lng,
        nearest != null ? nearest.path("ordersCount").asInt(1) : 1,
        delivered.contains(index)
      ));
    }
    return result;
  }

  private boolean isSameCoord(JsonNode waypoint, double lat, double lng) {
    return Math.abs(waypoint.path("lat").asDouble() - lat) < COORD_EPSILON
        && Math.abs(waypoint.path("lng").asDouble() - lng) < COORD_EPSILON;
  }

  private JsonNode nearestStop(JsonNode stops, double lat, double lng) {
    if (!stops.isArray()) {
      return null;
    }
    JsonNode best = null;
    double bestDistance = Double.MAX_VALUE;
    for (JsonNode stop : stops) {
      double dLat = stop.path("lat").asDouble() - lat;
      double dLng = stop.path("lng").asDouble() - lng;
      double distance = dLat * dLat + dLng * dLng;
      if (distance < bestDistance) {
        bestDistance = distance;
        best = stop;
      }
    }
    return best;
  }

  @Override
  public RouteEventDto createEvent(String accessToken, MobileEventRequestDto request) {
    SavedRoute route = requireRoute(accessToken);
    return routeEventService.create(route.getId(), toServiceRequest(route, request));
  }

  @Override
  public List<MobileEventBatchResultDto> createEvents(String accessToken, List<MobileEventRequestDto> requests) {
    SavedRoute route = requireRoute(accessToken);

    List<MobileEventBatchResultDto> results = new ArrayList<>(requests.size());
    for (MobileEventRequestDto request : requests) {
      // Um item inválido não pode levar os outros junto: o motorista perderia registros
      // que já fez em campo e não tem como refazer.
      try {
        RouteEventDto event = routeEventService.create(route.getId(), toServiceRequest(route, request));
        results.add(MobileEventBatchResultDto.accepted(request.clientEventId(), event));
      } catch (IllegalArgumentException ex) {
        log.warn("Evento recusado no lote: clientEventId={}, motivo={}", request.clientEventId(), ex.getMessage());
        results.add(MobileEventBatchResultDto.rejected(request.clientEventId(), ex.getMessage()));
      }
    }
    return results;
  }

  private RouteEventRequestDto toServiceRequest(SavedRoute route, MobileEventRequestDto request) {
    String driverName = driverRepository.findById(route.getDriverId())
      .map(Driver::getFullName)
      .orElse(FALLBACK_AUTHOR_NAME);

    return new RouteEventRequestDto(
      request.stopIndex(),
      request.type(),
      AUTHOR_ROLE,
      driverName,
      request.notes(),
      request.occurredAt(),
      request.clientEventId()
    );
  }

  /**
   * Token desconhecido devolve 404, nunca 403 — um 403 confirmaria que aquele token
   * existe, o que transforma a URL não-adivinhável num alvo de tentativa.
   */
  private SavedRoute requireRoute(String accessToken) {
    return routeRepository.findByAccessToken(accessToken)
      .orElseThrow(() -> new ResourceNotFoundException("Rota não encontrada."));
  }

  private JsonNode readJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("JSON da rota corrompido no banco: " + ex.getOriginalMessage(), ex);
    }
  }
}
