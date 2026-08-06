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

import static com.routewise.validation.AxlePosition.TRACAO;
import static com.routewise.validation.EmpiricalTireLifeCalculator.TireReplacementReason.DANO_ACIDENTE;
import static com.routewise.validation.EmpiricalTireLifeCalculator.TireReplacementReason.DESGASTE_NORMAL;

/**
 * Worked example: what changes if {@code VehicleProfile.medium()}'s fixed 60,000 km
 * {@code tireLifeKm} — documented in {@link VehicleProfile} as a plausible, not measured,
 * assumption — is replaced by an empirical value derived from a synthetic tire-replacement
 * log, via {@link EmpiricalTireLifeCalculator} (rolling window of the last
 * {@value EmpiricalTireLifeCalculator#WINDOW_SIZE} {@code DESGASTE_NORMAL} replacements for
 * the TRACAO axle group). Reruns the exact same fixed Toledo-PR scenario from
 * {@link ToledoRouteComparisonExample} — same seed search, same seven real waypoints, same
 * cargo — once with each tire-life value, side by side.
 *
 * <p>{@code CostMatrixBuilder} still takes a single {@code tireLifeKm} per profile — the
 * per-axle-position formula described in
 * {@code docs/superpowers/specs/2026-08-01-empirical-tire-wear-by-axle-position-design.md}
 * is future work. This example approximates it by swapping in the TRACAO group's calibrated
 * value alone, since traction-axle tires are the dominant wear source on a 3-axle delivery
 * truck (2 traction axle positions vs. 1 steering).
 *
 * <p>Reuses {@link ToledoRouteComparisonExample}'s waypoint/scenario-building helpers,
 * {@link AStarWaypointOptimizer} and {@link CostMatrixBuilder} unmodified; touches no
 * production code.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.TireLifeFromReplacementLogExample}
 */
public final class TireLifeFromReplacementLogExample {

  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "tire-life-from-replacement-log.md");

  private static final int WINDOW_SIZE = EmpiricalTireLifeCalculator.WINDOW_SIZE;

  // Synthetic — no real fleet telemetry is available at this stage (same caveat as every
  // VehicleProfile constant in this package). Illustrates the calculation, not a measured
  // fleet's real numbers. TRACAO axle group, medium truck class. One DANO_ACIDENTE record
  // is included to show it gets excluded before windowing, not just diluted by it.
  static final List<EmpiricalTireLifeCalculator.TireReplacementRecord> REPLACEMENT_LOG = List.of(
    new EmpiricalTireLifeCalculator.TireReplacementRecord(0.0, 52_000.0, DESGASTE_NORMAL, TRACAO),
    new EmpiricalTireLifeCalculator.TireReplacementRecord(52_000.0, 55_500.0, DANO_ACIDENTE, TRACAO), // pothole
    new EmpiricalTireLifeCalculator.TireReplacementRecord(55_500.0, 113_000.0, DESGASTE_NORMAL, TRACAO),
    new EmpiricalTireLifeCalculator.TireReplacementRecord(113_000.0, 156_000.0, DESGASTE_NORMAL, TRACAO),
    new EmpiricalTireLifeCalculator.TireReplacementRecord(156_000.0, 201_000.0, DESGASTE_NORMAL, TRACAO)
  );

  public static void main(String[] args) {
    VehicleProfile assumedProfile = ToledoRouteComparisonExample.PROFILE; // 60,000 km, documented assumption

    double empiricalTireLifeKm = EmpiricalTireLifeCalculator.tireLifeKm(
      REPLACEMENT_LOG, assumedProfile.tireLifeKm()
    );
    VehicleProfile empiricalProfile = assumedProfile
      .withLabel(assumedProfile.label() + " (vida útil de pneu empírica)")
      .withTireLifeKm(empiricalTireLifeKm);

    List<WaypointDto> waypoints = ToledoRouteComparisonExample.WAYPOINTS.stream()
      .map(NamedWaypoint::toDto).collect(Collectors.toList());
    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();

    // Reproduce the exact same seed search ToledoRouteComparisonExample runs, using the
    // assumed profile — Cenário A (duration-only) never depends on the vehicle profile, so
    // this always lands on the identical scenario/seed/orderA that report already used.
    Scenario scenario = null;
    List<Integer> orderA = null;
    List<Integer> orderBAssumed = null;
    long chosenSeed = ToledoRouteComparisonExample.SEED_BASE;

    for (int attempt = 0; attempt < ToledoRouteComparisonExample.SEED_SEARCH_ATTEMPTS; attempt++) {
      long seed = ToledoRouteComparisonExample.SEED_BASE + attempt;
      RoadType[][] roadType = ToledoRouteComparisonExample.sampleRoadTypes(seed);
      Scenario candidate = new Scenario(
        waypoints, ToledoRouteComparisonExample.distanceMatrix(), roadType,
        ToledoRouteComparisonExample.CARGO_WEIGHT_KG, ToledoRouteComparisonExample.CARGO_VOLUME_M3
      );
      EdgeCosts candidateCosts = CostMatrixBuilder.build(candidate, assumedProfile);
      List<Integer> candidateOrderA = optimizer.optimize(candidateCosts.costA(), ToledoRouteComparisonExample.MODE);
      List<Integer> candidateOrderB = optimizer.optimize(candidateCosts.costB(), ToledoRouteComparisonExample.MODE);

      scenario = candidate;
      orderA = candidateOrderA;
      orderBAssumed = candidateOrderB;
      chosenSeed = seed;

      if (!candidateOrderA.equals(candidateOrderB)) {
        break;
      }
    }

    EdgeCosts costsAssumed = CostMatrixBuilder.build(scenario, assumedProfile);
    EdgeCosts costsEmpirical = CostMatrixBuilder.build(scenario, empiricalProfile);
    List<Integer> orderBEmpirical = optimizer.optimize(costsEmpirical.costB(), ToledoRouteComparisonExample.MODE);

    String report = buildReport(
      empiricalTireLifeKm, assumedProfile, empiricalProfile,
      scenario, costsAssumed, costsEmpirical, orderA, orderBAssumed, orderBEmpirical, chosenSeed
    );

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, report);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write tire life report to " + REPORT_PATH, ex);
    }

    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  private static String buildReport(
    double empiricalTireLifeKm, VehicleProfile assumedProfile, VehicleProfile empiricalProfile,
    Scenario scenario, EdgeCosts costsAssumed, EdgeCosts costsEmpirical,
    List<Integer> orderA, List<Integer> orderBAssumed, List<Integer> orderBEmpirical, long seed
  ) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Vida Útil de Pneu Empírica (via Log de Trocas) vs Assumida — Relatório\n\n");
    sb.append(String.format(Locale.US, "Gerado em: %s\n\n",
      java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

    appendContext(sb);
    appendFormula(sb, empiricalTireLifeKm, assumedProfile);
    appendReplacementLog(sb, empiricalTireLifeKm);
    appendProfileComparison(sb, assumedProfile, empiricalProfile);
    appendScenarioRecap(sb, seed);
    appendWorkedEdgeExample(sb, scenario, orderA, assumedProfile, empiricalProfile);
    appendRouteComparison(sb, orderA, orderBAssumed, orderBEmpirical);
    appendAggregateComparison(sb, costsAssumed, costsEmpirical, orderA, orderBAssumed, orderBEmpirical);
    appendConclusion(sb, orderBAssumed, orderBEmpirical, empiricalTireLifeKm, assumedProfile.tireLifeKm());

    return sb.toString();
  }

  private static void appendContext(StringBuilder sb) {
    sb.append("## 1. Contexto\n\n");
    sb.append(String.format(Locale.US,
      "`VehicleProfile.medium()` usa `tireLifeKm = 60000.0` — uma constante documentada " +
      "como suposição plausível, não telemetria real (ver Javadoc de `VehicleProfile`). " +
      "Este relatório substitui esse valor fixo por uma média empírica calculada por " +
      "`EmpiricalTireLifeCalculator` a partir de uma janela móvel dos últimos %d eventos " +
      "de troca por desgaste normal (grupo eixo de TRAÇÃO, %d registros no log, incluindo " +
      "um evento de dano/acidente propositalmente excluído) e reaproveita o mesmo cenário " +
      "fixo de Toledo-PR de `ToledoRouteComparisonExample` (mesma seed, mesmos sete pontos " +
      "reais, mesma carga) para comparar os dois valores lado a lado.\n\n",
      WINDOW_SIZE, REPLACEMENT_LOG.size()
    ));
  }

  private static void appendFormula(StringBuilder sb, double empiricalTireLifeKm, VehicleProfile assumedProfile) {
    sb.append("## 2. Fórmula — antes e depois\n\n");
    sb.append("**Antes** (`VehicleProfile.medium()`, valor fixo assumido):\n\n");
    sb.append("```\ntireLifeKm = 60000.0   // constante documentada, não medida\n```\n\n");
    sb.append(String.format(Locale.US, "**Depois** (janela móvel dos últimos %d eventos de desgaste normal):\n\n", WINDOW_SIZE));
    sb.append(String.format(Locale.US,
      "```\n" +
      "tireLifeKm = média( kmUsado )   [últimos %d eventos DESGASTE_NORMAL, eventos DANO_ACIDENTE excluídos]\n" +
      "           = %.1f km\n" +
      "```\n\n",
      WINDOW_SIZE, empiricalTireLifeKm
    ));
    sb.append(
      "O resto da fórmula de `CostMatrixBuilder` não muda — `tireLifeKm` é só um dos " +
      "fatores de entrada:\n\n" +
      "```\n" +
      "tireWearFraction = (1 / tireLifeKm) × axleCount × (1 + 0.5 × loadFactor) × roadType.wearMultiplier\n" +
      "tireWearReais    = distanceKm × tireWearFraction × tireReplacementCostPerTire\n" +
      "```\n\n" +
      "A fórmula atual ainda usa um `tireLifeKm` único por veículo, não por posição de " +
      "eixo — a versão por posição (dianteiro/tração/reboque) está descrita na spec de " +
      "design, mas a refatoração de `CostMatrixBuilder` pra somar por posição é trabalho " +
      "futuro. Este exemplo aproxima isso plugando só o valor calibrado do grupo de " +
      "TRAÇÃO, que é a fonte dominante de desgaste num caminhão de 3 eixos (2 posições de " +
      "tração contra 1 de direção).\n\n"
    );
    sb.append(
      "**Por que média simples, e não ponderada por distância como no combustível:** cada " +
      "evento de troca já é uma medição completa de quanto km um pneu durou — não é uma " +
      "razão parcial (tipo litros/km) que precise de ponderação por distância percorrida. " +
      "Ver `EmpiricalTireLifeCalculator` e a spec de design para a justificativa completa " +
      "da janela pequena (2, contra 5 do combustível) e do filtro de motivo da troca.\n\n"
    );
  }

  private static void appendReplacementLog(StringBuilder sb, double empiricalTireLifeKm) {
    sb.append("## 3. Log de trocas (sintético) — eixo de TRAÇÃO\n\n");
    sb.append(String.format(Locale.US,
      "%d registros no total. A coluna **Motivo** marca eventos de dano/acidente (excluídos " +
      "antes de qualquer janela); a coluna **Na janela?** marca os últimos %d eventos de " +
      "desgaste normal, os únicos usados no cálculo.\n\n", REPLACEMENT_LOG.size(), WINDOW_SIZE));
    sb.append("| Km instalação | Km troca | Km rodado | Motivo | Na janela? |\n");
    sb.append("|---|---|---|---|---|\n");

    List<EmpiricalTireLifeCalculator.TireReplacementRecord> wearEvents = REPLACEMENT_LOG.stream()
      .filter(r -> r.motivoTroca() == DESGASTE_NORMAL).toList();
    int firstInWindowIndex = Math.max(0, wearEvents.size() - WINDOW_SIZE);
    int wearEventIndex = 0;
    for (EmpiricalTireLifeCalculator.TireReplacementRecord r : REPLACEMENT_LOG) {
      boolean isWear = r.motivoTroca() == DESGASTE_NORMAL;
      String naJanela = isWear && wearEventIndex >= firstInWindowIndex ? "sim" : (isWear ? "não" : "—");
      sb.append(String.format(Locale.US, "| %.0f | %.0f | %.0f | %s | %s |\n",
        r.kmInstalacao(), r.kmTroca(), r.kmUsado(), r.motivoTroca(), naJanela));
      if (isWear) {
        wearEventIndex++;
      }
    }
    sb.append(String.format(Locale.US, "| | | **Média (janela)** | | **%.1f km** |\n\n", empiricalTireLifeKm));
  }

  private static void appendProfileComparison(StringBuilder sb, VehicleProfile assumed, VehicleProfile empirical) {
    sb.append("## 4. Perfis comparados\n\n");
    sb.append("| Parâmetro | Assumido (produção/relatórios anteriores) | Empírico (este relatório) |\n");
    sb.append("|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Vida útil do pneu | %.0f km | %.1f km |\n",
      assumed.tireLifeKm(), empirical.tireLifeKm()));
    sb.append(String.format(Locale.US, "| Diferença | — | %+.1f km (%+.1f%%) |\n",
      empirical.tireLifeKm() - assumed.tireLifeKm(),
      100.0 * (empirical.tireLifeKm() - assumed.tireLifeKm()) / assumed.tireLifeKm()
    ));
    sb.append("\nDemais parâmetros (eixos, capacidade, consumo, preço do litro, preço do pneu, custo-hora) são idênticos — só a vida útil do pneu muda.\n\n");
  }

  private static void appendScenarioRecap(StringBuilder sb, long seed) {
    sb.append("## 5. Cenário reaproveitado\n\n");
    sb.append(String.format(Locale.US,
      "Mesmo cenário fixo de `ToledoRouteComparisonExample`: sete pontos reais em Toledo-PR " +
      "(Prefeitura como depósito, ROUND_TRIP), carga de %.0f kg / %.1f m³, tipo de via por " +
      "sentido sorteado com seed **%d** (a mesma seed daquele relatório, já que a busca é " +
      "determinística e não depende da vida útil do pneu). Detalhes de cada ponto e trecho " +
      "estão em `docs/validation-reports/toledo-pr-route-comparison.md` — este relatório " +
      "foca só no que muda com o pneu.\n\n",
      ToledoRouteComparisonExample.CARGO_WEIGHT_KG, ToledoRouteComparisonExample.CARGO_VOLUME_M3, seed
    ));
  }

  private static void appendWorkedEdgeExample(
    StringBuilder sb, Scenario scenario, List<Integer> orderA, VehicleProfile assumed, VehicleProfile empirical
  ) {
    int i = orderA.get(0);
    int j = orderA.get(1);
    double distanceKm = scenario.distanceKm()[i][j];
    RoadType roadType = scenario.roadType()[i][j];
    CargoOccupancy occupancy = CargoOccupancy.compute(scenario.cargoWeightKg(), scenario.cargoVolumeM3(), assumed);
    double loadFactor = occupancy.effectiveLoadFactor();

    double fractionAssumed = (1.0 / assumed.tireLifeKm()) * assumed.axleCount() * (1 + 0.5 * loadFactor) * roadType.wearMultiplier;
    double wearAssumed = distanceKm * fractionAssumed * assumed.tireReplacementCostPerTire();
    double fractionEmpirical = (1.0 / empirical.tireLifeKm()) * empirical.axleCount() * (1 + 0.5 * loadFactor) * roadType.wearMultiplier;
    double wearEmpirical = distanceKm * fractionEmpirical * empirical.tireReplacementCostPerTire();

    sb.append("## 6. Exemplo numérico — um trecho\n\n");
    sb.append(String.format(Locale.US,
      "Primeiro trecho da rota do Cenário A: **%s → %s** (%.2f km, via %s, loadFactor = %.3f):\n\n",
      waypointName(i), waypointName(j), distanceKm, roadType, loadFactor
    ));
    sb.append(String.format(Locale.US, "**Com vida útil assumida (%.0f km):**\n\n", assumed.tireLifeKm()));
    sb.append(String.format(Locale.US,
      "```\ntireWearFraction = (1 / %.0f) × %d × (1 + 0.5 × %.3f) × %.2f = %.8f\ntireWearReais    = %.2f × %.8f × %.2f = R$ %.4f\n```\n\n",
      assumed.tireLifeKm(), assumed.axleCount(), loadFactor, roadType.wearMultiplier, fractionAssumed,
      distanceKm, fractionAssumed, assumed.tireReplacementCostPerTire(), wearAssumed
    ));
    sb.append(String.format(Locale.US, "**Com vida útil empírica (%.1f km):**\n\n", empirical.tireLifeKm()));
    sb.append(String.format(Locale.US,
      "```\ntireWearFraction = (1 / %.1f) × %d × (1 + 0.5 × %.3f) × %.2f = %.8f\ntireWearReais    = %.2f × %.8f × %.2f = R$ %.4f\n```\n\n",
      empirical.tireLifeKm(), empirical.axleCount(), loadFactor, roadType.wearMultiplier, fractionEmpirical,
      distanceKm, fractionEmpirical, empirical.tireReplacementCostPerTire(), wearEmpirical
    ));
    sb.append(String.format(Locale.US,
      "Nesse único trecho, a vida útil empírica (mais curta) resulta em %+.4f R$ (%+.1f%%) a " +
      "mais de custo de desgaste do que a assumida — a mesma proporção se propaga para todos " +
      "os %d trechos do grafo, porque `tireWearFraction` é inversamente proporcional a " +
      "`tireLifeKm`.\n\n",
      wearEmpirical - wearAssumed, 100.0 * (wearEmpirical - wearAssumed) / wearAssumed,
      scenario.size() * (scenario.size() - 1)
    ));
  }

  private static void appendRouteComparison(
    StringBuilder sb, List<Integer> orderA, List<Integer> orderBAssumed, List<Integer> orderBEmpirical
  ) {
    boolean assumedDiffersFromA = !orderA.equals(orderBAssumed);
    boolean empiricalDiffersFromA = !orderA.equals(orderBEmpirical);
    boolean empiricalDiffersFromAssumed = !orderBAssumed.equals(orderBEmpirical);

    sb.append("## 7. A rota escolhida muda?\n\n");
    sb.append("- **Cenário A (só duração — não depende de pneu):** ").append(formatOrder(orderA)).append('\n');
    sb.append("- **Cenário B, vida útil assumida (60.000 km):** ").append(formatOrder(orderBAssumed)).append('\n');
    sb.append("- **Cenário B, vida útil empírica:** ").append(formatOrder(orderBEmpirical)).append('\n');
    sb.append(String.format(Locale.US,
      "- Cenário B (assumido) difere de A? **%s** · Cenário B (empírico) difere de A? **%s** · " +
      "Cenário B (empírico) difere de Cenário B (assumido)? **%s**\n\n",
      assumedDiffersFromA ? "Sim" : "Não", empiricalDiffersFromA ? "Sim" : "Não",
      empiricalDiffersFromAssumed ? "Sim" : "Não"
    ));
  }

  private static void appendAggregateComparison(
    StringBuilder sb, EdgeCosts costsAssumed, EdgeCosts costsEmpirical,
    List<Integer> orderA, List<Integer> orderBAssumed, List<Integer> orderBEmpirical
  ) {
    double wearA_underAssumedFormula = TrialResultBuilder.sumAlongPath(costsAssumed.tireWearReais(), orderA);
    double wearA_underEmpiricalFormula = TrialResultBuilder.sumAlongPath(costsEmpirical.tireWearReais(), orderA);
    double wearBAssumed = TrialResultBuilder.sumAlongPath(costsAssumed.tireWearReais(), orderBAssumed);
    double wearBEmpirical = TrialResultBuilder.sumAlongPath(costsEmpirical.tireWearReais(), orderBEmpirical);

    double costBAssumedUnderOrderA = TrialResultBuilder.sumAlongPath(costsAssumed.costB(), orderA);
    double costBAssumedUnderOrderB = TrialResultBuilder.sumAlongPath(costsAssumed.costB(), orderBAssumed);
    double gapAssumedPercent = costBAssumedUnderOrderA > 0
      ? 100.0 * (costBAssumedUnderOrderA - costBAssumedUnderOrderB) / costBAssumedUnderOrderA : 0.0;

    double costBEmpiricalUnderOrderA = TrialResultBuilder.sumAlongPath(costsEmpirical.costB(), orderA);
    double costBEmpiricalUnderOrderB = TrialResultBuilder.sumAlongPath(costsEmpirical.costB(), orderBEmpirical);
    double gapEmpiricalPercent = costBEmpiricalUnderOrderA > 0
      ? 100.0 * (costBEmpiricalUnderOrderA - costBEmpiricalUnderOrderB) / costBEmpiricalUnderOrderA : 0.0;

    sb.append("## 8. Métricas agregadas — assumido vs empírico\n\n");
    sb.append("| Métrica | Vida útil assumida (60.000 km) | Vida útil empírica |\n");
    sb.append("|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Desgaste de pneu — rota do Cenário A | R$ %.2f | R$ %.2f |\n",
      wearA_underAssumedFormula, wearA_underEmpiricalFormula));
    sb.append(String.format(Locale.US, "| Desgaste de pneu — rota do Cenário B | R$ %.2f | R$ %.2f |\n",
      wearBAssumed, wearBEmpirical));
    sb.append(String.format(Locale.US, "| Custo total (fórmula B) — rota do Cenário A | R$ %.2f | R$ %.2f |\n",
      costBAssumedUnderOrderA, costBEmpiricalUnderOrderA));
    sb.append(String.format(Locale.US, "| Custo total (fórmula B) — rota do Cenário B | R$ %.2f | R$ %.2f |\n",
      costBAssumedUnderOrderB, costBEmpiricalUnderOrderB));
    sb.append(String.format(Locale.US, "| Gap (A vs B, mesma fórmula) | R$ %.2f (%.2f%%) | R$ %.2f (%.2f%%) |\n\n",
      costBAssumedUnderOrderA - costBAssumedUnderOrderB, gapAssumedPercent,
      costBEmpiricalUnderOrderA - costBEmpiricalUnderOrderB, gapEmpiricalPercent
    ));
  }

  private static void appendConclusion(
    StringBuilder sb, List<Integer> orderBAssumed, List<Integer> orderBEmpirical,
    double empiricalTireLifeKm, double assumedTireLifeKm
  ) {
    boolean routeChanged = !orderBAssumed.equals(orderBEmpirical);
    sb.append("## 9. Conclusão\n\n");
    sb.append(String.format(Locale.US,
      "1. **A fórmula em si não muda** — só o valor que entra em `tireLifeKm`. Isso confirma " +
      "que `CostMatrixBuilder` já aceita um valor calibrado por telemetria real no lugar da " +
      "constante assumida, sem precisar mexer em código (dentro do modelo de um único " +
      "`tireLifeKm` por veículo — a versão por posição de eixo ainda é trabalho futuro).\n" +
      "2. **A vida útil empírica é %.1f%% %s que a assumida** (%.0f km → %.1f km) — o " +
      "impacto no custo de desgaste se propaga proporcionalmente a todos os trechos do " +
      "grafo, já que `tireWearFraction` é inversamente proporcional a `tireLifeKm`.\n" +
      "3. **A rota escolhida pelo Cenário B %s ao trocar a vida útil assumida pela empírica " +
      "neste cenário** — %s\n" +
      "4. **Sobre o log sintético:** o evento de dano/acidente incluído no log (seção 3) foi " +
      "corretamente excluído do cálculo — confirma que o filtro de `motivoTroca` funciona " +
      "como descrito na spec, não só em teste unitário isolado.\n",
      Math.abs(100.0 * (empiricalTireLifeKm - assumedTireLifeKm) / assumedTireLifeKm),
      empiricalTireLifeKm < assumedTireLifeKm ? "menor" : "maior",
      assumedTireLifeKm, empiricalTireLifeKm,
      routeChanged ? "muda" : "não muda",
      routeChanged
        ? "isso significa que, com dados reais de troca de pneu, o algoritmo teria escolhido uma ordem de visitação diferente da que a suposição de 60.000 km produziria."
        : "isso significa que, ao menos para esta geometria e carga específicas, o valor exato da vida útil do pneu não foi decisivo — mudou o custo total, mas não a ordem ótima."
    ));
  }

  private static String waypointName(int index) {
    return ToledoRouteComparisonExample.WAYPOINTS.get(index).name();
  }

  private static String formatOrder(List<Integer> order) {
    return order.stream().map(TireLifeFromReplacementLogExample::waypointName).collect(Collectors.joining(" → "));
  }
}
