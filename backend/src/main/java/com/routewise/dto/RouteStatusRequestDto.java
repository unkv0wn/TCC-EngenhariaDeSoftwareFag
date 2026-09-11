package com.routewise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Corpo pra mudar só o status de uma rota (ver ISavedRouteService#changeStatus). */
public record RouteStatusRequestDto(

  @NotNull(message = "Selecione o status.")
  @Pattern(regexp = "^(planejada|em_rota|concluida)$", message = "Status inválido.")
  String status

) {}
