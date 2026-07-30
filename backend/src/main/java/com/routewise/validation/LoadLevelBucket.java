package com.routewise.validation;

/** Aggregated metrics for all trials whose vehicle load fell in the same range. */
public record LoadLevelBucket(
  String label,
  int trialCount,
  double pctRoutesDiffer,
  double gapPercentMean
) {}
