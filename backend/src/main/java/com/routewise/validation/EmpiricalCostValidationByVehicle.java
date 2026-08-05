package com.routewise.validation;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.model.RouteMode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Runs the same empirical cost validation experiment
 * ({@link EmpiricalCostValidationExperiment}) once per truck class (leve/médio/pesado),
 * on the identical set of synthetic road scenarios, so the only thing that varies
 * between runs is the vehicle profile — isolating whether the effect of empirical
 * costs on route choice depends on which truck is used.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.EmpiricalCostValidationByVehicle}
 */
public final class EmpiricalCostValidationByVehicle {

  private static final int TRIALS = 500;
  private static final long SEED = 42L;
  private static final RouteMode MODE = RouteMode.ROUND_TRIP;
  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "empirical-cost-validation-por-veiculo.md");

  public static void main(String[] args) {
    List<VehicleProfile> profiles = List.of(
      VehicleProfile.light(), VehicleProfile.medium(), VehicleProfile.heavy()
    );

    // Same road scenarios for every profile — isolates the vehicle's effect instead
    // of conflating it with different random road draws per profile.
    Random rng = new Random(SEED);
    ScenarioGenerator generator = new ScenarioGenerator(rng);
    List<Scenario> scenarios = new ArrayList<>(TRIALS);
    for (int t = 0; t < TRIALS; t++) {
      scenarios.add(generator.generate());
    }

    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();

    List<Summary> summaries = new ArrayList<>(profiles.size());
    for (VehicleProfile profile : profiles) {
      List<TrialResult> results = new ArrayList<>(TRIALS);
      for (Scenario scenario : scenarios) {
        EdgeCosts costs = CostMatrixBuilder.build(scenario, profile);
        List<Integer> orderA = optimizer.optimize(costs.costA(), MODE);
        List<Integer> orderB = optimizer.optimize(costs.costB(), MODE);
        results.add(TrialResultBuilder.build(scenario, profile, costs, orderA, orderB));
      }
      summaries.add(StatisticalAnalyzer.analyze(results));
    }

    writeReport(profiles, summaries);

    for (int i = 0; i < profiles.size(); i++) {
      Summary s = summaries.get(i);
      System.out.printf(Locale.US, "%s: %.1f%% rotas diferentes, gap médio R$ %.2f (%.2f%%)%n",
        profiles.get(i).label(), s.pctRoutesDiffer(), s.gapReaisMean(), s.gapPercentMean());
    }
    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  private static void writeReport(List<VehicleProfile> profiles, List<Summary> summaries) {
    StringBuilder sb = new StringBuilder();
    sb.append("# Validação Empírica do Custo de Rota — Comparação entre Perfis de Veículo\n\n");
    sb.append("Os mesmos ").append(TRIALS).append(" cenários sintéticos de rota foram avaliados ")
      .append("para cada perfil de veículo abaixo — só o veículo muda entre as colunas.\n\n");

    sb.append("## Comparação entre perfis\n\n");
    sb.append("| Perfil | Rotas diferentes | Gap médio (R$) | Gap médio (%) | Wilcoxon p-valor |\n");
    sb.append("|---|---|---|---|---|\n");
    for (int i = 0; i < profiles.size(); i++) {
      Summary s = summaries.get(i);
      sb.append(String.format(Locale.US, "| %s | %.1f%% | R$ %.2f | %.2f%% | %s |\n",
        profiles.get(i).label(), s.pctRoutesDiffer(), s.gapReaisMean(), s.gapPercentMean(),
        Double.isNaN(s.wilcoxonPValue()) ? "N/A" : String.format(Locale.US, "%.4f", s.wilcoxonPValue())
      ));
    }
    sb.append("\n---\n\n");

    for (int i = 0; i < profiles.size(); i++) {
      sb.append(ReportWriter.render(summaries.get(i), profiles.get(i)));
      sb.append("\n---\n\n");
    }

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, sb.toString());
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write vehicle comparison report to " + REPORT_PATH, ex);
    }
  }
}
