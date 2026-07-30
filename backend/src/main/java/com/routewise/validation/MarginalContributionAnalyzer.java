package com.routewise.validation;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.model.RouteMode;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Reusable check for "if I add this new variable, does it actually help or is it
 * noise/redundant?" — the general form of the Cenário A vs Cenário B comparison used
 * to validate fuel and tire wear in the first place, now generalised to any future
 * candidate indicator.
 *
 * <p>Usage: implement how the candidate indicator contributes cost per edge (a
 * {@code (Scenario, VehicleProfile) -> double[][]} function, in R$), pass it here
 * along with a set of scenarios, and read the verdict. See
 * {@link NewIndicatorAdditionExample} for a worked example.
 */
public final class MarginalContributionAnalyzer {

  private static final double MIN_PCT_ROUTES_DIFFER_TO_MATTER = 5.0;
  private static final double REDUNDANT_CORRELATION_THRESHOLD = 0.9;

  private MarginalContributionAnalyzer() {}

  public static MarginalContributionResult evaluate(
    List<Scenario> scenarios,
    VehicleProfile profile,
    RouteMode mode,
    BiFunction<Scenario, VehicleProfile, double[][]> candidateComponentReais
  ) {
    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();
    int total = scenarios.size();

    double[] costUnderBaseRoute = new double[total];
    double[] costUnderExtendedRoute = new double[total];
    double[] candidateTotalOnBaseRoute = new double[total];
    double[] baseTotalOnBaseRoute = new double[total];
    int differCount = 0;

    for (int i = 0; i < total; i++) {
      Scenario scenario = scenarios.get(i);
      EdgeCosts baseCosts = CostMatrixBuilder.build(scenario, profile);
      double[][] candidate = candidateComponentReais.apply(scenario, profile);
      double[][] extendedCostB = addMatrices(baseCosts.costB(), candidate, scenario.size());

      List<Integer> orderBase = optimizer.optimize(baseCosts.costB(), mode);
      List<Integer> orderExtended = optimizer.optimize(extendedCostB, mode);

      if (!orderBase.equals(orderExtended)) {
        differCount++;
      }

      costUnderBaseRoute[i] = TrialResultBuilder.sumAlongPath(extendedCostB, orderBase);
      costUnderExtendedRoute[i] = TrialResultBuilder.sumAlongPath(extendedCostB, orderExtended);
      candidateTotalOnBaseRoute[i] = TrialResultBuilder.sumAlongPath(candidate, orderBase);
      baseTotalOnBaseRoute[i] = TrialResultBuilder.sumAlongPath(baseCosts.costB(), orderBase);
    }

    double pctRoutesDiffer = 100.0 * differCount / total;

    double gapReaisSum = 0;
    double gapPercentSum = 0;
    int gapPercentCount = 0;
    for (int i = 0; i < total; i++) {
      double gap = costUnderBaseRoute[i] - costUnderExtendedRoute[i];
      gapReaisSum += gap;
      if (costUnderBaseRoute[i] > 0) {
        gapPercentSum += 100.0 * gap / costUnderBaseRoute[i];
        gapPercentCount++;
      }
    }
    double gapReaisMean = gapReaisSum / total;
    double gapPercentMean = gapPercentCount > 0 ? gapPercentSum / gapPercentCount : 0.0;

    double wilcoxonP = runWilcoxon(costUnderBaseRoute, costUnderExtendedRoute);
    double correlation = new SpearmansCorrelation().correlation(candidateTotalOnBaseRoute, baseTotalOnBaseRoute);
    String verdict = buildVerdict(pctRoutesDiffer, wilcoxonP, correlation);

    return new MarginalContributionResult(
      total, pctRoutesDiffer, gapReaisMean, gapPercentMean, wilcoxonP, correlation, verdict
    );
  }

  private static double[][] addMatrices(double[][] a, double[][] b, int n) {
    double[][] result = new double[n][n];
    for (int r = 0; r < n; r++) {
      for (int c = 0; c < n; c++) {
        result[r][c] = a[r][c] + b[r][c];
      }
    }
    return result;
  }

  /** Paired Wilcoxon signed-rank test, excluding zero-difference pairs (see StatisticalAnalyzer). */
  private static double runWilcoxon(double[] a, double[] b) {
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
      return Double.NaN;
    }
    double[] x = Arrays.copyOf(xFiltered, count);
    double[] y = Arrays.copyOf(yFiltered, count);
    return new WilcoxonSignedRankTest().wilcoxonSignedRankTest(x, y, false);
  }

  private static String buildVerdict(double pctRoutesDiffer, double wilcoxonP, double correlation) {
    boolean significant = !Double.isNaN(wilcoxonP) && wilcoxonP < 0.05;
    boolean meaningfulChange = pctRoutesDiffer >= MIN_PCT_ROUTES_DIFFER_TO_MATTER && significant;
    boolean redundant = Math.abs(correlation) >= REDUNDANT_CORRELATION_THRESHOLD;

    if (!meaningfulChange) {
      return "NEGLIGENCIÁVEL — muda poucas rotas e/ou o efeito não é estatisticamente significativo. "
        + "Adicionar essa variável provavelmente não agrega valor ao modelo atual.";
    }
    if (redundant) {
      return "REDUNDANTE — muda rotas de forma significativa, mas está altamente correlacionada "
        + "(|r| >= " + REDUNDANT_CORRELATION_THRESHOLD + ") com o custo já existente. Pode estar "
        + "medindo essencialmente a mesma coisa que os componentes atuais (ex: tudo proporcional à distância).";
    }
    return "AGREGA VALOR — muda rotas de forma estatisticamente significativa e não é redundante "
      + "com o modelo atual. Vale considerar incluir essa variável na função de custo.";
  }
}
