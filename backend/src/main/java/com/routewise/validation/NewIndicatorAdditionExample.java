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
 * Worked example of {@link MarginalContributionAnalyzer}: shows how to check whether
 * a brand-new candidate cost indicator actually helps the model or is negligible/
 * redundant, before wiring it into {@link CostMatrixBuilder} for real.
 *
 * <p>The example indicator here is a synthetic "risk cost" per road type (theft/
 * incident risk), which — unlike fuel and tire wear — is a flat premium per edge
 * instead of scaling with distance. To validate a real new indicator, replace
 * {@link #riskCostReais} with its formula and rerun.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.NewIndicatorAdditionExample}
 */
public final class NewIndicatorAdditionExample {

  private static final int SCENARIOS = 500;
  private static final long SEED = 42L;
  private static final RouteMode MODE = RouteMode.ROUND_TRIP;
  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "marginal-contribution-example.md");

  public static void main(String[] args) {
    VehicleProfile profile = VehicleProfile.medium();

    Random rng = new Random(SEED);
    ScenarioGenerator generator = new ScenarioGenerator(rng);
    List<Scenario> scenarios = new ArrayList<>(SCENARIOS);
    for (int t = 0; t < SCENARIOS; t++) {
      scenarios.add(generator.generate());
    }

    MarginalContributionResult result = MarginalContributionAnalyzer.evaluate(
      scenarios, profile, MODE, NewIndicatorAdditionExample::riskCostReais
    );

    writeReport(result);

    System.out.printf(Locale.US,
      "Nova variável (custo de risco): %.1f%% rotas diferentes, gap médio %.2f%%, p=%s, correlação com custo atual=%.3f%n",
      result.pctRoutesDiffer(), result.gapPercentMean(),
      Double.isNaN(result.wilcoxonPValue()) ? "N/A" : String.format(Locale.US, "%.4f", result.wilcoxonPValue()),
      result.correlationWithBaseCost()
    );
    System.out.println("Veredito: " + result.verdict());
    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  /**
   * Example candidate indicator: a flat risk premium per edge based on road type,
   * NOT scaled by distance (unlike fuel/tire wear) — meant to demonstrate a
   * genuinely independent signal. Swap this out for a real new indicator's formula.
   */
  private static double[][] riskCostReais(Scenario scenario, VehicleProfile profile) {
    int n = scenario.size();
    double[][] risk = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          continue;
        }
        risk[i][j] = switch (scenario.roadType()[i][j]) {
          case RODOVIA -> 2.0;
          case ARTERIAL -> 5.0;
          case URBANA -> 15.0;
        };
      }
    }
    return risk;
  }

  private static void writeReport(MarginalContributionResult r) {
    String content = String.format(Locale.US, """
      # Validação de Contribuição Marginal — Nova Variável (exemplo: custo de risco)

      Testa se adicionar uma nova variável ao modelo de custo atual (Cenário B: tempo +
      combustível + desgaste de pneu) muda a rota escolhida de forma real, ou se é
      negligenciável/redundante com o que já existe.

      Exemplo usado aqui: um "custo de risco" fixo por trecho (não escala com distância,
      ao contrário de combustível/pneu), maior em vias urbanas — simula algo como risco
      de furto/sinistro. Para validar uma variável nova de verdade, troque a fórmula em
      `NewIndicatorAdditionExample.riskCostReais(...)` por ela e rode de novo.

      | Métrica | Valor |
      |---|---|
      | Cenários testados | %d |
      | Rotas que mudaram ao adicionar a variável | %.1f%% |
      | Gap médio de custo | %.2f%% |
      | Wilcoxon p-valor | %s |
      | Correlação (Spearman) com o custo já existente | %.3f |

      Correlação alta (>= 0.9) sugere que a variável nova está medindo, na prática,
      algo já capturado pelas variáveis atuais (redundância). Correlação baixa com
      mudança de rota significativa sugere um sinal genuinamente novo.

      ## Veredito

      %s
      """,
      r.totalScenarios(), r.pctRoutesDiffer(), r.gapPercentMean(),
      Double.isNaN(r.wilcoxonPValue()) ? "N/A" : String.format(Locale.US, "%.4f", r.wilcoxonPValue()),
      r.correlationWithBaseCost(),
      r.verdict()
    );

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, content);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write marginal contribution report to " + REPORT_PATH, ex);
    }
  }
}
