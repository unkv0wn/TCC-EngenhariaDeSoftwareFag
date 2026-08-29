package com.routewise.validation;

/**
 * Fixed operational-cost assumptions for the empirical cost validation experiment.
 *
 * <p>These are <strong>not</strong> measured fleet data — no real telemetry is
 * available at this stage. They are documented, plausible constants per truck
 * class, used to validate the algorithmic logic before any real-world calibration.
 *
 * @param label                       human-readable name, used in reports
 * @param axleCount                   number of axles — descriptive only; tire wear is
 *                                    driven by {@code axleLayout}, not this count
 * @param axleLayout                  how many tires sit at each {@link AxlePosition}
 * @param capacityKg                  maximum load capacity, by weight
 * @param capacityM3                  maximum load capacity, by volume (cubic meters)
 * @param baseFuelConsumptionLPer100Km fuel consumption at empty load, on an ARTERIAL road
 * @param fuelPricePerLiter           diesel price (R$/liter)
 * @param tireReplacementCostPerTire  cost of replacing one tire (R$)
 * @param tireLifeKm                  expected distance a tire lasts at rated load (km),
 *                                    at the {@link AxlePosition#TRACAO} reference position
 * @param driverCostPerHourReais      driver cost (R$/hour)
 */
public record VehicleProfile(
  String label,
  int axleCount,
  AxleLayout axleLayout,
  double capacityKg,
  double capacityM3,
  double baseFuelConsumptionLPer100Km,
  double fuelPricePerLiter,
  double tireReplacementCostPerTire,
  double tireLifeKm,
  double driverCostPerHourReais
) {

  /** Small delivery van/truck (VUC) — 2 axles, 6 tires. */
  public static VehicleProfile light() {
    return new VehicleProfile(
      "Leve (2 eixos)", 2, AxleLayout.light(), 1500.0, 8.0, 10.0, 6.10, 900.0, 50_000.0, 30.0);
  }

  /** Medium delivery truck (Toco) — 3 axles, 10 tires. The profile used before truck-class comparison existed. */
  public static VehicleProfile medium() {
    return new VehicleProfile(
      "Médio (3 eixos)", 3, AxleLayout.medium(), 4000.0, 25.0, 15.0, 6.10, 1800.0, 60_000.0, 35.0);
  }

  /** Heavy articulated truck (Carreta) — 5 axles, 18 tires. */
  public static VehicleProfile heavy() {
    return new VehicleProfile(
      "Pesado (5 eixos)", 5, AxleLayout.heavy(), 12000.0, 90.0, 32.0, 6.10, 2200.0, 80_000.0, 42.0);
  }

  /** Backward-compatible alias for single-profile scripts — the medium truck. */
  public static VehicleProfile defaultProfile() {
    return medium();
  }

  /**
   * Copy of this profile with a different label — used by the calibration scripts to mark
   * a derived profile ("... (consumo empírico)") without restating every other field.
   */
  public VehicleProfile withLabel(String newLabel) {
    return new VehicleProfile(newLabel, axleCount, axleLayout, capacityKg, capacityM3,
      baseFuelConsumptionLPer100Km, fuelPricePerLiter, tireReplacementCostPerTire,
      tireLifeKm, driverCostPerHourReais);
  }

  /** Copy of this profile with fuel consumption replaced by an empirically derived value. */
  public VehicleProfile withFuelConsumption(double lPer100Km) {
    return new VehicleProfile(label, axleCount, axleLayout, capacityKg, capacityM3,
      lPer100Km, fuelPricePerLiter, tireReplacementCostPerTire,
      tireLifeKm, driverCostPerHourReais);
  }

  /** Copy of this profile with tire life replaced by an empirically derived value. */
  public VehicleProfile withTireLifeKm(double km) {
    return new VehicleProfile(label, axleCount, axleLayout, capacityKg, capacityM3,
      baseFuelConsumptionLPer100Km, fuelPricePerLiter, tireReplacementCostPerTire,
      km, driverCostPerHourReais);
  }
}
