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
  String fuelType
) {}
