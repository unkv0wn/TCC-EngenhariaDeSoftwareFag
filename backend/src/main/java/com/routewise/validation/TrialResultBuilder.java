package com.routewise.validation;

import java.util.List;

/**
 * Builds a {@link TrialResult} from a scenario's two chosen routes, and exposes the
 * path-summing helpers shared by every script in this package.
 */
public final class TrialResultBuilder {

  private TrialResultBuilder() {}

  public static TrialResult build(
    Scenario scenario, EdgeCosts costs, List<Integer> orderA, List<Integer> orderB
  ) {
    boolean routesDiffer = !orderA.equals(orderB);

    double costBUnderOrderA = sumAlongPath(costs.costB(), orderA);
    double costBUnderOrderB = sumAlongPath(costs.costB(), orderB);
    double gapReais = costBUnderOrderA - costBUnderOrderB;
    double gapPercent = costBUnderOrderA > 0 ? 100.0 * gapReais / costBUnderOrderA : 0.0;

    double distanceKmOrderB = sumAlongPath(scenario.distanceKm(), orderB);
    double durationMinOrderB = sumAlongPath(costs.durationSec(), orderB) / 60.0;
    double fuelLitersOrderB = sumAlongPath(costs.fuelLiters(), orderB);
    double tireWearReaisOrderB = sumAlongPath(costs.tireWearReais(), orderB);
    double urbanFraction = urbanFraction(scenario, orderB);

    return new TrialResult(
      scenario.size(), routesDiffer, costBUnderOrderA, costBUnderOrderB,
      gapReais, gapPercent, distanceKmOrderB, durationMinOrderB,
      fuelLitersOrderB, tireWearReaisOrderB, scenario.loadFactor(), urbanFraction
    );
  }

  public static double sumAlongPath(double[][] matrix, List<Integer> order) {
    double total = 0;
    for (int k = 0; k < order.size() - 1; k++) {
      total += matrix[order.get(k)][order.get(k + 1)];
    }
    return total;
  }

  private static double urbanFraction(Scenario scenario, List<Integer> order) {
    int edges = order.size() - 1;
    if (edges <= 0) {
      return 0.0;
    }
    int urbanCount = 0;
    for (int k = 0; k < edges; k++) {
      if (scenario.roadType()[order.get(k)][order.get(k + 1)] == RoadType.URBANA) {
        urbanCount++;
      }
    }
    return (double) urbanCount / edges;
  }
}
