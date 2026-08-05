package com.routewise.validation;

/**
 * Aggregated metrics for all trials whose cargo occupancy (max of weight and volume
 * fraction — see {@link CargoOccupancy}) fell in the same range.
 */
public record OccupancyLevelBucket(
  String label,
  int trialCount,
  double pctRoutesDiffer,
  double gapPercentMean
) {}
