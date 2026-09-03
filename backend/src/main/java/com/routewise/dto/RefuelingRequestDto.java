package com.routewise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

/** Request body for creating/updating a refueling. */
public record RefuelingRequestDto(

  @NotNull(message = "Selecione o veículo.")
  UUID vehicleId,

  @NotNull(message = "Selecione o motorista.")
  UUID driverId,

  @NotNull(message = "Informe a data.")
  @PastOrPresent(message = "A data não pode ser futura.")
  LocalDate date,

  @Positive(message = "Informe um valor de odômetro válido.")
  double odometerKm,

  @Positive(message = "Informe uma quantidade válida.")
  double litersRefueled,

  @Positive(message = "Informe um preço válido.")
  double pricePerLiter

) {}
