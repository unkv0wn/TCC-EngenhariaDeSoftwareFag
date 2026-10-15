package com.routewise.service;

import com.routewise.dto.RouteEventDto;
import com.routewise.dto.RouteEventRequestDto;
import com.routewise.entity.RouteEvent;
import com.routewise.entity.SavedRoute;
import com.routewise.exception.ResourceNotFoundException;
import com.routewise.repository.RouteEventRepository;
import com.routewise.repository.SavedRouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class RouteEventServiceImpl implements IRouteEventService {

  /**
   * Tipos aceitos — espelha EVENT_TYPES de lib/validations/routeEvent.ts no front.
   * Lista fechada validada aqui, e não por anotação, pra a mensagem de erro poder
   * enumerar o que é aceito.
   */
  private static final Set<String> VALID_TYPES =
    Set.of("saida", "chegada", "entrega", "ocorrencia", "abastecimento", "retorno");

  /** Eventos que acontecem numa parada específica e, portanto, exigem `stopIndex`. */
  private static final Set<String> TYPES_REQUIRING_STOP = Set.of("chegada", "entrega");

  private static final Set<String> VALID_AUTHOR_ROLES = Set.of("motorista", "gestor");

  private final RouteEventRepository repository;
  private final SavedRouteRepository routeRepository;
  private final RouteStopResolver stopResolver;

  public RouteEventServiceImpl(
    RouteEventRepository repository,
    SavedRouteRepository routeRepository,
    RouteStopResolver stopResolver
  ) {
    this.repository = repository;
    this.routeRepository = routeRepository;
    this.stopResolver = stopResolver;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RouteEventDto> findByRoute(UUID routeId) {
    requireRoute(routeId);
    return repository.findByRouteIdOrderByOccurredAtAsc(routeId).stream().map(this::toDto).toList();
  }

  @Override
  public RouteEventDto create(UUID routeId, RouteEventRequestDto request) {
    SavedRoute route = requireRoute(routeId);

    // Idempotência da fila offline: o app reenvia quando não teve certeza de que subiu.
    // Devolver o que já existe é o que impede uma entrega de ser contada duas vezes.
    //
    // Vem antes das validações de propósito: um evento já gravado tem que continuar
    // sendo aceito mesmo que a regra tenha mudado desde então — em particular depois do
    // encerramento da rota, quando o reenvio do último lote cairia no bloqueio abaixo e
    // o app ficaria com eventos presos na fila, tentando para sempre.
    if (request.clientEventId() != null) {
      Optional<RouteEvent> existing = repository.findByClientEventId(request.clientEventId());
      if (existing.isPresent()) {
        return toDto(existing.get());
      }
    }

    // Rota encerrada é fechada para registro novo: depois dela o histórico é o que
    // alimenta relatório e conferência, e um evento chegando atrasado mudaria um número
    // que alguém já leu. Corrigir uma rota fechada é trabalho do gestor, na web.
    if (RouteStatus.CONCLUIDA.equals(route.getStatus())) {
      throw new IllegalArgumentException(
        "Esta rota já foi encerrada e não aceita novos registros. Fale com o gestor."
      );
    }

    String type = request.type().trim().toLowerCase();
    if (!VALID_TYPES.contains(type)) {
      throw new IllegalArgumentException(
        "Tipo de evento inválido: '" + request.type() + "'. Aceitos: " + String.join(", ", VALID_TYPES) + "."
      );
    }

    String authorRole = request.authorRole().trim().toLowerCase();
    if (!VALID_AUTHOR_ROLES.contains(authorRole)) {
      throw new IllegalArgumentException(
        "Autor inválido: '" + request.authorRole() + "'. Aceitos: " + String.join(", ", VALID_AUTHOR_ROLES) + "."
      );
    }

    if (TYPES_REQUIRING_STOP.contains(type) && request.stopIndex() == null) {
      throw new IllegalArgumentException("Eventos de '" + type + "' precisam indicar a parada.");
    }

    requireStopInRoute(route, request.stopIndex());

    RouteEvent entity = new RouteEvent(UUID.randomUUID());
    entity.setRouteId(routeId);
    entity.setStopIndex(request.stopIndex());
    entity.setType(type);
    entity.setAuthorRole(authorRole);
    entity.setAuthorName(request.authorName().trim());
    entity.setNotes(request.notes() == null || request.notes().isBlank() ? null : request.notes().trim());
    // Registrar algo que já aconteceu é o caso comum; sem `occurredAt` assume-se agora.
    entity.setOccurredAt(request.occurredAt() != null ? request.occurredAt() : Instant.now());
    entity.setClientEventId(request.clientEventId());

    // saveAndFlush, não save: o @CreationTimestamp só é preenchido no flush, e sem ele
    // a resposta sairia com `createdAt: null` — um campo do DTO mentindo.
    return toDto(repository.saveAndFlush(entity));
  }

  @Override
  public void delete(UUID routeId, UUID id) {
    RouteEvent entity = repository.findByIdAndRouteId(id, routeId)
      .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado na rota: " + id));
    repository.delete(entity);
  }

  /**
   * O índice tem que ser uma parada que esta rota realmente tem.
   *
   * <p>Sem isto, um evento apontando para a parada 9 de uma rota de 5 paradas entra
   * calado: não aparece em tela nenhuma, mas conta no histórico e no total de entregues.
   * Foi exatamente o que a fila offline, antes separada por rota, conseguia produzir ao
   * trocar de rota com eventos pendentes — e o servidor é o único lugar onde isso dá para
   * barrar de vez, inclusive para um app desatualizado.
   *
   * <p>Rota sem resultado de otimização gravado não tem índice que possa ser conferido;
   * nesse caso não há o que validar.
   */
  private void requireStopInRoute(SavedRoute route, Integer stopIndex) {
    if (stopIndex == null) {
      return;
    }

    int stops = stopResolver.countStops(route);
    if (stops == 0) {
      return;
    }

    if (stopIndex < 1 || stopIndex > stops) {
      throw new IllegalArgumentException(
        "A parada " + stopIndex + " não existe nesta rota, que tem " + stops + " paradas."
      );
    }
  }

  private SavedRoute requireRoute(UUID routeId) {
    return routeRepository.findById(routeId)
      .orElseThrow(() -> new ResourceNotFoundException("Rota não encontrada: " + routeId));
  }

  private RouteEventDto toDto(RouteEvent entity) {
    return new RouteEventDto(
      entity.getId(),
      entity.getRouteId(),
      entity.getStopIndex(),
      entity.getType(),
      entity.getAuthorRole(),
      entity.getAuthorName(),
      entity.getNotes(),
      entity.getOccurredAt(),
      entity.getCreatedAt()
    );
  }
}
