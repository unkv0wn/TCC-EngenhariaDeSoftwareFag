package com.routewise.validation;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.model.RouteMode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Same 500-trial statistical experiment as {@link EmpiricalCostValidationExperiment}, but run
 * with the **combined empirical profile** — {@code baseFuelConsumptionLPer100Km} and
 * {@code tireLifeKm} both calibrated from synthetic logs via
 * {@link EmpiricalFuelConsumptionCalculator} and {@link EmpiricalTireLifeCalculator} (same
 * logs and same calibration {@link CombinedEmpiricalCostExample} uses) — instead of the fixed,
 * documented-as-assumed {@link VehicleProfile#defaultProfile()}.
 *
 * <p>The point of this run: {@link EmpiricalCostValidationExperiment}'s "Gap" — how much R$
 * the operation leaves on the table following Cenário A (duration-only, today's production
 * behavior) instead of Cenário B (full R$ cost) — is only as trustworthy as the profile it's
 * computed against. Calibrating the profile with (synthetic, for now) real-world data doesn't
 * change the mechanism that saves money — switching route selection from Cenário A to Cenário
 * B still does that — but it changes how much confidence to put in the *size* of that saving.
 * Uses the same {@link #SEED} and {@link #TRIALS} as {@link EmpiricalCostValidationExperiment}
 * so the two reports are directly comparable, scenario-for-scenario — only the profile differs.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.CalibratedCostValidationExperiment}
 *
 * <p>See {@code docs/superpowers/specs/2026-08-01-empirical-fuel-consumption-rolling-window-design.md}
 * and {@code docs/superpowers/specs/2026-08-01-empirical-tire-wear-by-axle-position-design.md}
 * for the calibration design. Does not touch the production route-computation pipeline —
 * reuses {@link AStarWaypointOptimizer} unmodified.
 */
public final class CalibratedCostValidationExperiment {

  private static final int TRIALS = 500;
  private static final long SEED = 42L; // same seed as EmpiricalCostValidationExperiment, for a direct comparison
  private static final RouteMode MODE = RouteMode.ROUND_TRIP;
  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "empirical-cost-validation-perfil-calibrado.md");

  public static void main(String[] args) {
    VehicleProfile assumedProfile = VehicleProfile.defaultProfile();

    double empiricalConsumption = EmpiricalFuelConsumptionCalculator.consumptionLPer100Km(
      FuelConsumptionFromRefuelingExample.REFUELING_LOG, assumedProfile.baseFuelConsumptionLPer100Km()
    );
    double empiricalTireLifeKm = EmpiricalTireLifeCalculator.tireLifeKm(
      TireLifeFromReplacementLogExample.REPLACEMENT_LOG, assumedProfile.tireLifeKm()
    );
    VehicleProfile calibratedProfile = new VehicleProfile(
      assumedProfile.label() + " (calibrado: combustível + pneu empíricos)", assumedProfile.axleCount(),
      assumedProfile.capacityKg(), assumedProfile.capacityM3(),
      empiricalConsumption, assumedProfile.fuelPricePerLiter(),
      assumedProfile.tireReplacementCostPerTire(), empiricalTireLifeKm,
      assumedProfile.driverCostPerHourReais()
    );

    Random rng = new Random(SEED);
    ScenarioGenerator generator = new ScenarioGenerator(rng);
    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();

    List<TrialResult> results = new ArrayList<>(TRIALS);

    for (int t = 0; t < TRIALS; t++) {
      Scenario scenario = generator.generate();
      EdgeCosts costs = CostMatrixBuilder.build(scenario, calibratedProfile);

      List<Integer> orderA = optimizer.optimize(costs.costA(), MODE);
      List<Integer> orderB = optimizer.optimize(costs.costB(), MODE);

      results.add(TrialResultBuilder.build(scenario, calibratedProfile, costs, orderA, orderB));
    }

    Summary summary = StatisticalAnalyzer.analyze(results);
    ReportWriter.write(REPORT_PATH, summary, calibratedProfile);

    System.out.printf(Locale.US,
      "Experimento concluído (perfil calibrado): %d trials, %.1f%% com rota diferente, gap médio R$ %.2f (%.2f%%). Relatório: %s%n",
      summary.totalTrials(), summary.pctRoutesDiffer(),
      summary.gapReaisMean(), summary.gapPercentMean(),
      REPORT_PATH.toAbsolutePath().normalize()
    );
  }
}
