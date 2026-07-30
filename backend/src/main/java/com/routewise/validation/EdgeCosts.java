package com.routewise.validation;

/**
 * Per-edge cost matrices derived from a {@link Scenario}, ready to feed the
 * (unmodified) {@code AStarWaypointOptimizer}.
 *
 * @param costA        Cenário A — duration only, in seconds (identical to what
 *                      production optimizes on today)
 * @param costB         Cenário B — combined monetary cost, in R$ (time + fuel + tire wear)
 * @param durationSec   duration matrix, in seconds (component of both scenarios)
 * @param fuelLiters    fuel consumption matrix, in liters
 * @param tireWearReais tire wear cost matrix, in R$
 */
public record EdgeCosts(
  double[][] costA,
  double[][] costB,
  double[][] durationSec,
  double[][] fuelLiters,
  double[][] tireWearReais
) {}
