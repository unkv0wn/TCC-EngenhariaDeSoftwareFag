package com.routewise.dto;

import java.time.Instant;

/** Rota como um sistema externo a enxerga — sem id interno e sem código de acesso. */
public record IntegrationRouteDto(
  Instant createdAt,
  String status,
  String departureTime,
  String driverName,
  String vehiclePlate,
  int ordersCount,
  int totalStops,
  int deliveredStops,
  double totalDistanceKm
) {}
