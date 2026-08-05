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
 * Worked example: what changes if {@code VehicleProfile.medium()}'s fixed 15 L/100km
 * "consumo base" — documented in {@link CostMatrixBuilder} as a plausible, not measured,
 * assumption — is replaced by an empirical average derived from a synthetic refueling
 * log, via {@link EmpiricalFuelConsumptionCalculator} (rolling window of the last
 * {@value EmpiricalFuelConsumptionCalculator#WINDOW_SIZE} refuels, Σ litros / Σ km × 100).
 * Reruns the exact same fixed Toledo-PR scenario from {@link ToledoRouteComparisonExample}
 * — same seed search, same seven real waypoints, same cargo — once with each consumption
 * value, side by side.
 *
 * <p>Reuses {@link ToledoRouteComparisonExample}'s waypoint/scenario-building helpers,
 * {@link AStarWaypointOptimizer} and {@link CostMatrixBuilder} unmodified; touches no
 * production code.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.FuelConsumptionFromRefuelingExample}
 */
public final class FuelConsumptionFromRefuelingExample {

  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "fuel-consumption-from-refueling.md");

  private static final int WINDOW_SIZE = EmpiricalFuelConsumptionCalculator.WINDOW_SIZE;

  // Synthetic — no real fleet telemetry is available at this stage (same caveat as every
  // VehicleProfile constant in this package). Illustrates the calculation, not a measured
  // fleet's real numbers. 8 records on file, but only the last WINDOW_SIZE feed the average
  // (see EmpiricalFuelConsumptionCalculator) — the earlier ones are here to show the window
  // sliding past them.
  static final List<EmpiricalFuelConsumptionCalculator.RefuelingRecord> REFUELING_LOG = List.of(
    new EmpiricalFuelConsumptionCalculator.RefuelingRecord("2026-01-08", 215.0, 1200.0),
    new EmpiricalFuelConsumptionCalculator.RefuelingRecord("2026-01-22", 198.0, 1080.0),
    new EmpiricalFuelConsumptionCalculator.RefuelingRecord("2026-02-05", 230.0, 1250.0),
    new EmpiricalFuelConsumptionCalculator.RefuelingRecord("2026-02-19", 205.0, 1150.0),
    new EmpiricalFuelConsumptionCalculator.RefuelingRecord("2026-03-05", 190.0, 1030.0),
    new EmpiricalFuelConsumptionCalculator.RefuelingRecord("2026-03-19", 222.0, 1190.0),
    new EmpiricalFuelConsumptionCalculator.RefuelingRecord("2026-04-02", 208.0, 1120.0),
    new EmpiricalFuelConsumptionCalculator.RefuelingRecord("2026-04-16", 214.0, 1160.0)
  );

  public static void main(String[] args) {
    VehicleProfile assumedProfile = ToledoRouteComparisonExample.PROFILE; // 15.0 L/100km, documented assumption

    List<EmpiricalFuelConsumptionCalculator.RefuelingRecord> window =
      REFUELING_LOG.subList(Math.max(0, REFUELING_LOG.size() - WINDOW_SIZE), REFUELING_LOG.size());
    double totalLiters = window.stream().mapToDouble(EmpiricalFuelConsumptionCalculator.RefuelingRecord::litersRefueled).sum();
    double totalKm = window.stream().mapToDouble(EmpiricalFuelConsumptionCalculator.RefuelingRecord::kmSincePrevious).sum();
    double empiricalConsumption = EmpiricalFuelConsumptionCalculator.consumptionLPer100Km(
      REFUELING_LOG, assumedProfile.baseFuelConsumptionLPer100Km()
    );
    VehicleProfile empiricalProfile = new VehicleProfile(
      assumedProfile.label() + " (consumo empírico)", assumedProfile.axleCount(),
      assumedProfile.capacityKg(), assumedProfile.capacityM3(),
      empiricalConsumption, assumedProfile.fuelPricePerLiter(),
      assumedProfile.tireReplacementCostPerTire(), assumedProfile.tireLifeKm(),
      assumedProfile.driverCostPerHourReais()
    );

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
      empiricalConsumption, totalLiters, totalKm, assumedProfile, empiricalProfile,
      scenario, costsAssumed, costsEmpirical, orderA, orderBAssumed, orderBEmpirical, chosenSeed
    );

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, report);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write fuel consumption report to " + REPORT_PATH, ex);
    }

    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  private static String buildReport(
    double empiricalConsumption, double totalLiters, double totalKm,
    VehicleProfile assumedProfile, VehicleProfile empiricalProfile,
    Scenario scenario, EdgeCosts costsAssumed, EdgeCosts costsEmpirical,
    List<Integer> orderA, List<Integer> orderBAssumed, List<Integer> orderBEmpirical, long seed
  ) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Consumo Empírico (via Histórico de Abastecimento) vs Consumo Assumido — Relatório\n\n");
    sb.append(String.format(Locale.US, "Gerado em: %s\n\n",
      java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

    appendContext(sb, empiricalConsumption);
    appendFormula(sb, empiricalConsumption);
    appendRefuelingLog(sb, totalLiters, totalKm, empiricalConsumption);
    appendProfileComparison(sb, assumedProfile, empiricalProfile);
    appendScenarioRecap(sb, seed);
    appendWorkedEdgeExample(sb, scenario, orderA, assumedProfile, empiricalProfile);
    appendRouteComparison(sb, orderA, orderBAssumed, orderBEmpirical);
    appendAggregateComparison(sb, scenario, costsAssumed, costsEmpirical, orderA, orderBAssumed, orderBEmpirical);
    appendConclusion(sb, orderBAssumed, orderBEmpirical, empiricalConsumption, assumedProfile.baseFuelConsumptionLPer100Km());

    return sb.toString();
  }

  private static void appendContext(StringBuilder sb, double empiricalConsumption) {
    sb.append("## 1. Contexto\n\n");
    sb.append(String.format(Locale.US,
      "`VehicleProfile.medium()` usa `baseFuelConsumptionLPer100Km = 15.0` — uma constante " +
      "documentada como suposição plausível, não telemetria real (ver Javadoc de " +
      "`VehicleProfile`). Este relatório substitui esse valor fixo por uma média empírica " +
      "calculada por `EmpiricalFuelConsumptionCalculator` a partir de uma janela móvel dos " +
      "últimos %d abastecimentos de um histórico sintético (%d registros no total) e " +
      "reaproveita o mesmo cenário fixo de Toledo-PR de `ToledoRouteComparisonExample` " +
      "(mesma seed, mesmos sete pontos reais, mesma carga) para comparar os dois valores " +
      "lado a lado — o que muda na fórmula, e o que muda no resultado do teste.\n\n",
      WINDOW_SIZE, REFUELING_LOG.size()
    ));
  }

  private static void appendFormula(StringBuilder sb, double empiricalConsumption) {
    sb.append("## 2. Fórmula — antes e depois\n\n");
    sb.append("**Antes** (`VehicleProfile.medium()`, valor fixo assumido):\n\n");
    sb.append("```\nbaseFuelConsumptionLPer100Km = 15.0   // constante documentada, não medida\n```\n\n");
    sb.append(String.format(Locale.US, "**Depois** (janela móvel dos últimos %d abastecimentos):\n\n", WINDOW_SIZE));
    sb.append(String.format(Locale.US,
      "```\n" +
      "baseFuelConsumptionLPer100Km = ( Σ litrosAbastecidos / Σ kmRodados ) × 100   [últimos %d abastecimentos]\n" +
      "                              = ( %.1f / %.1f ) × 100\n" +
      "                              = %.2f L/100km\n" +
      "```\n\n",
      WINDOW_SIZE,
      REFUELING_LOG.subList(Math.max(0, REFUELING_LOG.size() - WINDOW_SIZE), REFUELING_LOG.size())
        .stream().mapToDouble(EmpiricalFuelConsumptionCalculator.RefuelingRecord::litersRefueled).sum(),
      REFUELING_LOG.subList(Math.max(0, REFUELING_LOG.size() - WINDOW_SIZE), REFUELING_LOG.size())
        .stream().mapToDouble(EmpiricalFuelConsumptionCalculator.RefuelingRecord::kmSincePrevious).sum(),
      empiricalConsumption
    ));
    sb.append(
      "O resto da fórmula de `CostMatrixBuilder` não muda — `baseFuelConsumptionLPer100Km` " +
      "é só um dos fatores de entrada:\n\n" +
      "```\n" +
      "consumoAjustado = baseFuelConsumptionLPer100Km × (1 + 0.30 × loadFactor) × roadType.fuelMultiplier\n" +
      "fuelLiters      = distanceKm × consumoAjustado / 100\n" +
      "```\n\n" +
      "Ou seja: a mudança está inteiramente em **qual número entra** na fórmula, não na " +
      "fórmula em si — `loadFactor` (peso/volume da carga) e `roadType.fuelMultiplier` " +
      "(tipo de via) continuam ajustando esse valor base do mesmo jeito.\n\n"
    );
    sb.append(String.format(Locale.US,
      "**Por que janela móvel, e não o histórico inteiro:** um único tanque isolado tem " +
      "ruído alto (trecho de rodovia vs. cidade, carga variando de viagem pra viagem); o " +
      "histórico inteiro suaviza esse ruído, mas também demora a refletir uma mudança real " +
      "recente (pneu careca, motor precisando de revisão). A janela dos últimos %d " +
      "abastecimentos é o meio-termo: estável o bastante pra não ser dominada por um tanque " +
      "atípico, recente o bastante pra acompanhar o estado atual do veículo. Ver " +
      "`EmpiricalFuelConsumptionCalculator` e a spec de design para a política completa, " +
      "incluindo o comportamento de partida a frio (veículo sem histórico ainda).\n\n" +
      "**Por que soma de litros / soma de km dentro da janela, e não a média simples do " +
      "consumo de cada abastecimento individual:** cada abastecimento tem um número " +
      "diferente de km rodados; uma média simples das razões `litros/km` de cada linha pesa " +
      "igualmente um tanque que rodou 1030 km e um que rodou 1250 km, distorcendo o " +
      "resultado para cima ou para baixo dependendo de quais tanques tiveram trechos mais " +
      "curtos. Dividir a soma total de litros pela soma total de km pondera cada " +
      "abastecimento pela distância real que ele cobriu — é o consumo médio correto do " +
      "período, não a média das médias.\n\n",
      WINDOW_SIZE
    ));
  }

  private static void appendRefuelingLog(StringBuilder sb, double totalLiters, double totalKm, double empiricalConsumption) {
    sb.append("## 3. Histórico de abastecimento (sintético)\n\n");
    sb.append(String.format(Locale.US,
      "%d registros no total; a coluna **Na janela?** marca os últimos %d, que são os únicos " +
      "usados no cálculo (os mais antigos aparecem só pra contexto — mostram o que a janela " +
      "deixou de fora).\n\n", REFUELING_LOG.size(), WINDOW_SIZE));
    sb.append("| Data | Litros abastecidos | Km rodados desde o último | Consumo do tanque | Na janela? |\n");
    sb.append("|---|---|---|---|---|\n");
    int firstInWindowIndex = Math.max(0, REFUELING_LOG.size() - WINDOW_SIZE);
    for (int idx = 0; idx < REFUELING_LOG.size(); idx++) {
      EmpiricalFuelConsumptionCalculator.RefuelingRecord r = REFUELING_LOG.get(idx);
      sb.append(String.format(Locale.US, "| %s | %.1f L | %.0f km | %.2f L/100km | %s |\n",
        r.date(), r.litersRefueled(), r.kmSincePrevious(), r.consumptionLPer100Km(),
        idx >= firstInWindowIndex ? "sim" : "não"));
    }
    sb.append(String.format(Locale.US, "| **Total / Média ponderada (janela)** | **%.1f L** | **%.0f km** | **%.2f L/100km** | |\n\n",
      totalLiters, totalKm, empiricalConsumption));
    List<EmpiricalFuelConsumptionCalculator.RefuelingRecord> window =
      REFUELING_LOG.subList(firstInWindowIndex, REFUELING_LOG.size());
    double simpleAverage = window.stream()
      .mapToDouble(EmpiricalFuelConsumptionCalculator.RefuelingRecord::consumptionLPer100Km).average().orElse(0.0);
    sb.append(String.format(Locale.US,
      "Para comparação: a média simples (não ponderada) das linhas na janela seria %.4f " +
      "L/100km — próxima, mas não idêntica, à média ponderada de %.4f L/100km usada neste " +
      "relatório; a diferença entre as duas cresce quanto mais desigual for a quilometragem " +
      "entre tanques.\n\n",
      simpleAverage, empiricalConsumption
    ));
  }

  private static void appendProfileComparison(StringBuilder sb, VehicleProfile assumed, VehicleProfile empirical) {
    sb.append("## 4. Perfis comparados\n\n");
    sb.append("| Parâmetro | Assumido (produção/relatórios anteriores) | Empírico (este relatório) |\n");
    sb.append("|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Consumo base | %.1f L/100km | %.2f L/100km |\n",
      assumed.baseFuelConsumptionLPer100Km(), empirical.baseFuelConsumptionLPer100Km()));
    sb.append(String.format(Locale.US, "| Diferença | — | %+.2f L/100km (%+.1f%%) |\n",
      empirical.baseFuelConsumptionLPer100Km() - assumed.baseFuelConsumptionLPer100Km(),
      100.0 * (empirical.baseFuelConsumptionLPer100Km() - assumed.baseFuelConsumptionLPer100Km()) / assumed.baseFuelConsumptionLPer100Km()
    ));
    sb.append("\nDemais parâmetros (eixos, capacidade, preço do litro, pneu, custo-hora) são idênticos — só o consumo base muda.\n\n");
  }

  private static void appendScenarioRecap(StringBuilder sb, long seed) {
    sb.append("## 5. Cenário reaproveitado\n\n");
    sb.append(String.format(Locale.US,
      "Mesmo cenário fixo de `ToledoRouteComparisonExample`: sete pontos reais em Toledo-PR " +
      "(Prefeitura como depósito, ROUND_TRIP), carga de %.0f kg / %.1f m³, tipo de via por " +
      "sentido sorteado com seed **%d** (a mesma seed daquele relatório, já que a busca é " +
      "determinística e não depende do consumo de combustível). Detalhes de cada ponto e " +
      "trecho estão em `docs/validation-reports/toledo-pr-route-comparison.md` — este " +
      "relatório foca só no que muda com o consumo.\n\n",
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

    double consumoAjustadoAssumed = assumed.baseFuelConsumptionLPer100Km() * (1 + 0.30 * loadFactor) * roadType.fuelMultiplier;
    double fuelAssumed = distanceKm * consumoAjustadoAssumed / 100.0;
    double consumoAjustadoEmpirical = empirical.baseFuelConsumptionLPer100Km() * (1 + 0.30 * loadFactor) * roadType.fuelMultiplier;
    double fuelEmpirical = distanceKm * consumoAjustadoEmpirical / 100.0;

    sb.append("## 6. Exemplo numérico — um trecho\n\n");
    sb.append(String.format(Locale.US,
      "Primeiro trecho da rota do Cenário A: **%s → %s** (%.2f km, via %s, loadFactor = %.3f):\n\n",
      waypointName(i), waypointName(j), distanceKm, roadType, loadFactor
    ));
    sb.append("**Com consumo assumido (15.0 L/100km):**\n\n");
    sb.append(String.format(Locale.US,
      "```\nconsumoAjustado = 15.0 × (1 + 0.30 × %.3f) × %.2f = %.3f L/100km\nfuelLiters      = %.2f × %.3f / 100 = %.3f L\n```\n\n",
      loadFactor, roadType.fuelMultiplier, consumoAjustadoAssumed, distanceKm, consumoAjustadoAssumed, fuelAssumed
    ));
    sb.append(String.format(Locale.US, "**Com consumo empírico (%.2f L/100km):**\n\n", empirical.baseFuelConsumptionLPer100Km()));
    sb.append(String.format(Locale.US,
      "```\nconsumoAjustado = %.2f × (1 + 0.30 × %.3f) × %.2f = %.3f L/100km\nfuelLiters      = %.2f × %.3f / 100 = %.3f L\n```\n\n",
      empirical.baseFuelConsumptionLPer100Km(), loadFactor, roadType.fuelMultiplier, consumoAjustadoEmpirical, distanceKm, consumoAjustadoEmpirical, fuelEmpirical
    ));
    sb.append(String.format(Locale.US,
      "Nesse único trecho, o consumo empírico consome %+.3f L (%+.1f%%) a mais do que o " +
      "assumido — a mesma proporção de aumento (%.1f%%) do consumo base se propaga para " +
      "todos os %d trechos do grafo, porque `consumoAjustado` é linear em " +
      "`baseFuelConsumptionLPer100Km`.\n\n",
      fuelEmpirical - fuelAssumed, 100.0 * (fuelEmpirical - fuelAssumed) / fuelAssumed,
      100.0 * (empirical.baseFuelConsumptionLPer100Km() - assumed.baseFuelConsumptionLPer100Km()) / assumed.baseFuelConsumptionLPer100Km(),
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
    sb.append("- **Cenário A (só duração — não depende de combustível):** ").append(formatOrder(orderA)).append('\n');
    sb.append("- **Cenário B, consumo assumido (15.0 L/100km):** ").append(formatOrder(orderBAssumed)).append('\n');
    sb.append("- **Cenário B, consumo empírico:** ").append(formatOrder(orderBEmpirical)).append('\n');
    sb.append(String.format(Locale.US,
      "- Cenário B (assumido) difere de A? **%s** · Cenário B (empírico) difere de A? **%s** · " +
      "Cenário B (empírico) difere de Cenário B (assumido)? **%s**\n\n",
      assumedDiffersFromA ? "Sim" : "Não", empiricalDiffersFromA ? "Sim" : "Não",
      empiricalDiffersFromAssumed ? "Sim" : "Não"
    ));
  }

  private static void appendAggregateComparison(
    StringBuilder sb, Scenario scenario, EdgeCosts costsAssumed, EdgeCosts costsEmpirical,
    List<Integer> orderA, List<Integer> orderBAssumed, List<Integer> orderBEmpirical
  ) {
    double fuelA_underAssumedFormula = TrialResultBuilder.sumAlongPath(costsAssumed.fuelLiters(), orderA);
    double fuelA_underEmpiricalFormula = TrialResultBuilder.sumAlongPath(costsEmpirical.fuelLiters(), orderA);
    double fuelBAssumed = TrialResultBuilder.sumAlongPath(costsAssumed.fuelLiters(), orderBAssumed);
    double fuelBEmpirical = TrialResultBuilder.sumAlongPath(costsEmpirical.fuelLiters(), orderBEmpirical);

    double costBAssumedUnderOrderA = TrialResultBuilder.sumAlongPath(costsAssumed.costB(), orderA);
    double costBAssumedUnderOrderB = TrialResultBuilder.sumAlongPath(costsAssumed.costB(), orderBAssumed);
    double gapAssumedPercent = costBAssumedUnderOrderA > 0
      ? 100.0 * (costBAssumedUnderOrderA - costBAssumedUnderOrderB) / costBAssumedUnderOrderA : 0.0;

    double costBEmpiricalUnderOrderA = TrialResultBuilder.sumAlongPath(costsEmpirical.costB(), orderA);
    double costBEmpiricalUnderOrderB = TrialResultBuilder.sumAlongPath(costsEmpirical.costB(), orderBEmpirical);
    double gapEmpiricalPercent = costBEmpiricalUnderOrderA > 0
      ? 100.0 * (costBEmpiricalUnderOrderA - costBEmpiricalUnderOrderB) / costBEmpiricalUnderOrderA : 0.0;

    sb.append("## 8. Métricas agregadas — assumido vs empírico\n\n");
    sb.append("| Métrica | Consumo assumido (15.0 L/100km) | Consumo empírico |\n");
    sb.append("|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Combustível — rota do Cenário A | %.2f L | %.2f L |\n",
      fuelA_underAssumedFormula, fuelA_underEmpiricalFormula));
    sb.append(String.format(Locale.US, "| Combustível — rota do Cenário B | %.2f L | %.2f L |\n",
      fuelBAssumed, fuelBEmpirical));
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
    double empiricalConsumption, double assumedConsumption
  ) {
    boolean routeChanged = !orderBAssumed.equals(orderBEmpirical);
    sb.append("## 9. Conclusão\n\n");
    sb.append(String.format(Locale.US,
      "1. **A fórmula em si não muda** — só o valor que entra em `baseFuelConsumptionLPer100Km`. " +
      "Isso confirma que `CostMatrixBuilder` já está desenhado para aceitar um valor " +
      "calibrado por telemetria real no lugar da constante assumida, sem precisar mexer em " +
      "código.\n" +
      "2. **O aumento é proporcional em todo trecho:** %.2f → %.2f L/100km é um aumento de " +
      "%.1f%%, e como `consumoAjustado` é linear em `baseFuelConsumptionLPer100Km`, todo " +
      "litro calculado no grafo sobe na mesma proporção — não só no trecho do exemplo da " +
      "seção 6.\n" +
      "3. **A rota escolhida pelo Cenário B %s ao trocar o consumo assumido pelo empírico " +
      "neste cenário** — %s\n" +
      "4. **Generalização:** este é um único cenário fixo; o efeito de recalibrar o consumo " +
      "base tende a ser mais visível quanto mais díspares forem os tipos de via/carga entre " +
      "as rotas candidatas (ver `docs/validation-reports/empirical-cost-validation.md` para " +
      "o comportamento agregado em 500 cenários sintéticos). O objetivo aqui era mostrar " +
      "*como* plugar um consumo derivado de abastecimento real na fórmula existente, não " +
      "afirmar que 15.0 L/100km está errado.\n",
      assumedConsumption, empiricalConsumption,
      100.0 * (empiricalConsumption - assumedConsumption) / assumedConsumption,
      routeChanged ? "muda" : "não muda",
      routeChanged
        ? "isso significa que, com dados reais de abastecimento, o algoritmo teria escolhido uma ordem de visitação diferente da que a suposição de 15 L/100km produziria."
        : "isso significa que, ao menos para esta geometria e carga específicas, o valor exato do consumo base não foi decisivo — mudou o custo total, mas não a ordem ótima."
    ));
  }

  private static String waypointName(int index) {
    return ToledoRouteComparisonExample.WAYPOINTS.get(index).name();
  }

  private static String formatOrder(List<Integer> order) {
    return order.stream().map(FuelConsumptionFromRefuelingExample::waypointName).collect(Collectors.joining(" → "));
  }
}
