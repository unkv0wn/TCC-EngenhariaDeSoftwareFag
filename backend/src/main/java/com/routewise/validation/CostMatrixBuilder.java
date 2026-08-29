package com.routewise.validation;

/**
 * Builds the Cenário A (time-only) and Cenário B (time + fuel + tire wear, in R$)
 * cost matrices for a given {@link Scenario} and {@link VehicleProfile}.
 *
 * <p>Formulas per directed edge {@code (i, j)} — see
 * {@code docs/superpowers/specs/2026-07-29-empirical-cost-validation-design.md}:
 * <pre>
 *   durationSec      = distanceKm / roadType.avgSpeedKmh * 3600
 *   loadFactor        = min(max(cargoWeightKg / capacityKg, cargoVolumeM3 / capacityM3), 1) — see {@link CargoOccupancy}
 *   consumoAjustado   = baseFuelConsumptionLPer100Km * (1 + 0.30 * loadFactor) * roadType.fuelMultiplier
 *   fuelLiters        = distanceKm * consumoAjustado / 100
 *   tireWearFraction  = Σ_posição [ (1 / (tireLifeKm * posição.lifeFactor)) * nºPneusNaPosição ]
 *                        * (1 + 0.5 * loadFactor) * roadType.wearMultiplier
 *   tireWearReais     = distanceKm * tireWearFraction * tireReplacementCostPerTire
 *   timeCostReais     = (durationSec / 3600) * driverCostPerHourReais
 *   costB             = timeCostReais + (fuelLiters * fuelPricePerLiter) + tireWearReais
 *   costA             = durationSec
 * </pre>
 *
 * <p>{@code loadFactor} is whichever of weight or volume occupancy is more restrictive
 * for this vehicle, clamped to {@code [0, 1]} since the formulas above are only
 * calibrated for that range — see {@link CargoOccupancy} for the uncapped fractions
 * and feasibility check.
 *
 * <p>Tire wear sums over {@link AxlePosition} rather than applying one averaged life to
 * every axle. Two things changed with that: each position now carries its own life
 * ({@link AxlePosition#lifeFactor}), and the per-position multiplier is the real tire
 * count from {@link AxleLayout} instead of {@code axleCount}. The latter was a
 * significant understatement — a 3-axle truck rolls on 10 tires, not 3 — so absolute
 * tire cost here is roughly 3x what the earlier formula produced.
 */
public final class CostMatrixBuilder {

  private static final double LOAD_FUEL_FACTOR = 0.30;
  private static final double LOAD_WEAR_FACTOR = 0.50;

  private CostMatrixBuilder() {}

  /**
   * Fraction of a tire's life consumed per kilometre, summed across every axle position,
   * before load and road-surface adjustment. Depends only on the vehicle, so it is
   * computed once per profile rather than per edge.
   */
  static double baseTireWearPerKm(VehicleProfile profile) {
    double wearPerKm = 0.0;
    for (AxlePosition position : AxlePosition.values()) {
      int tires = profile.axleLayout().tireCount(position);
      if (tires == 0) {
        continue;
      }
      wearPerKm += tires / (profile.tireLifeKm() * position.lifeFactor);
    }
    return wearPerKm;
  }

  public static EdgeCosts build(Scenario scenario, VehicleProfile profile) {
    int n = scenario.size();
    double loadFactor = CargoOccupancy.compute(scenario.cargoWeightKg(), scenario.cargoVolumeM3(), profile)
      .effectiveLoadFactor();
    double baseTireWearPerKm = baseTireWearPerKm(profile);

    double[][] durationSec = new double[n][n];
    double[][] fuelLiters = new double[n][n];
    double[][] tireWearReais = new double[n][n];
    double[][] costA = new double[n][n];
    double[][] costB = new double[n][n];

    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          continue;
        }

        double distanceKm = scenario.distanceKm()[i][j];
        RoadType roadType = scenario.roadType()[i][j];

        double duration = distanceKm / roadType.avgSpeedKmh * 3600.0;

        double consumoAjustado = profile.baseFuelConsumptionLPer100Km()
          * (1 + LOAD_FUEL_FACTOR * loadFactor)
          * roadType.fuelMultiplier;
        double fuel = distanceKm * consumoAjustado / 100.0;

        double tireWearFraction = baseTireWearPerKm
          * (1 + LOAD_WEAR_FACTOR * loadFactor)
          * roadType.wearMultiplier;
        double tireWear = distanceKm * tireWearFraction * profile.tireReplacementCostPerTire();

        double timeCostReais = (duration / 3600.0) * profile.driverCostPerHourReais();
        double fuelCostReais = fuel * profile.fuelPricePerLiter();

        durationSec[i][j] = duration;
        fuelLiters[i][j] = fuel;
        tireWearReais[i][j] = tireWear;
        costA[i][j] = duration;
        costB[i][j] = timeCostReais + fuelCostReais + tireWear;
      }
    }

    return new EdgeCosts(costA, costB, durationSec, fuelLiters, tireWearReais);
  }
}
