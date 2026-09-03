package com.routewise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for creating/updating a unit of measure. */
public record UnitRequestDto(

  @NotBlank(message = "Informe o código.")
  @Size(max = 10, message = "Máximo de 10 caracteres.")
  String code,

  @NotBlank(message = "Informe o nome.")
  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String name

) {}
