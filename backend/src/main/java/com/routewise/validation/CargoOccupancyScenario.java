package com.routewise.validation;

/**
 * A named cargo load (weight + volume) used by {@link CargoOccupancyValidationScript}
 * to document how {@link CargoOccupancy} behaves across vehicle classes. Mirrors the
 * cases asserted by {@code CargoOccupancyTest} (JUnit), but evaluated against every
 * {@link VehicleProfile} instead of just one.
 *
 * @param name        short label, used as a table row header in the report
 * @param description one-line real-world framing (what kind of cargo this represents)
 * @param cargoWeightKg raw cargo weight, independent of any vehicle's capacity
 * @param cargoVolumeM3 raw cargo volume, independent of any vehicle's capacity
 */
public record CargoOccupancyScenario(
  String name,
  String description,
  double cargoWeightKg,
  double cargoVolumeM3
) {}
