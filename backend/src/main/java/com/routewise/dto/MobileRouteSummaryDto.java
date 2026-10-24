package com.routewise.dto;

import java.time.Instant;

/**
 * Uma rota como ela aparece na lista da tela inicial do app.
 *
 * <p>Deliberadamente magra, ao contrário de {@link MobileRouteDto}: aquele carrega o
 * snapshot inteiro do A*, todas as paradas e todos os eventos, o que faz sentido para
 * abrir uma rota e não para listar várias. Numa lista de dez rotas o peso viria dez vezes,
 * por cima de rede de campo, para preencher cartões que mostram seis campos.
 *
 * <p>O {@code accessCode} vem junto porque é por ele que o app abre a rota — o celular
 * continua sem conhecer id interno, como no resto desta superfície.
 *
 * @param deliveredStops quantas paradas já têm entrega registrada
 * @param totalStops     quantas paradas a rota tem, sem contar o depósito
 */
public record MobileRouteSummaryDto(
  String accessCode,
  Instant createdAt,
  String status,
  String departureTime,
  int ordersCount,
  int totalStops,
  int deliveredStops,
  double totalDistanceKm,
  double totalDurationMin,
  String vehiclePlate,
  String vehicleModel
) {}
