package com.routewise.validation;

import java.util.List;

/** Aggregated statistics produced by {@link StatisticalAnalyzer} across all trials. */
public record Summary(
  int totalTrials,
  double pctRoutesDiffer,
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
  double corrLoadFactor,
  double corrUrbanFraction,
  double corrWaypointCount,
  List<WaypointCountBucket> byWaypointCount,
  List<LoadLevelBucket> byLoadLevel
) {}
