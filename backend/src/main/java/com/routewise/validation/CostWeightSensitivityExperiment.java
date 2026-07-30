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
 * Sensitivity analysis on the cost-function weights: {@code custoB = timeWeight·tempo
 * + fuelWeight·combustível + tireWearWeight·desgaste}. The current model implicitly
 * uses (1, 1, 1) since every component is already in R$. This experiment shows what
 * happens to the chosen routes when a business would rather prioritise one factor
 * over the others (e.g. minimise fuel even if it costs more time).
 *
 * <p>Same 500 scenarios and vehicle profile across every weight configuration — only
 * the weights change, so differences are attributable to the weighting choice alone.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.CostWeightSensitivityExperiment}
 */
public final class CostWeightSensitivityExperiment {

  private static final int SCENARIOS = 500;
  private static final long SEED = 42L;
  private static final RouteMode MODE = RouteMode.ROUND_TRIP;
  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "cost-weight-sensitivity.md");

  private record WeightConfig(String label, double timeWeight, double fuelWeight, double tireWearWeight) {}

  private static final List<WeightConfig> CONFIGS = List.of(
    new WeightConfig("Só tempo (produção atual)", 1.0, 0.0, 0.0),
    new WeightConfig("Equilibrado (1,1,1) — modelo atual", 1.0, 1.0, 1.0),
    new WeightConfig("Prioriza combustível", 0.3, 2.0, 0.3),
    new WeightConfig("Prioriza desgaste de pneu", 0.3, 0.3, 2.0),
    new WeightConfig("Só combustível", 0.0, 1.0, 0.0),
    new WeightConfig("Só desgaste de pneu", 0.0, 0.0, 1.0)
  );

  public static void main(String[] args) {
    VehicleProfile profile = VehicleProfile.medium();
    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();

    Random rng = new Random(SEED);
    ScenarioGenerator generator = new ScenarioGenerator(rng);
    List<Scenario> scenarios = new ArrayList<>(SCENARIOS);
    for (int t = 0; t < SCENARIOS; t++) {
      scenarios.add(generator.generate());
    }

    List<EdgeCosts> costsPerScenario = new ArrayList<>(SCENARIOS);
    for (Scenario scenario : scenarios) {
      costsPerScenario.add(CostMatrixBuilder.build(scenario, profile));
    }

    // Reference for "does the route change" comparisons: today's equal-weight model.
    List<List<Integer>> equalWeightOrders = new ArrayList<>(SCENARIOS);
    for (EdgeCosts costs : costsPerScenario) {
      equalWeightOrders.add(optimizer.optimize(costs.costB(), MODE));
    }

    List<WeightSensitivityResult> results = new ArrayList<>();
    for (WeightConfig config : CONFIGS) {
      results.add(evaluateConfig(config, scenarios, costsPerScenario, equalWeightOrders, profile, optimizer));
    }

    writeReport(results);

    for (WeightSensitivityResult r : results) {
      System.out.printf(Locale.US,
        "%s: %.1f km, %.1f min, %.2f L, R$ %.2f (pneu), muda %.1f%% das rotas vs modelo equilibrado%n",
        r.label(), r.meanDistanceKm(), r.meanDurationMin(), r.meanFuelLiters(), r.meanTireWearReais(),
        r.pctRoutesDifferFromEqualWeights());
    }
    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  private static WeightSensitivityResult evaluateConfig(
    WeightConfig config, List<Scenario> scenarios, List<EdgeCosts> costsPerScenario,
    List<List<Integer>> equalWeightOrders, VehicleProfile profile, AStarWaypointOptimizer optimizer
  ) {
    int total = scenarios.size();
    double sumDistance = 0, sumDuration = 0, sumFuel = 0, sumTireWear = 0, sumCostAtEqualWeights = 0;
    int differCount = 0;

    for (int i = 0; i < total; i++) {
      Scenario scenario = scenarios.get(i);
      EdgeCosts costs = costsPerScenario.get(i);
      double[][] weightedCost = buildWeightedCost(costs, profile, config);

      List<Integer> order = optimizer.optimize(weightedCost, MODE);

      sumDistance += TrialResultBuilder.sumAlongPath(scenario.distanceKm(), order);
      sumDuration += TrialResultBuilder.sumAlongPath(costs.durationSec(), order) / 60.0;
      sumFuel += TrialResultBuilder.sumAlongPath(costs.fuelLiters(), order);
      sumTireWear += TrialResultBuilder.sumAlongPath(costs.tireWearReais(), order);
      sumCostAtEqualWeights += TrialResultBuilder.sumAlongPath(costs.costB(), order);

      if (!order.equals(equalWeightOrders.get(i))) {
        differCount++;
      }
    }

    return new WeightSensitivityResult(
      config.label(), config.timeWeight(), config.fuelWeight(), config.tireWearWeight(),
      sumDistance / total, sumDuration / total, sumFuel / total, sumTireWear / total,
      sumCostAtEqualWeights / total, 100.0 * differCount / total
    );
  }

  /** Recombines the (weight-independent) per-edge components already in EdgeCosts with new weights. */
  private static double[][] buildWeightedCost(EdgeCosts costs, VehicleProfile profile, WeightConfig config) {
    int n = costs.costA().length;
    double[][] weighted = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          continue;
        }
        double timeCostReais = (costs.durationSec()[i][j] / 3600.0) * profile.driverCostPerHourReais();
        double fuelCostReais = costs.fuelLiters()[i][j] * profile.fuelPricePerLiter();
        double tireWearReais = costs.tireWearReais()[i][j];
        weighted[i][j] = config.timeWeight() * timeCostReais
          + config.fuelWeight() * fuelCostReais
          + config.tireWearWeight() * tireWearReais;
      }
    }
    return weighted;
  }

  private static void writeReport(List<WeightSensitivityResult> results) {
    StringBuilder sb = new StringBuilder();
    sb.append("# Análise de Sensibilidade dos Pesos da Função de Custo\n\n");
    sb.append("Mesmos 500 cenários sintéticos e mesmo veículo (Médio, 3 eixos) em todas as linhas — ")
      .append("só os pesos `(tempo, combustível, desgaste de pneu)` mudam na combinação ")
      .append("`custoB = α·tempo + β·combustível + γ·desgaste`. Mostra o trade-off real: ")
      .append("priorizar um fator tende a piorar os outros.\n\n");

    sb.append("| Configuração | Pesos (α,β,γ) | Distância média | Duração média | Combustível médio | Desgaste médio (R$) | Muda rota vs. equilibrado |\n");
    sb.append("|---|---|---|---|---|---|---|\n");
    for (WeightSensitivityResult r : results) {
      sb.append(String.format(Locale.US,
        "| %s | (%.1f, %.1f, %.1f) | %.2f km | %.1f min | %.2f L | R$ %.2f | %.1f%% |\n",
        r.label(), r.timeWeight(), r.fuelWeight(), r.tireWearWeight(),
        r.meanDistanceKm(), r.meanDurationMin(), r.meanFuelLiters(), r.meanTireWearReais(),
        r.pctRoutesDifferFromEqualWeights()));
    }

    sb.append("\n\"Muda rota vs. equilibrado\" compara a rota escolhida por essa configuração com a rota do ")
      .append("modelo atual (pesos 1,1,1) — quantifica o quanto mudar os pesos realmente altera a decisão.\n\n");

    sb.append("## Como interpretar\n\n");
    sb.append("Compare \"Duração média\" com \"Combustível médio\"/\"Desgaste médio\" entre as linhas: se ")
      .append("priorizar combustível reduz o combustível médio mas aumenta a duração média, isso confirma um ")
      .append("trade-off real (não é ruído) — a escolha de pesos deve refletir o objetivo real do negócio ")
      .append("(cumprir prazo, economizar combustível ou preservar a vida útil da frota).\n");

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, sb.toString());
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write weight sensitivity report to " + REPORT_PATH, ex);
    }
  }
}
