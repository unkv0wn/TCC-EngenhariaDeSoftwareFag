package com.routewise.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.routewise.entity.SavedRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a correspondência parada ↔ pedido que fecha o ciclo do pedido.
 *
 * <p>Por que justamente isto: a ordem do array {@code stops} é a de seleção do gestor, e
 * a de {@code orderedWaypoints} é a que o A* escolheu — <strong>são diferentes por
 * definição</strong>, é para isso que o otimizador existe. Casar as duas por posição em
 * vez de por coordenada compila, passa em qualquer teste de "não lança", e faz a entrega
 * de uma parada fechar o pedido de outra. O erro não aparece em tela nenhuma: o pedido
 * errado simplesmente consta como entregue.
 */
class RouteStopResolverTest {

  private final RouteStopResolver resolver = new RouteStopResolver(new ObjectMapper());

  private static final UUID ORDER_A1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ORDER_A2 = UUID.fromString("11111111-1111-1111-1111-111111111112");
  private static final UUID ORDER_B1 = UUID.fromString("22222222-2222-2222-2222-222222222221");

  /**
   * Depósito em (0,0); o A* visita primeiro o cliente B (2,2) e depois o A (1,1) — e o
   * array {@code stops} vem na ordem oposta, A antes de B, como a tela o montou.
   */
  private SavedRoute routeWithOrderIds() {
    SavedRoute route = new SavedRoute(UUID.randomUUID());
    route.setResult("""
      {"orderedWaypoints":[
        {"lat":0.0,"lng":0.0},
        {"lat":2.0,"lng":2.0},
        {"lat":1.0,"lng":1.0},
        {"lat":0.0,"lng":0.0}
      ]}""");
    route.setStops("""
      [
        {"lat":1.0,"lng":1.0,"label":"Cliente A","ordersCount":2,
         "orderIds":["11111111-1111-1111-1111-111111111111","11111111-1111-1111-1111-111111111112"]},
        {"lat":2.0,"lng":2.0,"label":"Cliente B","ordersCount":1,
         "orderIds":["22222222-2222-2222-2222-222222222221"]}
      ]""");
    return route;
  }

  @Test
  @DisplayName("casa parada e pedido por coordenada, não pela posição no array")
  void mapsByCoordinateNotByArrayPosition() {
    Map<Integer, List<UUID>> byStop = resolver.orderIdsByStop(routeWithOrderIds());

    // A parada 1 é a primeira que o A* visita — o Cliente B, que está em SEGUNDO no
    // array `stops`. Casar por posição devolveria aqui os pedidos do Cliente A.
    assertEquals(List.of(ORDER_B1), byStop.get(1), "parada 1 é o Cliente B (2,2)");
    assertEquals(List.of(ORDER_A1, ORDER_A2), byStop.get(2), "parada 2 é o Cliente A (1,1)");
  }

  @Test
  @DisplayName("o retorno ao depósito do ROUND_TRIP não é parada")
  void returnLegIsNotAStop() {
    SavedRoute route = routeWithOrderIds();

    assertEquals(2, resolver.countStops(route));
    assertTrue(resolver.orderIdsByStop(route).keySet().stream().allMatch(index -> index >= 1 && index <= 2));
  }

  @Test
  @DisplayName("em OPEN_ROUTE o último waypoint é parada")
  void openRouteKeepsLastWaypoint() {
    SavedRoute route = new SavedRoute(UUID.randomUUID());
    route.setResult("""
      {"orderedWaypoints":[
        {"lat":0.0,"lng":0.0},
        {"lat":1.0,"lng":1.0},
        {"lat":2.0,"lng":2.0}
      ]}""");
    route.setStops("""
      [
        {"lat":1.0,"lng":1.0,"label":"A","orderIds":["11111111-1111-1111-1111-111111111111"]},
        {"lat":2.0,"lng":2.0,"label":"B","orderIds":["22222222-2222-2222-2222-222222222221"]}
      ]""");

    assertEquals(2, resolver.countStops(route));
    assertEquals(List.of(ORDER_A1), resolver.orderIdsByStop(route).get(1));
    assertEquals(List.of(ORDER_B1), resolver.orderIdsByStop(route).get(2));
  }

  @Test
  @DisplayName("todos os pedidos da rota saem sem repetição")
  void allOrderIdsIsFlatAndDistinct() {
    assertEquals(List.of(ORDER_B1, ORDER_A1, ORDER_A2), resolver.allOrderIds(routeWithOrderIds()));
  }

  @Test
  @DisplayName("rota salva antes do campo orderIds não quebra, só não propaga")
  void routeSavedBeforeOrderIdsExisted() {
    SavedRoute route = new SavedRoute(UUID.randomUUID());
    route.setResult("""
      {"orderedWaypoints":[{"lat":0.0,"lng":0.0},{"lat":1.0,"lng":1.0},{"lat":0.0,"lng":0.0}]}""");
    // Como as rotas antigas foram gravadas: com contagem, sem os ids.
    route.setStops("""
      [{"lat":1.0,"lng":1.0,"label":"Cliente A","ordersCount":1}]""");

    assertEquals(1, resolver.countStops(route), "a parada continua existindo");
    assertTrue(resolver.orderIdsByStop(route).isEmpty(), "sem vínculo: não há o que propagar");
    assertTrue(resolver.allOrderIds(route).isEmpty());
  }

  @Test
  @DisplayName("id corrompido é ignorado sem derrubar a leitura da rota")
  void malformedOrderIdIsSkipped() {
    SavedRoute route = new SavedRoute(UUID.randomUUID());
    route.setResult("""
      {"orderedWaypoints":[{"lat":0.0,"lng":0.0},{"lat":1.0,"lng":1.0}]}""");
    route.setStops("""
      [{"lat":1.0,"lng":1.0,"label":"A",
        "orderIds":["nao-e-uuid","11111111-1111-1111-1111-111111111111"]}]""");

    assertEquals(List.of(ORDER_A1), resolver.orderIdsByStop(route).get(1));
  }

  @Test
  @DisplayName("rota sem resultado de otimização não tem parada nenhuma")
  void routeWithoutOptimizationResult() {
    SavedRoute route = new SavedRoute(UUID.randomUUID());
    route.setResult("{}");
    route.setStops("[]");

    assertEquals(0, resolver.countStops(route));
    assertTrue(resolver.orderIdsByStop(route).isEmpty());
  }
}
