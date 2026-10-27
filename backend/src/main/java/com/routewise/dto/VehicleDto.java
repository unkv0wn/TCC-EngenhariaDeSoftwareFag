package com.routewise.dto;

import java.util.UUID;

/** Response shape for a vehicle. */
public record VehicleDto(
  UUID id,
  String plate,
  String model,
  String brand,
  int year,
  String color,
  double capacityKg,
  String fuelType,
  /** Leitura informada no cadastro; null quando não informada. */
  Double initialOdometerKm,
  /**
   * Maior leitura conhecida do veículo — a do cadastro ou a do último abastecimento, o
   * que for maior. Derivado a cada leitura e nunca armazenado: uma coluna mantida pelo
   * abastecimento ficaria errada ao editar um registro antigo, e nada avisaria.
   *
   * <p>Null quando o veículo não tem leitura nenhuma: nem cadastro, nem abastecimento.
   */
  Double currentOdometerKm
) {}
