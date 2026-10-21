package com.routewise.service;

import com.routewise.entity.Order;
import com.routewise.entity.SavedRoute;
import com.routewise.exception.InvalidOrderStatusTransitionException;
import com.routewise.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Liga o ciclo de vida da rota ao do pedido.
 *
 * <p>Antes disto o laço ficava aberto: o pedido virava {@code faturado} na mão, a rota
 * nascia dos faturados, o motorista registrava a entrega em campo — e o pedido continuava
 * {@code faturado} para sempre. Os estados {@code em_rota} e {@code entregue} existiam na
 * máquina de transições sem que nada no sistema os alcançasse.
 *
 * <p>Quatro momentos, que juntos fecham o laço:
 * <ol>
 *   <li>rota criada → {@code faturado} vira {@code em_rota}</li>
 *   <li>entrega registrada numa parada → os pedidos dela viram {@code entregue}</li>
 *   <li>rota encerrada → o que sobrou em {@code em_rota} volta a {@code faturado}</li>
 *   <li>rota excluída → idem, senão o pedido fica preso em {@code em_rota} sem rota</li>
 * </ol>
 *
 * <h2>Duas regras que valem mais do que parecem</h2>
 *
 * <p><strong>Nada aqui lança por motivo de negócio.</strong> Um pedido que não está no
 * estado esperado é pulado, com log, nunca recusado. É o que impede uma entrega feita em
 * campo de falhar porque alguém cancelou o pedido no painel enquanto o caminhão rodava —
 * o evento é o fato primário, o status do pedido é derivado dele. Falha de banco, sim,
 * desfaz tudo junto: aí o evento também não deveria ter sido gravado.
 *
 * <p><strong>A transição vai por {@link IOrderService#changeStatus}</strong>, e não por
 * {@code setStatus} direto, pra passar pela mesma validação do painel e deixar o registro
 * na linha do tempo do pedido ({@code order_status_history}). Assim a transição automática
 * fica visível na tela do pedido como qualquer outra — ela não é invisível só por não ter
 * sido feita por uma pessoa.
 */
@Component
public class RouteOrderLifecycle {

  private static final Logger log = LoggerFactory.getLogger(RouteOrderLifecycle.class);

  private static final String FATURADO = "faturado";
  private static final String EM_ROTA = "em_rota";
  private static final String ENTREGUE = "entregue";

  private final OrderRepository orderRepository;
  private final IOrderService orderService;
  private final RouteStopResolver stopResolver;

  public RouteOrderLifecycle(
    OrderRepository orderRepository,
    IOrderService orderService,
    RouteStopResolver stopResolver
  ) {
    this.orderRepository = orderRepository;
    this.orderService = orderService;
    this.stopResolver = stopResolver;
  }

  /**
   * Rota criada: os pedidos dela saem da fila de faturados.
   *
   * <p>Acontece na criação, e não quando a rota muda para {@code em_rota}, de propósito.
   * A tela de gerar rota oferece exatamente os pedidos {@code faturado} — se eles ficassem
   * faturados enquanto a rota está apenas planejada, o mesmo pedido poderia ser colocado
   * em duas rotas, e dois caminhões sairiam com ele sem ninguém perceber. O preço é que o
   * pedido fica {@code em_rota} algumas horas antes de o caminhão sair; o status da própria
   * rota ({@code planejada}) desfaz essa ambiguidade, e despacho duplicado não teria
   * desfazimento nenhum.
   */
  public void onRouteCreated(SavedRoute route) {
    move(stopResolver.allOrderIds(route), FATURADO, EM_ROTA, "rota criada", route.getId());
  }

  /** Entrega registrada numa parada: só os pedidos daquela parada fecham. */
  public void onDelivered(SavedRoute route, int stopIndex) {
    List<UUID> orderIds = stopResolver.orderIdsByStop(route).getOrDefault(stopIndex, List.of());
    move(orderIds, EM_ROTA, ENTREGUE, "entrega na parada " + stopIndex, route.getId());
  }

  /**
   * Rota encerrada ou excluída: o que não foi entregue volta para a fila.
   *
   * <p>{@code em_rota → faturado} é o "Retorno à empresa" que a máquina de estados do
   * pedido já previa. Sem isto, encerrar uma rota com parada sem entrega deixaria aqueles
   * pedidos em {@code em_rota} para sempre — um estado que mente, porque não há rota em
   * andamento, e que os tira da fila de faturados sem nunca os entregar.
   */
  public void onRouteClosed(SavedRoute route, String reason) {
    move(stopResolver.allOrderIds(route), EM_ROTA, FATURADO, reason, route.getId());
  }

  /**
   * Move os pedidos que estão em {@code from} para {@code to}, pulando os demais.
   *
   * <p>O estado de origem é conferido antes de chamar o serviço, em vez de capturar a
   * exceção de transição inválida: o pedido fora do estado esperado é o caso normal aqui
   * (cancelado no painel, já entregue por um evento anterior, de uma rota antiga sem
   * {@code orderIds}), e caso normal não se trata por exceção.
   */
  private void move(List<UUID> orderIds, String from, String to, String reason, UUID routeId) {
    if (orderIds.isEmpty()) {
      return;
    }

    int moved = 0;
    int skipped = 0;

    for (UUID orderId : orderIds) {
      Order order = orderRepository.findById(orderId).orElse(null);
      if (order == null) {
        skipped++;
        continue;
      }
      if (!from.equals(order.getStatus())) {
        skipped++;
        continue;
      }

      try {
        orderService.changeStatus(orderId, to);
        moved++;
      } catch (InvalidOrderStatusTransitionException ex) {
        // Não deveria acontecer — o estado foi conferido logo acima. Se acontecer, é
        // sinal de que as duas máquinas de estado divergiram, e isso precisa aparecer.
        log.warn("Transição recusada de {} para {} no pedido {}: {}", from, to, orderId, ex.getMessage());
        skipped++;
      }
    }

    log.info(
      "Ciclo do pedido ({}): rota={}, {}->{}, movidos={}, ignorados={}",
      reason, routeId, from, to, moved, skipped
    );
  }
}
