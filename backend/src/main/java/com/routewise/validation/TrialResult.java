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
 * @param cargoWeightKg      the trial's raw cargo weight
 * @param cargoVolumeM3      the trial's raw cargo volume
 * @param weightFraction     cargoWeightKg / vehicle capacityKg, uncapped (see {@link CargoOccupancy})
 * @param volumeFraction     cargoVolumeM3 / vehicle capacityM3, uncapped (see {@link CargoOccupancy})
 * @param occupancyFraction  max(weightFraction, volumeFraction), uncapped — the fraction actually
 *                           driving the cost formulas (after clamping to 1.0) and used for bucketing
 * @param volumeBound        true if volume, not weight, was this trial's binding constraint
 * @param feasible           true if the cargo fit within both the weight and volume capacity
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
  double cargoWeightKg,
  double cargoVolumeM3,
  double weightFraction,
  double volumeFraction,
  double occupancyFraction,
  boolean volumeBound,
  boolean feasible,
  double urbanFraction
) {}
