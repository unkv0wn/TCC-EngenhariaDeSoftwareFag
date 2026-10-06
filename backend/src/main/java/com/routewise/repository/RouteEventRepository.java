package com.routewise.repository;

import com.routewise.entity.RouteEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteEventRepository extends JpaRepository<RouteEvent, UUID> {

  List<RouteEvent> findByRouteIdOrderByOccurredAtAsc(UUID routeId);

  /** Garante que o evento pertence à rota do path — evita apagar evento de outra rota pelo id. */
  Optional<RouteEvent> findByIdAndRouteId(UUID id, UUID routeId);

  /** Idempotência da fila offline: reenvio do mesmo evento devolve o que já foi gravado. */
  Optional<RouteEvent> findByClientEventId(UUID clientEventId);
}
