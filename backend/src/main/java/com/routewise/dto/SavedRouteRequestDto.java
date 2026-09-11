package com.routewise.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/** Corpo pra salvar uma rota gerada. `result`/`stops` são o snapshot do algoritmo. */
public record SavedRouteRequestDto(

  @NotNull(message = "Selecione o motorista.")
  UUID driverId,

  @NotNull(message = "Selecione o veículo.")
  UUID vehicleId,

  @NotNull(message = "Informe o horário de saída.")
  @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de saída inválido.")
  String departureTime,

  @NotNull(message = "Informe a quantidade de pedidos.")
  @PositiveOrZero(message = "Quantidade de pedidos inválida.")
  Integer ordersCount,

  @NotNull(message = "Informe o valor total.")
  @PositiveOrZero(message = "Valor total inválido.")
  Double totalValue,

  @NotNull(message = "Informe o resultado da rota.")
  JsonNode result,

  @NotNull(message = "Informe as paradas da rota.")
  JsonNode stops

) {}
