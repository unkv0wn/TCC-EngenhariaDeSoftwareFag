package com.routewise.validation;

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
 * Second worked example of {@link MarginalContributionAnalyzer}, this time with a
 * genuinely empirical (not synthetic-placeholder) candidate indicator: **toll cost**.
 *
 * <p>Brazilian toll roads (pedágio) charge per axle, only on RODOVIA edges, roughly
 * proportional to distance travelled on that road — a real operational cost that a
 * time-only route optimizer ignores today, and one that can genuinely conflict with
 * the time-optimal choice (the fastest highway route isn't always the cheapest once
 * tolls are counted).
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.TollCostAdditionExample}
 */
public final class TollCostAdditionExample {

  private static final int SCENARIOS = 500;
  private static final long SEED = 42L;
  private static final RouteMode MODE = RouteMode.ROUND_TRIP;

  /** R$ per km, per axle, on toll roads — a rough, documented assumption (ANTT-style axle-based pricing). */
  private static final double TOLL_RATE_PER_KM_PER_AXLE = 0.05;

  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "marginal-contribution-toll-example.md");

  public static void main(String[] args) {
    VehicleProfile profile = VehicleProfile.medium();

    Random rng = new Random(SEED);
    ScenarioGenerator generator = new ScenarioGenerator(rng);
    List<Scenario> scenarios = new ArrayList<>(SCENARIOS);
    for (int t = 0; t < SCENARIOS; t++) {
      scenarios.add(generator.generate());
    }

    MarginalContributionResult result = MarginalContributionAnalyzer.evaluate(
      scenarios, profile, MODE, TollCostAdditionExample::tollCostReais
    );

    writeReport(result, profile);

    System.out.printf(Locale.US,
      "Nova variável (pedágio): %.1f%% rotas diferentes, gap médio %.2f%%, p=%s, correlação com custo atual=%.3f%n",
      result.pctRoutesDiffer(), result.gapPercentMean(),
      Double.isNaN(result.wilcoxonPValue()) ? "N/A" : String.format(Locale.US, "%.4f", result.wilcoxonPValue()),
      result.correlationWithBaseCost()
    );
    System.out.println("Veredito: " + result.verdict());
    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  /** Toll only applies on RODOVIA edges; scales with distance and axle count, like real Brazilian toll pricing. */
  private static double[][] tollCostReais(Scenario scenario, VehicleProfile profile) {
    int n = scenario.size();
    double[][] toll = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j || scenario.roadType()[i][j] != RoadType.RODOVIA) {
          continue;
        }
        toll[i][j] = scenario.distanceKm()[i][j] * TOLL_RATE_PER_KM_PER_AXLE * profile.axleCount();
      }
    }
    return toll;
  }

  private static void writeReport(MarginalContributionResult r, VehicleProfile profile) {
    String content = String.format(Locale.US, """
      # Validação de Contribuição Marginal — Nova Variável (pedágio)

      Candidato real (não é placeholder sintético como o exemplo de risco): **custo de
      pedágio**. Só se aplica em trechos RODOVIA, proporcional à distância e ao número
      de eixos (R$ %.2f/km/eixo — aproximação da lógica de cobrança por eixo dos
      pedágios brasileiros, ANTT). Perfil de veículo: %s (%d eixos).

      Motivação: o otimizador atual (só tempo) tende a preferir rodovia por ser mais
      rápida; se o pedágio for caro o suficiente, pode compensar financeiramente uma
      rota um pouco mais lenta por vias sem pedágio.

      | Métrica | Valor |
      |---|---|
      | Cenários testados | %d |
      | Rotas que mudaram ao adicionar o pedágio | %.1f%% |
      | Gap médio de custo | %.2f%% |
      | Wilcoxon p-valor | %s |
      | Correlação (Spearman) com o custo já existente | %.3f |

      ## Veredito

      %s
      """,
      TOLL_RATE_PER_KM_PER_AXLE, profile.label(), profile.axleCount(),
      r.totalScenarios(), r.pctRoutesDiffer(), r.gapPercentMean(),
      Double.isNaN(r.wilcoxonPValue()) ? "N/A" : String.format(Locale.US, "%.4f", r.wilcoxonPValue()),
      r.correlationWithBaseCost(),
      r.verdict()
    );

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, content);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write toll marginal contribution report to " + REPORT_PATH, ex);
    }
  }
}
