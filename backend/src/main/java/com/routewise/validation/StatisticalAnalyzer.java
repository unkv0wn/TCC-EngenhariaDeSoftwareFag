package com.routewise.validation;

import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Aggregates {@link TrialResult}s into a {@link Summary}: descriptive statistics on
 * the cost gap, a Wilcoxon signed-rank test on the paired route costs, and Spearman
 * correlations between the gap size and candidate explanatory variables.
 */
public final class StatisticalAnalyzer {

  private StatisticalAnalyzer() {}

  public static Summary analyze(List<TrialResult> results) {
    int total = results.size();
    long differCount = results.stream().filter(TrialResult::routesDiffer).count();
    double pctRoutesDiffer = 100.0 * differCount / total;

    DescriptiveStatistics gapReais = new DescriptiveStatistics();
    DescriptiveStatistics gapPercent = new DescriptiveStatistics();
    for (TrialResult r : results) {
      gapReais.addValue(r.gapReais());
      gapPercent.addValue(r.gapPercent());
    }

    double[] costBUnderOrderA = new double[total];
    double[] costBUnderOrderB = new double[total];
    for (int i = 0; i < total; i++) {
      costBUnderOrderA[i] = results.get(i).costBUnderOrderA();
      costBUnderOrderB[i] = results.get(i).costBUnderOrderB();
    }

    double[] wilcoxon = runWilcoxon(costBUnderOrderA, costBUnderOrderB);

    double[] gapPercentArr = results.stream().mapToDouble(TrialResult::gapPercent).toArray();
    double[] loadFactorArr = results.stream().mapToDouble(TrialResult::loadFactor).toArray();
    double[] urbanFractionArr = results.stream().mapToDouble(TrialResult::urbanFraction).toArray();
    double[] nArr = results.stream().mapToDouble(TrialResult::n).toArray();

    SpearmansCorrelation correlation = new SpearmansCorrelation();
    List<WaypointCountBucket> byWaypointCount = buildWaypointCountBreakdown(results);
    List<LoadLevelBucket> byLoadLevel = buildLoadLevelBreakdown(results);

    return new Summary(
      total,
      pctRoutesDiffer,
      gapReais.getMean(),
      gapReais.getPercentile(50),
      gapReais.getStandardDeviation(),
      gapReais.getMin(),
      gapReais.getMax(),
      gapPercent.getMean(),
      gapPercent.getPercentile(50),
      gapPercent.getStandardDeviation(),
      gapPercent.getMin(),
      gapPercent.getMax(),
      wilcoxon[0],
      wilcoxon[1],
      correlation.correlation(gapPercentArr, loadFactorArr),
      correlation.correlation(gapPercentArr, urbanFractionArr),
      correlation.correlation(gapPercentArr, nArr),
      byWaypointCount,
      byLoadLevel
    );
  }

  /** Groups trials by waypoint count so the report can show how the gap trends with n. */
  private static List<WaypointCountBucket> buildWaypointCountBreakdown(List<TrialResult> results) {
    Map<Integer, List<TrialResult>> byN = results.stream()
      .collect(Collectors.groupingBy(TrialResult::n, TreeMap::new, Collectors.toList()));

    List<WaypointCountBucket> buckets = new ArrayList<>();
    for (Map.Entry<Integer, List<TrialResult>> entry : byN.entrySet()) {
      List<TrialResult> group = entry.getValue();
      long differCount = group.stream().filter(TrialResult::routesDiffer).count();
      double pctDiffer = 100.0 * differCount / group.size();
      double gapMean = group.stream().mapToDouble(TrialResult::gapPercent).average().orElse(0.0);
      buckets.add(new WaypointCountBucket(entry.getKey(), group.size(), pctDiffer, gapMean));
    }
    buckets.sort(Comparator.comparingInt(WaypointCountBucket::n));
    return buckets;
  }

  private static final double[] LOAD_LEVEL_UPPER_BOUNDS = {0.25, 0.50, 0.75, 1.01};
  private static final String[] LOAD_LEVEL_LABELS = {"0–25%", "25–50%", "50–75%", "75–100%"};

  /** Groups trials by vehicle load (% of capacity) so the report can show how the gap trends with cargo weight. */
  private static List<LoadLevelBucket> buildLoadLevelBreakdown(List<TrialResult> results) {
    List<LoadLevelBucket> buckets = new ArrayList<>();
    for (int b = 0; b < LOAD_LEVEL_LABELS.length; b++) {
      double lower = b == 0 ? -0.01 : LOAD_LEVEL_UPPER_BOUNDS[b - 1];
      double upper = LOAD_LEVEL_UPPER_BOUNDS[b];

      List<TrialResult> group = results.stream()
        .filter(r -> r.loadFactor() > lower && r.loadFactor() <= upper)
        .collect(Collectors.toList());

      if (group.isEmpty()) {
        continue;
      }

      long differCount = group.stream().filter(TrialResult::routesDiffer).count();
      double pctDiffer = 100.0 * differCount / group.size();
      double gapMean = group.stream().mapToDouble(TrialResult::gapPercent).average().orElse(0.0);
      buckets.add(new LoadLevelBucket(LOAD_LEVEL_LABELS[b], group.size(), pctDiffer, gapMean));
    }
    return buckets;
  }

  /**
   * Runs the Wilcoxon signed-rank test on the paired costs, excluding zero-difference
   * pairs (routes that happened to be identical between scenarios) per standard
   * procedure — a tie at zero carries no information about which scenario is cheaper.
   *
   * @return {@code [statistic, pValue]}; {@code [NaN, NaN]} if fewer than 2 non-zero
   *         pairs remain (test is not meaningful)
   */
  private static double[] runWilcoxon(double[] a, double[] b) {
    int n = a.length;
    double[] xFiltered = new double[n];
    double[] yFiltered = new double[n];
    int count = 0;
    for (int i = 0; i < n; i++) {
      if (a[i] != b[i]) {
        xFiltered[count] = a[i];
        yFiltered[count] = b[i];
        count++;
      }
    }

    if (count < 2) {
      return new double[]{Double.NaN, Double.NaN};
    }

    double[] x = java.util.Arrays.copyOf(xFiltered, count);
    double[] y = java.util.Arrays.copyOf(yFiltered, count);

    WilcoxonSignedRankTest test = new WilcoxonSignedRankTest();
    double statistic = test.wilcoxonSignedRank(x, y);
    double pValue = test.wilcoxonSignedRankTest(x, y, false);
    return new double[]{statistic, pValue};
  }
}
