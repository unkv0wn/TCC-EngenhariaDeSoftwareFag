package com.routewise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.routewise.dto.MobileStopDto;
import com.routewise.dto.RouteEventDto;
import com.routewise.entity.SavedRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Traduz o resultado do A* guardado na rota para a lista de paradas que o motorista vê.
 *
 * <p>Existe como componente próprio porque vários serviços precisam da mesma definição de
 * "quais waypoints são paradas": {@link MobileRouteServiceImpl}, pra montar a tela,
 * {@link RouteEventServiceImpl}, pra recusar um evento apontando pra parada que não
 * existe, e {@link RouteOrderLifecycle}, pra saber quais pedidos uma entrega fecha.
 * Duplicar a regra abriria a porta pra elas divergirem — e aí o app mostraria uma parada
 * que o servidor não aceita, aceitaria um índice que o app não sabe exibir, ou (o pior)
 * uma entrega fecharia o pedido de outra parada.
 */
@Component
public class RouteStopResolver {

  private static final Logger log = LoggerFactory.getLogger(RouteStopResolver.class);

  /** Tolerância pra reconhecer o waypoint de retorno como sendo o próprio depósito. */
  private static final double COORD_EPSILON = 1e-7;

  private final ObjectMapper objectMapper;

  public RouteStopResolver(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Uma parada da rota: o índice que o app usa e o nó de {@code stops} que a descreve. */
  private record StopMatch(int index, JsonNode stop) {}

  /**
   * Casa cada waypoint otimizado com a parada de cliente mais próxima — mesma regra de
   * {@code routeMarkers} em {@code FrontEnd-Next/lib/routeResult.ts}.
   *
   * <p>O {@code orderedWaypoints} do A* não referencia a parada de origem: só traz
   * {@code lat/lng}. Por isso o rótulo é resolvido por proximidade, e não por posição.
   * O índice 0 é o depósito; em {@code ROUND_TRIP} o último waypoint é o retorno a ele
   * e também não vira parada.
   *
   * <p><strong>É a única travessia</strong> que decide o que é parada e com que índice.
   * Tudo que precisa dessa correspondência passa por aqui.
   */
  private List<StopMatch> matchStops(SavedRoute route) {
    JsonNode waypoints = readJson(route.getResult()).path("orderedWaypoints");
    JsonNode stops = readJson(route.getStops());

    if (!waypoints.isArray() || waypoints.isEmpty()) {
      return List.of();
    }

    double depotLat = waypoints.get(0).path("lat").asDouble();
    double depotLng = waypoints.get(0).path("lng").asDouble();

    int last = waypoints.size() - 1;
    boolean lastIsReturn = waypoints.size() > 1 && isSameCoord(waypoints.get(last), depotLat, depotLng);
    int end = lastIsReturn ? last : waypoints.size();

    List<StopMatch> matches = new ArrayList<>();
    for (int index = 1; index < end; index++) {
      JsonNode waypoint = waypoints.get(index);
      matches.add(new StopMatch(
        index,
        nearestStop(stops, waypoint.path("lat").asDouble(), waypoint.path("lng").asDouble())
      ));
    }
    return matches;
  }

  public List<MobileStopDto> resolve(SavedRoute route, List<RouteEventDto> events) {
    JsonNode waypoints = readJson(route.getResult()).path("orderedWaypoints");

    Set<Integer> delivered = events.stream()
      .filter(event -> "entrega".equals(event.type()) && event.stopIndex() != null)
      .map(RouteEventDto::stopIndex)
      .collect(Collectors.toSet());

    List<MobileStopDto> result = new ArrayList<>();
    for (StopMatch match : matchStops(route)) {
      JsonNode waypoint = waypoints.get(match.index());
      JsonNode stop = match.stop();
      result.add(new MobileStopDto(
        match.index(),
        stop != null ? stop.path("label").asText("") : "",
        waypoint.path("lat").asDouble(),
        waypoint.path("lng").asDouble(),
        stop != null ? stop.path("ordersCount").asInt(1) : 1,
        delivered.contains(match.index())
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
    return matchStops(route).size();
  }

  /**
   * Quais pedidos cada parada entrega, por {@code stopIndex}.
   *
   * <p>Vem de {@code orderIds} dentro de {@code stops}, gravado na geração da rota. Rotas
   * salvas antes desse campo existir devolvem mapa vazio — e é por isso que quem usa isso
   * precisa tratar "nenhum pedido" como caso normal, não como erro: não há como recuperar
   * o vínculo dessas rotas, o dado nunca foi registrado.
   */
  public Map<Integer, List<UUID>> orderIdsByStop(SavedRoute route) {
    Map<Integer, List<UUID>> byStop = new LinkedHashMap<>();
    for (StopMatch match : matchStops(route)) {
      List<UUID> ids = readOrderIds(match.stop());
      if (!ids.isEmpty()) {
        byStop.put(match.index(), ids);
      }
    }
    return byStop;
  }

  /** Todos os pedidos da rota, de todas as paradas. */
  public List<UUID> allOrderIds(SavedRoute route) {
    return orderIdsByStop(route).values().stream().flatMap(List::stream).distinct().toList();
  }

  private List<UUID> readOrderIds(JsonNode stop) {
    if (stop == null) {
      return List.of();
    }
    JsonNode ids = stop.path("orderIds");
    if (!ids.isArray()) {
      return List.of();
    }

    List<UUID> result = new ArrayList<>();
    for (JsonNode id : ids) {
      try {
        result.add(UUID.fromString(id.asText()));
      } catch (IllegalArgumentException ex) {
        // Um id corrompido não pode derrubar a leitura da rota inteira: a tela do
        // motorista depende desta travessia, e ela tem que abrir mesmo com dado ruim.
        log.warn("orderId inválido no JSON da rota, ignorado: {}", id.asText());
      }
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

  private JsonNode readJson(String json) {
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("JSON da rota corrompido no banco: " + ex.getOriginalMessage(), ex);
    }
  }
}
