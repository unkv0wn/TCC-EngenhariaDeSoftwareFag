package com.routewise.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

/**
 * A rota como o app do motorista a enxerga.
 *
 * <p>Não carrega o id interno da rota nem os ids de motorista e veículo — o celular
 * navega por código, e entregar o UUID daria a ele o caminho pro CRUD do gestor
 * (que está aberto; ver "Authentication" no CLAUDE.md). Nome do motorista e dados do
 * veículo já vêm resolvidos, porque o app não tem como cruzar cadastro nenhum.
 */
public record MobileRouteDto(
  String accessCode,
  Instant createdAt,
  String status,
  String departureTime,
  int ordersCount,
  double totalValue,
  double totalDistanceKm,
  double totalDurationMin,
  String driverName,
  String vehiclePlate,
  String vehicleModel,
  double vehicleCapacityKg,
  /** Paradas na ordem otimizada, já casadas com o cliente e com o estado de entrega. */
  List<MobileStopDto> stops,
  /** Snapshot do A*: waypoints ordenados, segmentos e geometria — o mesmo que o gestor vê. */
  JsonNode result,
  List<RouteEventDto> events
) {}
