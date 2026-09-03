package com.routewise.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Response shape for a refueling. `kmSincePrevious` não existe aqui — é derivado no front. */
public record RefuelingDto(
  UUID id,
  UUID vehicleId,
  UUID driverId,
  LocalDate date,
  double odometerKm,
  double litersRefueled,
  double pricePerLiter
) {}
