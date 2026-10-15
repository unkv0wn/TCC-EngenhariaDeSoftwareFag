package com.routewise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routewise.dto.MobileStopDto;
import com.routewise.dto.RouteEventDto;
import com.routewise.entity.SavedRoute;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Traduz o resultado do A* guardado na rota para a lista de paradas que o motorista vê.
 *
 * <p>Existe como componente próprio porque dois serviços precisam da mesma definição de
 * "quais waypoints são paradas": {@link MobileRouteServiceImpl}, pra montar a tela, e
 * {@link RouteEventServiceImpl}, pra recusar um evento apontando pra parada que não
 * existe. Duplicar a regra abriria a porta pra elas divergirem — e aí o app mostraria
 * uma parada que o servidor não aceita, ou aceitaria um índice que o app não sabe exibir.
 */
@Component
public class RouteStopResolver {

  /** Tolerância pra reconhecer o waypoint de retorno como sendo o próprio depósito. */
  private static final double COORD_EPSILON = 1e-7;

  private final ObjectMapper objectMapper;

  public RouteStopResolver(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
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
  public List<MobileStopDto> resolve(SavedRoute route, List<RouteEventDto> events) {
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

  /**
   * Quantas paradas a rota tem — o maior {@code stopIndex} válido.
   *
   * <p>Zero quando a rota não tem resultado de otimização gravado: nesse caso não há
   * índice que possa ser conferido, e quem chama trata isso como "não valide".
   */
  public int countStops(SavedRoute route) {
    return resolve(route, List.of()).size();
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

  private JsonNode readJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("JSON da rota corrompido no banco: " + ex.getOriginalMessage(), ex);
    }
  }
}
