package com.routewise.validation;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.model.RouteMode;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * Standalone worked example: simulates a single fixed scenario with 10 waypoints
 * and compares, side by side, the route chosen by Cenário A (time-only, today's
 * production behaviour) against Cenário B (time + fuel + tire wear).
 *
 * <p>Complements the aggregate statistical experiment in
 * {@link EmpiricalCostValidationExperiment} — that one answers "does this matter on
 * average across many scenarios?"; this one answers "what does it look like in one
 * concrete case?".
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.EmpiricalCostComparisonExample}
 */
public final class EmpiricalCostComparisonExample {

  private static final int WAYPOINT_COUNT = 10;
  private static final int MAX_SEED_ATTEMPTS = 1000;
  private static final double TARGET_GAP_PERCENT = 3.0;
  private static final RouteMode MODE = RouteMode.ROUND_TRIP;
  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "empirical-cost-comparison-example.md");

  public static void main(String[] args) {
    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();
    VehicleProfile profile = VehicleProfile.defaultProfile();

    // A single random 10-point scenario often has identical A/B routes (aggregate
    // experiment shows this happens ~71% of the time). To make a useful worked
    // example, search seeds for one where the empirical cost function actually
    // changes the chosen route, instead of just taking the first seed we try.
    long chosenSeed = -1;
    Scenario chosenScenario = null;
    EdgeCosts chosenCosts = null;
    List<Integer> chosenOrderA = null;
    List<Integer> chosenOrderB = null;
    double bestGapPercent = -1;

    for (long seed = 1; seed <= MAX_SEED_ATTEMPTS; seed++) {
      Scenario scenario = new ScenarioGenerator(new Random(seed)).generate(WAYPOINT_COUNT);
      EdgeCosts costs = CostMatrixBuilder.build(scenario, profile);

      List<Integer> orderA = optimizer.optimize(costs.costA(), MODE);
      List<Integer> orderB = optimizer.optimize(costs.costB(), MODE);

      if (orderA.equals(orderB)) {
        continue;
      }

      double costBUnderOrderA = TrialResultBuilder.sumAlongPath(costs.costB(), orderA);
      double costBUnderOrderB = TrialResultBuilder.sumAlongPath(costs.costB(), orderB);
      double gapPercent = costBUnderOrderA > 0
        ? 100.0 * (costBUnderOrderA - costBUnderOrderB) / costBUnderOrderA
        : 0.0;

      if (gapPercent > bestGapPercent) {
        bestGapPercent = gapPercent;
        chosenSeed = seed;
        chosenScenario = scenario;
        chosenCosts = costs;
        chosenOrderA = orderA;
        chosenOrderB = orderB;
      }

      if (gapPercent >= TARGET_GAP_PERCENT) {
        break;
      }
    }

    if (chosenScenario == null) {
      throw new IllegalStateException(
        "No seed among the first " + MAX_SEED_ATTEMPTS + " produced a differing route; "
        + "widen MAX_SEED_ATTEMPTS or lower TARGET_GAP_PERCENT."
      );
    }

    ComparisonReportWriter.write(REPORT_PATH, chosenScenario, chosenCosts, chosenOrderA, chosenOrderB, profile, chosenSeed);

    System.out.println("Seed escolhida: " + chosenSeed + " (gap = " + String.format("%.2f", bestGapPercent) + "%)");
    System.out.println("Rota Cenário A: " + chosenOrderA);
    System.out.println("Rota Cenário B: " + chosenOrderB);
    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }
}
