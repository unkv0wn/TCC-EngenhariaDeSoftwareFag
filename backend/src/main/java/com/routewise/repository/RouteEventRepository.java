package com.routewise.repository;

import com.routewise.entity.RouteEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteEventRepository extends JpaRepository<RouteEvent, UUID> {

  List<RouteEvent> findByRouteIdOrderByOccurredAtAsc(UUID routeId);

  /**
   * Eventos de várias rotas de uma vez — a lista de rotas do app precisa do progresso de
   * cada uma, e uma consulta por rota seria N+1 numa tela que abre a todo momento.
   */
  List<RouteEvent> findByRouteIdIn(Collection<UUID> routeIds);

  /** Garante que o evento pertence à rota do path — evita apagar evento de outra rota pelo id. */
  Optional<RouteEvent> findByIdAndRouteId(UUID id, UUID routeId);

  /** Idempotência da fila offline: reenvio do mesmo evento devolve o que já foi gravado. */
  Optional<RouteEvent> findByClientEventId(UUID clientEventId);
}
