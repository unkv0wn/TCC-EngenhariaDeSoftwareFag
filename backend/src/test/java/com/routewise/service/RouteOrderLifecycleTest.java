package com.routewise.service;

import com.routewise.entity.Order;
import com.routewise.entity.SavedRoute;
import com.routewise.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testa a propagação de status que fecha o ciclo do pedido.
 *
 * <p>O que estes testes protegem não é "funciona", é **não estragar**: a propagação toca
 * pedidos que ninguém está olhando no momento, e um pedido fechado por engano — ou
 * devolvido à fila por engano — só é descoberto quando alguém confere a mão. Daí a ênfase
 * em quem <em>não</em> deve ser movido.
 */
@ExtendWith(MockitoExtension.class)
class RouteOrderLifecycleTest {

  private static final UUID STOP1_ORDER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STOP2_ORDER_A = UUID.fromString("22222222-2222-2222-2222-222222222221");
  private static final UUID STOP2_ORDER_B = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Mock private OrderRepository orderRepository;
  @Mock private IOrderService orderService;
  @Mock private RouteStopResolver stopResolver;

  private RouteOrderLifecycle lifecycle;
  private SavedRoute route;

  @BeforeEach
  void setUp() {
    lifecycle = new RouteOrderLifecycle(orderRepository, orderService, stopResolver);
    route = new SavedRoute(UUID.randomUUID());
  }

  /** Parada 1 com um pedido; parada 2 com dois (mesmo cliente, mesmo endereço). */
  private void givenTwoStops() {
    lenient().when(stopResolver.orderIdsByStop(route)).thenReturn(Map.of(
      1, List.of(STOP1_ORDER),
      2, List.of(STOP2_ORDER_A, STOP2_ORDER_B)
    ));
    lenient().when(stopResolver.allOrderIds(route))
      .thenReturn(List.of(STOP1_ORDER, STOP2_ORDER_A, STOP2_ORDER_B));
  }

  private void givenOrder(UUID id, String status) {
    Order order = org.mockito.Mockito.mock(Order.class);
    lenient().when(order.getStatus()).thenReturn(status);
    lenient().when(orderRepository.findById(id)).thenReturn(Optional.of(order));
  }

  @Test
  @DisplayName("rota criada tira do faturado só o que está faturado")
  void routeCreatedMovesOnlyInvoicedOrders() {
    givenTwoStops();
    givenOrder(STOP1_ORDER, "faturado");
    givenOrder(STOP2_ORDER_A, "faturado");
    // Cancelado no painel depois de entrar na seleção da rota.
    givenOrder(STOP2_ORDER_B, "cancelado");

    lifecycle.onRouteCreated(route);

    verify(orderService).changeStatus(STOP1_ORDER, "em_rota");
    verify(orderService).changeStatus(STOP2_ORDER_A, "em_rota");
    verify(orderService, never()).changeStatus(eq(STOP2_ORDER_B), any());
  }

  @Test
  @DisplayName("a entrega fecha os pedidos da parada entregue, e só deles")
  void deliveryClosesOnlyThatStopsOrders() {
    givenTwoStops();
    givenOrder(STOP1_ORDER, "em_rota");
    givenOrder(STOP2_ORDER_A, "em_rota");
    givenOrder(STOP2_ORDER_B, "em_rota");

    lifecycle.onDelivered(route, 2);

    verify(orderService).changeStatus(STOP2_ORDER_A, "entregue");
    verify(orderService).changeStatus(STOP2_ORDER_B, "entregue");
    // O pedido da parada 1 não foi entregue — este é o erro que custaria caro.
    verify(orderService, never()).changeStatus(eq(STOP1_ORDER), any());
  }

  @Test
  @DisplayName("entrega repetida não mexe em pedido já entregue")
  void repeatedDeliveryIsIdempotent() {
    givenTwoStops();
    givenOrder(STOP1_ORDER, "entregue");

    lifecycle.onDelivered(route, 1);

    verify(orderService, never()).changeStatus(any(), any());
  }

  @Test
  @DisplayName("entrega numa parada sem vínculo de pedido não faz nada")
  void deliveryOnStopWithoutOrders() {
    givenTwoStops();

    lifecycle.onDelivered(route, 7);

    verifyNoInteractions(orderService);
  }

  @Test
  @DisplayName("rota encerrada devolve à fila o que não foi entregue")
  void closingRouteReturnsUndeliveredOrders() {
    givenTwoStops();
    givenOrder(STOP1_ORDER, "entregue");
    givenOrder(STOP2_ORDER_A, "em_rota");
    givenOrder(STOP2_ORDER_B, "em_rota");

    lifecycle.onRouteClosed(route, "rota encerrada");

    verify(orderService).changeStatus(STOP2_ORDER_A, "faturado");
    verify(orderService).changeStatus(STOP2_ORDER_B, "faturado");
    // Entregue não volta para a fila — seria desfazer uma entrega que aconteceu.
    verify(orderService, never()).changeStatus(eq(STOP1_ORDER), any());
  }

  @Test
  @DisplayName("rota antiga, sem orderIds gravados, não toca em pedido nenhum")
  void legacyRouteWithoutOrderIds() {
    when(stopResolver.allOrderIds(route)).thenReturn(List.of());

    lifecycle.onRouteCreated(route);

    verifyNoInteractions(orderService);
    verifyNoInteractions(orderRepository);
  }

  @Test
  @DisplayName("pedido apagado do banco é ignorado, não derruba a propagação")
  void deletedOrderIsSkipped() {
    givenTwoStops();
    when(orderRepository.findById(STOP1_ORDER)).thenReturn(Optional.empty());
    givenOrder(STOP2_ORDER_A, "faturado");
    givenOrder(STOP2_ORDER_B, "faturado");

    lifecycle.onRouteCreated(route);

    verify(orderService).changeStatus(STOP2_ORDER_A, "em_rota");
    verify(orderService).changeStatus(STOP2_ORDER_B, "em_rota");
    verify(orderService, never()).changeStatus(eq(STOP1_ORDER), any());
  }
}
