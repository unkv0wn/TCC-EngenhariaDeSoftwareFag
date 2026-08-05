package com.routewise.validation;

import java.util.List;

/** Aggregated statistics produced by {@link StatisticalAnalyzer} across all trials. */
public record Summary(
  int totalTrials,
  double pctRoutesDiffer,
  double pctInfeasible,
  double pctVolumeBound,
  double gapReaisMean,
  double gapReaisMedian,
  double gapReaisStd,
  double gapReaisMin,
  double gapReaisMax,
  double gapPercentMean,
  double gapPercentMedian,
  double gapPercentStd,
  double gapPercentMin,
  double gapPercentMax,
  double wilcoxonStatistic,
  double wilcoxonPValue,
  double corrWeightFraction,
  double corrVolumeFraction,
  double corrOccupancyFraction,
  double corrUrbanFraction,
  double corrWaypointCount,
  List<WaypointCountBucket> byWaypointCount,
  List<OccupancyLevelBucket> byOccupancyLevel
) {}
