package com.routewise.service;

import com.routewise.dto.RouteEventDto;
import com.routewise.dto.RouteEventRequestDto;
import com.routewise.entity.RouteEvent;
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

  public RouteEventServiceImpl(RouteEventRepository repository, SavedRouteRepository routeRepository) {
    this.repository = repository;
    this.routeRepository = routeRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RouteEventDto> findByRoute(UUID routeId) {
    requireRoute(routeId);
    return repository.findByRouteIdOrderByOccurredAtAsc(routeId).stream().map(this::toDto).toList();
  }

  @Override
  public RouteEventDto create(UUID routeId, RouteEventRequestDto request) {
    requireRoute(routeId);

    // Idempotência da fila offline: o app reenvia quando não teve certeza de que subiu.
    // Devolver o que já existe é o que impede uma entrega de ser contada duas vezes.
    if (request.clientEventId() != null) {
      Optional<RouteEvent> existing = repository.findByClientEventId(request.clientEventId());
      if (existing.isPresent()) {
        return toDto(existing.get());
      }
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

  private void requireRoute(UUID routeId) {
    if (!routeRepository.existsById(routeId)) {
      throw new ResourceNotFoundException("Rota não encontrada: " + routeId);
    }
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
