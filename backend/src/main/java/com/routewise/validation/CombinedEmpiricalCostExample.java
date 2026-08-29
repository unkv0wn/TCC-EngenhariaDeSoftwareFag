package com.routewise.validation;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.dto.WaypointDto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Worked example: stacks both empirical corrections this package has produced so far —
 * {@link EmpiricalFuelConsumptionCalculator} (rolling window of the last
 * {@value EmpiricalFuelConsumptionCalculator#WINDOW_SIZE} refuels) and
 * {@link EmpiricalTireLifeCalculator} (rolling window of the last
 * {@value EmpiricalTireLifeCalculator#WINDOW_SIZE} wear-only tire replacements) — on top of
 * each other, and compares the result against **the current state of the project**:
 * {@link ToledoRouteComparisonExample#PROFILE}, the fixed, documented-as-assumed
 * {@code VehicleProfile.medium()} every other validation report in this package uses as its
 * baseline. Reuses the exact synthetic logs
 * {@link FuelConsumptionFromRefuelingExample#REFUELING_LOG} and
 * {@link TireLifeFromReplacementLogExample#REPLACEMENT_LOG} already validated individually in
 * their own reports — this one only combines them.
 *
 * <p>Reuses {@link ToledoRouteComparisonExample}'s waypoint/scenario-building helpers,
 * {@link AStarWaypointOptimizer} and {@link CostMatrixBuilder} unmodified; touches no
 * production code.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.CombinedEmpiricalCostExample}
 */
public final class CombinedEmpiricalCostExample {

  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "combined-empirical-cost-comparison.md");

  public static void main(String[] args) {
    VehicleProfile currentProjectProfile = ToledoRouteComparisonExample.PROFILE; // estado atual: 15.0 L/100km, 60,000 km

    double empiricalConsumption = EmpiricalFuelConsumptionCalculator.consumptionLPer100Km(
      FuelConsumptionFromRefuelingExample.REFUELING_LOG, currentProjectProfile.baseFuelConsumptionLPer100Km()
    );
    double empiricalTireLifeKm = EmpiricalTireLifeCalculator.tireLifeKm(
      TireLifeFromReplacementLogExample.REPLACEMENT_LOG, currentProjectProfile.tireLifeKm()
    );

    VehicleProfile fullyEmpiricalProfile = currentProjectProfile
      .withLabel(currentProjectProfile.label() + " (combustível + pneu empíricos)")
      .withFuelConsumption(empiricalConsumption)
      .withTireLifeKm(empiricalTireLifeKm);

    List<WaypointDto> waypoints = ToledoRouteComparisonExample.WAYPOINTS.stream()
      .map(NamedWaypoint::toDto).collect(Collectors.toList());
    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();

    Scenario scenario = null;
    List<Integer> orderA = null;
    List<Integer> orderBCurrent = null;
    long chosenSeed = ToledoRouteComparisonExample.SEED_BASE;

    for (int attempt = 0; attempt < ToledoRouteComparisonExample.SEED_SEARCH_ATTEMPTS; attempt++) {
      long seed = ToledoRouteComparisonExample.SEED_BASE + attempt;
      RoadType[][] roadType = ToledoRouteComparisonExample.sampleRoadTypes(seed);
      Scenario candidate = new Scenario(
        waypoints, ToledoRouteComparisonExample.distanceMatrix(), roadType,
        ToledoRouteComparisonExample.CARGO_WEIGHT_KG, ToledoRouteComparisonExample.CARGO_VOLUME_M3
      );
      EdgeCosts candidateCosts = CostMatrixBuilder.build(candidate, currentProjectProfile);
      List<Integer> candidateOrderA = optimizer.optimize(candidateCosts.costA(), ToledoRouteComparisonExample.MODE);
      List<Integer> candidateOrderB = optimizer.optimize(candidateCosts.costB(), ToledoRouteComparisonExample.MODE);

      scenario = candidate;
      orderA = candidateOrderA;
      orderBCurrent = candidateOrderB;
      chosenSeed = seed;

      if (!candidateOrderA.equals(candidateOrderB)) {
        break;
      }
    }

    EdgeCosts costsCurrent = CostMatrixBuilder.build(scenario, currentProjectProfile);
    EdgeCosts costsEmpirical = CostMatrixBuilder.build(scenario, fullyEmpiricalProfile);
    List<Integer> orderBEmpirical = optimizer.optimize(costsEmpirical.costB(), ToledoRouteComparisonExample.MODE);

    String report = buildReport(
      empiricalConsumption, empiricalTireLifeKm, currentProjectProfile, fullyEmpiricalProfile,
      costsCurrent, costsEmpirical, orderA, orderBCurrent, orderBEmpirical, chosenSeed
    );

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, report);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write combined empirical cost report to " + REPORT_PATH, ex);
    }

    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  private static String buildReport(
    double empiricalConsumption, double empiricalTireLifeKm,
    VehicleProfile currentProjectProfile, VehicleProfile fullyEmpiricalProfile,
    EdgeCosts costsCurrent, EdgeCosts costsEmpirical,
    List<Integer> orderA, List<Integer> orderBCurrent, List<Integer> orderBEmpirical, long seed
  ) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Combustível + Pneu Empíricos, Combinados — vs. Estado Atual do Projeto\n\n");
    sb.append(String.format(Locale.US, "Gerado em: %s\n\n",
      java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

    appendContext(sb);
    appendProfileComparison(sb, currentProjectProfile, fullyEmpiricalProfile);
    appendSourceLogsRecap(sb);
    appendScenarioRecap(sb, seed);
    appendRouteComparison(sb, orderA, orderBCurrent, orderBEmpirical);
    appendAggregateComparison(sb, costsCurrent, costsEmpirical, orderA, orderBCurrent, orderBEmpirical);
    appendConclusion(sb, currentProjectProfile, fullyEmpiricalProfile, orderBCurrent, orderBEmpirical,
      costsCurrent, costsEmpirical, orderA);

    return sb.toString();
  }

  private static void appendContext(StringBuilder sb) {
    sb.append("## 1. Contexto\n\n");
    sb.append(
      "Os dois relatórios anteriores (`fuel-consumption-from-refueling.md` e " +
      "`tire-life-from-replacement-log.md`) validam cada correção empírica isoladamente. " +
      "Este relatório empilha as duas em cima do mesmo perfil e compara o resultado direto " +
      "contra **o estado atual do projeto** — `VehicleProfile.medium()` com as constantes " +
      "assumidas (`baseFuelConsumptionLPer100Km = 15.0`, `tireLifeKm = 60000.0`), que é o " +
      "perfil que `ToledoRouteComparisonExample` e os demais relatórios deste pacote usam " +
      "hoje como linha de base — nenhum deles usa dado calibrado ainda.\n\n"
    );
  }

  private static void appendProfileComparison(StringBuilder sb, VehicleProfile current, VehicleProfile empirical) {
    sb.append("## 2. Perfis comparados\n\n");
    sb.append("| Parâmetro | Estado atual do projeto (assumido) | Combinado (fuel + pneu empíricos) | Diferença |\n");
    sb.append("|---|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Consumo base | %.1f L/100km | %.2f L/100km | %+.1f%% |\n",
      current.baseFuelConsumptionLPer100Km(), empirical.baseFuelConsumptionLPer100Km(),
      100.0 * (empirical.baseFuelConsumptionLPer100Km() - current.baseFuelConsumptionLPer100Km()) / current.baseFuelConsumptionLPer100Km()
    ));
    sb.append(String.format(Locale.US, "| Vida útil do pneu | %.0f km | %.1f km | %+.1f%% |\n",
      current.tireLifeKm(), empirical.tireLifeKm(),
      100.0 * (empirical.tireLifeKm() - current.tireLifeKm()) / current.tireLifeKm()
    ));
    sb.append("\nDemais parâmetros (eixos, capacidade, preços, custo-hora) são idênticos nos dois perfis.\n\n");
  }

  private static void appendSourceLogsRecap(StringBuilder sb) {
    sb.append("## 3. Origem de cada valor\n\n");
    sb.append(String.format(Locale.US,
      "- **Consumo (%.2f L/100km):** janela móvel dos últimos %d abastecimentos de " +
      "`FuelConsumptionFromRefuelingExample.REFUELING_LOG` (%d registros no log). Detalhe " +
      "completo, tanque por tanque, em `docs/validation-reports/fuel-consumption-from-refueling.md`.\n" +
      "- **Vida útil do pneu (%.1f km):** janela móvel dos últimos %d eventos de troca por " +
      "desgaste normal (eixo de tração) de " +
      "`TireLifeFromReplacementLogExample.REPLACEMENT_LOG` (%d registros no log, incluindo " +
      "um evento de dano/acidente excluído do cálculo). Detalhe completo em " +
      "`docs/validation-reports/tire-life-from-replacement-log.md`.\n\n",
      EmpiricalFuelConsumptionCalculator.consumptionLPer100Km(
        FuelConsumptionFromRefuelingExample.REFUELING_LOG, ToledoRouteComparisonExample.PROFILE.baseFuelConsumptionLPer100Km()
      ),
      EmpiricalFuelConsumptionCalculator.WINDOW_SIZE, FuelConsumptionFromRefuelingExample.REFUELING_LOG.size(),
      EmpiricalTireLifeCalculator.tireLifeKm(
        TireLifeFromReplacementLogExample.REPLACEMENT_LOG, ToledoRouteComparisonExample.PROFILE.tireLifeKm()
      ),
      EmpiricalTireLifeCalculator.WINDOW_SIZE, TireLifeFromReplacementLogExample.REPLACEMENT_LOG.size()
    ));
  }

  private static void appendScenarioRecap(StringBuilder sb, long seed) {
    sb.append("## 4. Cenário reaproveitado\n\n");
    sb.append(String.format(Locale.US,
      "Mesmo cenário fixo de `ToledoRouteComparisonExample`: sete pontos reais em Toledo-PR " +
      "(Prefeitura como depósito, ROUND_TRIP), carga de %.0f kg / %.1f m³, tipo de via por " +
      "sentido sorteado com seed **%d** (a mesma seed dos outros dois relatórios, já que a " +
      "busca é determinística e não depende de combustível nem pneu).\n\n",
      ToledoRouteComparisonExample.CARGO_WEIGHT_KG, ToledoRouteComparisonExample.CARGO_VOLUME_M3, seed
    ));
  }

  private static void appendRouteComparison(
    StringBuilder sb, List<Integer> orderA, List<Integer> orderBCurrent, List<Integer> orderBEmpirical
  ) {
    boolean currentDiffersFromA = !orderA.equals(orderBCurrent);
    boolean empiricalDiffersFromA = !orderA.equals(orderBEmpirical);
    boolean empiricalDiffersFromCurrent = !orderBCurrent.equals(orderBEmpirical);

    sb.append("## 5. A rota escolhida muda?\n\n");
    sb.append("- **Cenário A (só duração):** ").append(formatOrder(orderA)).append('\n');
    sb.append("- **Cenário B, estado atual do projeto (assumido):** ").append(formatOrder(orderBCurrent)).append('\n');
    sb.append("- **Cenário B, combinado (fuel + pneu empíricos):** ").append(formatOrder(orderBEmpirical)).append('\n');
    sb.append(String.format(Locale.US,
      "- Cenário B (atual) difere de A? **%s** · Cenário B (combinado) difere de A? **%s** · " +
      "Cenário B (combinado) difere de Cenário B (atual)? **%s**\n\n",
      currentDiffersFromA ? "Sim" : "Não", empiricalDiffersFromA ? "Sim" : "Não",
      empiricalDiffersFromCurrent ? "Sim" : "Não"
    ));
  }

  private static void appendAggregateComparison(
    StringBuilder sb, EdgeCosts costsCurrent, EdgeCosts costsEmpirical,
    List<Integer> orderA, List<Integer> orderBCurrent, List<Integer> orderBEmpirical
  ) {
    double fuelUnderCurrentFormula = TrialResultBuilder.sumAlongPath(costsCurrent.fuelLiters(), orderA);
    double fuelUnderEmpiricalFormula = TrialResultBuilder.sumAlongPath(costsEmpirical.fuelLiters(), orderA);
    double wearUnderCurrentFormula = TrialResultBuilder.sumAlongPath(costsCurrent.tireWearReais(), orderA);
    double wearUnderEmpiricalFormula = TrialResultBuilder.sumAlongPath(costsEmpirical.tireWearReais(), orderA);

    double costCurrentUnderOrderA = TrialResultBuilder.sumAlongPath(costsCurrent.costB(), orderA);
    double costCurrentUnderOrderB = TrialResultBuilder.sumAlongPath(costsCurrent.costB(), orderBCurrent);
    double gapCurrentPercent = costCurrentUnderOrderA > 0
      ? 100.0 * (costCurrentUnderOrderA - costCurrentUnderOrderB) / costCurrentUnderOrderA : 0.0;

    double costEmpiricalUnderOrderA = TrialResultBuilder.sumAlongPath(costsEmpirical.costB(), orderA);
    double costEmpiricalUnderOrderB = TrialResultBuilder.sumAlongPath(costsEmpirical.costB(), orderBEmpirical);
    double gapEmpiricalPercent = costEmpiricalUnderOrderA > 0
      ? 100.0 * (costEmpiricalUnderOrderA - costEmpiricalUnderOrderB) / costEmpiricalUnderOrderA : 0.0;

    sb.append("## 6. Métricas agregadas — estado atual vs combinado\n\n");
    sb.append("Todas avaliadas sobre a rota do Cenário A (mesma rota, isolando o efeito da troca de perfil):\n\n");
    sb.append("| Métrica | Estado atual do projeto | Combinado (fuel + pneu empíricos) |\n");
    sb.append("|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Combustível | %.2f L | %.2f L |\n", fuelUnderCurrentFormula, fuelUnderEmpiricalFormula));
    sb.append(String.format(Locale.US, "| Desgaste de pneu | R$ %.2f | R$ %.2f |\n", wearUnderCurrentFormula, wearUnderEmpiricalFormula));
    sb.append(String.format(Locale.US, "| Custo total (fórmula B) | R$ %.2f | R$ %.2f |\n", costCurrentUnderOrderA, costEmpiricalUnderOrderA));
    sb.append('\n');
    sb.append("Gap interno de cada perfil (rota do Cenário A vs. sua própria rota ótima do Cenário B):\n\n");
    sb.append("| Perfil | Gap (R$) | Gap (%) |\n|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Estado atual do projeto | R$ %.2f | %.2f%% |\n",
      costCurrentUnderOrderA - costCurrentUnderOrderB, gapCurrentPercent));
    sb.append(String.format(Locale.US, "| Combinado (fuel + pneu empíricos) | R$ %.2f | %.2f%% |\n\n",
      costEmpiricalUnderOrderA - costEmpiricalUnderOrderB, gapEmpiricalPercent));
  }

  private static void appendConclusion(
    StringBuilder sb, VehicleProfile current, VehicleProfile empirical,
    List<Integer> orderBCurrent, List<Integer> orderBEmpirical,
    EdgeCosts costsCurrent, EdgeCosts costsEmpirical,
    List<Integer> orderA
  ) {
    boolean routeChanged = !orderBCurrent.equals(orderBEmpirical);
    double costCurrent = TrialResultBuilder.sumAlongPath(costsCurrent.costB(), orderA);
    double costEmpirical = TrialResultBuilder.sumAlongPath(costsEmpirical.costB(), orderA);

    sb.append("## 7. Conclusão\n\n");
    sb.append(String.format(Locale.US,
      "1. **Os dois efeitos vão na mesma direção neste log sintético:** consumo empírico " +
      "%.1f%% maior e vida útil de pneu empírica %.1f%% menor do que o estado atual do " +
      "projeto — ambos aumentam o custo por trecho, então o efeito combinado é maior do " +
      "que qualquer um isolado (%.2f%% de aumento no custo total da rota do Cenário A, vs. " +
      "os dois relatórios individuais). Isso não é garantido em geral — com outro log, " +
      "consumo e vida útil de pneu poderiam divergir em direções opostas e se cancelar " +
      "parcialmente.\n" +
      "2. **A rota escolhida pelo Cenário B %s ao trocar o estado atual do projeto pelo " +
      "perfil combinado neste cenário** — %s\n" +
      "3. **O que isso significa pra produção:** nenhuma dessas correções está plugada em " +
      "`RouteController`/`RouteOptimizerServiceImpl` — os três relatórios deste pacote " +
      "(combustível, pneu, combinado) validam a lógica com logs sintéticos. O próximo passo " +
      "real seria plugar logs de abastecimento e troca de pneu de fato (ou pelo menos um " +
      "piloto com poucos veículos reais) antes de considerar levar isso pra produção.\n",
      100.0 * (empirical.baseFuelConsumptionLPer100Km() - current.baseFuelConsumptionLPer100Km()) / current.baseFuelConsumptionLPer100Km(),
      100.0 * (current.tireLifeKm() - empirical.tireLifeKm()) / current.tireLifeKm(),
      100.0 * (costEmpirical - costCurrent) / costCurrent,
      routeChanged ? "muda" : "não muda",
      routeChanged
        ? "isso significa que, com os dois dados empíricos combinados, o algoritmo teria escolhido uma ordem de visitação diferente da que o estado atual do projeto produziria."
        : "isso significa que, ao menos para esta geometria e carga específicas, os valores exatos de consumo e vida útil do pneu não foram decisivos juntos — mudou o custo total, mas não a ordem ótima."
    ));
  }

  private static String waypointName(int index) {
    return ToledoRouteComparisonExample.WAYPOINTS.get(index).name();
  }

  private static String formatOrder(List<Integer> order) {
    return order.stream().map(CombinedEmpiricalCostExample::waypointName).collect(Collectors.joining(" → "));
  }
}
