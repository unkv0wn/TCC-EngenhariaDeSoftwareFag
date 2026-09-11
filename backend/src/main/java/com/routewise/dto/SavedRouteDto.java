package com.routewise.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/** Resposta de uma rota salva. */
public record SavedRouteDto(
  UUID id,
  Instant createdAt,
  String status,
  int completedStops,
  int ordersCount,
  double totalValue,
  double totalDistanceKm,
  double totalDurationMin,
  UUID driverId,
  UUID vehicleId,
  String departureTime,
  JsonNode result,
  JsonNode stops
) {}
