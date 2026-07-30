package com.routewise.validation;

/**
 * Builds the Cenário A (time-only) and Cenário B (time + fuel + tire wear, in R$)
 * cost matrices for a given {@link Scenario} and {@link VehicleProfile}.
 *
 * <p>Formulas per directed edge {@code (i, j)} — see
 * {@code docs/superpowers/specs/2026-07-29-empirical-cost-validation-design.md}:
 * <pre>
 *   durationSec      = distanceKm / roadType.avgSpeedKmh * 3600
 *   consumoAjustado   = baseFuelConsumptionLPer100Km * (1 + 0.30 * loadFactor) * roadType.fuelMultiplier
 *   fuelLiters        = distanceKm * consumoAjustado / 100
 *   tireWearFraction  = (1 / tireLifeKm) * axleCount * (1 + 0.5 * loadFactor) * roadType.wearMultiplier
 *   tireWearReais     = distanceKm * tireWearFraction * tireReplacementCostPerTire
 *   timeCostReais     = (durationSec / 3600) * driverCostPerHourReais
 *   costB             = timeCostReais + (fuelLiters * fuelPricePerLiter) + tireWearReais
 *   costA             = durationSec
 * </pre>
 */
public final class CostMatrixBuilder {

  private static final double LOAD_FUEL_FACTOR = 0.30;
  private static final double LOAD_WEAR_FACTOR = 0.50;

  private CostMatrixBuilder() {}

  public static EdgeCosts build(Scenario scenario, VehicleProfile profile) {
    int n = scenario.size();

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
          * (1 + LOAD_FUEL_FACTOR * scenario.loadFactor())
          * roadType.fuelMultiplier;
        double fuel = distanceKm * consumoAjustado / 100.0;

        double tireWearFraction = (1.0 / profile.tireLifeKm())
          * profile.axleCount()
          * (1 + LOAD_WEAR_FACTOR * scenario.loadFactor())
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
