package com.routewise.dto;

import java.time.Instant;
import java.util.UUID;

/** Resposta de um evento de rota. */
public record RouteEventDto(
  UUID id,
  UUID routeId,
  Integer stopIndex,
  String type,
  String authorRole,
  String authorName,
  String notes,
  Instant occurredAt,
  Instant createdAt
) {}
