package com.routewise.validation;

/**
 * Fixed operational-cost assumptions for the empirical cost validation experiment.
 *
 * <p>These are <strong>not</strong> measured fleet data — no real telemetry is
 * available at this stage. They are documented, plausible constants per truck
 * class, used to validate the algorithmic logic before any real-world calibration.
 *
 * @param label                       human-readable name, used in reports
 * @param axleCount                   number of axles (affects tire wear)
 * @param capacityKg                  maximum load capacity, by weight
 * @param capacityM3                  maximum load capacity, by volume (cubic meters)
 * @param baseFuelConsumptionLPer100Km fuel consumption at empty load, on an ARTERIAL road
 * @param fuelPricePerLiter           diesel price (R$/liter)
 * @param tireReplacementCostPerTire  cost of replacing one tire (R$)
 * @param tireLifeKm                  expected distance a tire lasts at rated load (km)
 * @param driverCostPerHourReais      driver cost (R$/hour)
 */
public record VehicleProfile(
  String label,
  int axleCount,
  double capacityKg,
  double capacityM3,
  double baseFuelConsumptionLPer100Km,
  double fuelPricePerLiter,
  double tireReplacementCostPerTire,
  double tireLifeKm,
  double driverCostPerHourReais
) {

  /** Small delivery van/truck (VUC) — 2 axles. */
  public static VehicleProfile light() {
    return new VehicleProfile("Leve (2 eixos)", 2, 1500.0, 8.0, 10.0, 6.10, 900.0, 50_000.0, 30.0);
  }

  /** Medium delivery truck (Toco) — 3 axles. The profile used before truck-class comparison existed. */
  public static VehicleProfile medium() {
    return new VehicleProfile("Médio (3 eixos)", 3, 4000.0, 25.0, 15.0, 6.10, 1800.0, 60_000.0, 35.0);
  }

  /** Heavy articulated truck (Carreta) — 5 axles. */
  public static VehicleProfile heavy() {
    return new VehicleProfile("Pesado (5 eixos)", 5, 12000.0, 90.0, 32.0, 6.10, 2200.0, 80_000.0, 42.0);
  }

  /** Backward-compatible alias for single-profile scripts — the medium truck. */
  public static VehicleProfile defaultProfile() {
    return medium();
  }
}
