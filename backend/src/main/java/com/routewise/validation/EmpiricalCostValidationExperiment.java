package com.routewise.validation;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.model.RouteMode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Standalone experiment validating whether empirical operational costs (fuel, tire
 * wear) meaningfully change the route the A* optimizer selects, compared to the
 * time-only cost function used in production today.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.EmpiricalCostValidationExperiment}
 *
 * <p>See {@code docs/superpowers/specs/2026-07-29-empirical-cost-validation-design.md}
 * for the full design rationale. Does not touch the production route-computation
 * pipeline — reuses {@link AStarWaypointOptimizer} unmodified.
 */
public final class EmpiricalCostValidationExperiment {

  private static final int TRIALS = 500;
  private static final long SEED = 42L; // fixed for reproducibility
  private static final RouteMode MODE = RouteMode.ROUND_TRIP;
  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "empirical-cost-validation.md");

  public static void main(String[] args) {
    Random rng = new Random(SEED);
    ScenarioGenerator generator = new ScenarioGenerator(rng);
    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();
    VehicleProfile profile = VehicleProfile.defaultProfile();

    List<TrialResult> results = new ArrayList<>(TRIALS);

    for (int t = 0; t < TRIALS; t++) {
      Scenario scenario = generator.generate();
      EdgeCosts costs = CostMatrixBuilder.build(scenario, profile);

      List<Integer> orderA = optimizer.optimize(costs.costA(), MODE);
      List<Integer> orderB = optimizer.optimize(costs.costB(), MODE);

      results.add(TrialResultBuilder.build(scenario, profile, costs, orderA, orderB));
    }

    Summary summary = StatisticalAnalyzer.analyze(results);
    ReportWriter.write(REPORT_PATH, summary, profile);

    System.out.printf(Locale.US,
      "Experimento concluído: %d trials, %.1f%% com rota diferente, gap médio R$ %.2f (%.2f%%). Relatório: %s%n",
      summary.totalTrials(), summary.pctRoutesDiffer(),
      summary.gapReaisMean(), summary.gapPercentMean(),
      REPORT_PATH.toAbsolutePath().normalize()
    );
  }
}
