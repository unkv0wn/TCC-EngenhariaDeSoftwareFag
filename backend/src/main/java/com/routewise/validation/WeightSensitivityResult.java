package com.routewise.validation;

/**
 * Aggregated outcome of running one weight configuration
 * ({@code custoB = timeWeight·tempo + fuelWeight·combustível + tireWearWeight·desgaste})
 * across all scenarios.
 *
 * @param meanCostAtEqualWeightsReais mean cost of this config's chosen routes, evaluated under
 *                                    the current default (1,1,1) model — a common yardstick to
 *                                    compare configs fairly, regardless of their own weights
 * @param pctRoutesDifferFromEqualWeights % of scenarios where this config picked a different
 *                                        route than the current default (1,1,1) model
 */
public record WeightSensitivityResult(
  String label,
  double timeWeight,
  double fuelWeight,
  double tireWearWeight,
  double meanDistanceKm,
  double meanDurationMin,
  double meanFuelLiters,
  double meanTireWearReais,
  double meanCostAtEqualWeightsReais,
  double pctRoutesDifferFromEqualWeights
) {}
