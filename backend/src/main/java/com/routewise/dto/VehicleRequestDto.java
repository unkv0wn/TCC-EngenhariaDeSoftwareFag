package com.routewise.dto;

import jakarta.validation.constraints.*;

/** Request body for creating/updating a vehicle. */
public record VehicleRequestDto(

  @NotBlank(message = "Informe a placa.")
  @Pattern(regexp = "^([A-Z]{3}-\\d{4}|[A-Z]{3}\\d[A-Z]\\d{2})$", message = "Informe uma placa válida (ABC-1234 ou ABC1D23).")
  String plate,

  @NotBlank(message = "Informe o modelo.")
  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String model,

  @NotBlank(message = "Informe a marca.")
  @Size(max = 100, message = "Máximo de 100 caracteres.")
  String brand,

  @Min(value = 1950, message = "Informe um ano válido.")
  @Max(value = 2100, message = "Informe um ano válido.")
  int year,

  @NotBlank(message = "Informe a cor.")
  @Size(max = 50, message = "Máximo de 50 caracteres.")
  String color,

  @Positive(message = "Informe uma capacidade válida.")
  double capacityKg,

  @NotBlank(message = "Informe o tipo de combustível.")
  @Pattern(regexp = "^(diesel|gasolina|etanol|eletrico)$", message = "Tipo de combustível inválido.")
  String fuelType

) {}
