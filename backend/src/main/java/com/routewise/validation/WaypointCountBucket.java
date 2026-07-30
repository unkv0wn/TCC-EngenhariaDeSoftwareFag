package com.routewise.validation;

/** Aggregated metrics for all trials sharing the same waypoint count. */
public record WaypointCountBucket(
  int n,
  int trialCount,
  double pctRoutesDiffer,
  double gapPercentMean
) {}
