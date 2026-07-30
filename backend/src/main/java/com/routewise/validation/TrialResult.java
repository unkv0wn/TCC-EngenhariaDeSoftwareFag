package com.routewise.validation;

/**
 * Outcome of a single trial: both scenarios' chosen routes evaluated under the
 * Cenário B (combined) cost function, plus descriptive metrics for the
 * Cenário B route.
 *
 * @param n                  number of waypoints in this trial
 * @param routesDiffer       whether Cenário A and Cenário B chose a different order
 * @param costBUnderOrderA   Cenário B's cost function applied to Cenário A's route
 * @param costBUnderOrderB   Cenário B's cost function applied to its own route (the minimum)
 * @param gapReais           {@code costBUnderOrderA - costBUnderOrderB}; money "left on the
 *                           table" if empirical factors are ignored (always >= 0)
 * @param gapPercent         {@code gapReais} as a percentage of {@code costBUnderOrderA}
 * @param distanceKmOrderB   total distance of the Cenário B route
 * @param durationMinOrderB  total duration of the Cenário B route, in minutes
 * @param fuelLitersOrderB   total fuel consumption of the Cenário B route
 * @param tireWearReaisOrderB total tire wear cost of the Cenário B route
 * @param loadFactor         the trial's vehicle load factor (0-1)
 * @param urbanFraction      fraction of edges on the Cenário B route that were URBANA
 */
public record TrialResult(
  int n,
  boolean routesDiffer,
  double costBUnderOrderA,
  double costBUnderOrderB,
  double gapReais,
  double gapPercent,
  double distanceKmOrderB,
  double durationMinOrderB,
  double fuelLitersOrderB,
  double tireWearReaisOrderB,
  double loadFactor,
  double urbanFraction
) {}
