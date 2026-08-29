package com.routewise.validation;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.model.RouteMode;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Replaces {@link VehicleProfile}'s {@code baseFuelConsumptionLPer100Km} — documented in that
 * class as "not measured fleet data ... documented, plausible constants" — with values derived
 * from publicly documented real-vehicle engine specs and manufacturer/industry-reported fuel
 * consumption, one representative vehicle per truck class. Then reruns the 500-scenario Monte
 * Carlo comparison to see whether the correction actually changes anything that matters (route
 * choice, cost magnitude) — the same "does this matter" question
 * {@link CalibratedCostValidationExperiment} and {@link MarginalContributionAnalyzer} ask for
 * other calibration inputs, applied here to a literature-sourced correction instead of a
 * synthetic log.
 *
 * <h2>Derivation methodology</h2>
 *
 * <p>{@code baseFuelConsumptionLPer100Km} is defined (see {@link VehicleProfile}'s Javadoc) as
 * consumption <em>at empty load, on an ARTERIAL road</em> ({@link RoadType#ARTERIAL},
 * {@code fuelMultiplier = 1.0}, {@code loadFactor = 0}). Public spec sheets report city
 * ("cidade", ≈ {@link RoadType#URBANA}) and highway ("estrada", ≈ {@link RoadType#RODOVIA})
 * figures instead, so the ARTERIAL/empty baseline is backed out by inverting
 * {@link CostMatrixBuilder}'s own formula ({@code loadFactor = 0} at spec-sheet conditions):
 *
 * <pre>
 *   consumptionAtRoadType = base * roadType.fuelMultiplier
 *   base_from_city    = cityLPer100Km    / URBANA.fuelMultiplier   (1.3)
 *   base_from_highway = highwayLPer100Km / RODOVIA.fuelMultiplier  (0.85)
 *   base              = average(base_from_city, base_from_highway)
 * </pre>
 *
 * <h3>Leve — Hyundai HR 2.5 CRDi (motor D4CB, 2.5L turbodiesel, 130 cv)</h3>
 * Cidade 8 km/L (12,50 L/100km), estrada 11 km/L (9,09 L/100km).
 * base = média(12,50/1,3; 9,09/0,85) = média(9,62; 10,70) = <b>10,16 L/100km</b>
 * (atual: 10,0 — praticamente igual, diferença de +1,6%).
 *
 * <h3>Médio — Mercedes-Benz Accelo 815 (motor OM924 LA, 4.8L, 156 cv, 580 Nm)</h3>
 * Cidade 4,2 km/L (23,81 L/100km), estrada 6,8 km/L (14,71 L/100km).
 * base = média(23,81/1,3; 14,71/0,85) = média(18,32; 17,31) = <b>17,81 L/100km</b>
 * (atual: 15,0 — diferença de +18,7%).
 *
 * <h3>Pesado — bitrem carregado (cavalo Scania R450, motor DC13, 12.7L, 450 cv, 2350 Nm)</h3>
 * Diferente dos outros dois: não há ficha pública "vazio, cidade/estrada" para uma combinação
 * bitrem completa (18 pneus, como {@link VehicleProfile#heavy()} modela — 2 dianteiro + 8 tração
 * + 8 reboque). O dado público disponível é o consumo médio já <em>carregado</em>, em
 * rodovia: 1,8–2,3 km/L (ponto médio 2,0 km/L = 50,0 L/100km). Assumindo {@code loadFactor ≈ 1}
 * (quase cheio) sobre RODOVIA, e invertendo a parcela de combustível inteira (inclui o fator de
 * carga, {@code LOAD_FUEL_FACTOR = 0.30}):
 * base = 50,0 / [(1 + 0,30·1) · 0,85] = 50,0 / 1,105 = <b>45,25 L/100km</b>
 * (atual: 32,0 — diferença de +41,3%). Faixa correspondente ao intervalo público (1,8–2,3 km/L):
 * 39,3–50,3 L/100km — a maior incerteza dos três, por depender de uma suposição de carga que os
 * outros dois não precisam (spec de fábrica já é "vazio" por definição).
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.EngineSpecFuelCalibrationExperiment}
 *
 * <p>Fontes (consultadas em 2026-08-13): motortudo.com (Hyundai HR), consultadeplaca.net/blog
 * (Accelo 815 — dados compilados de testes reais/frotistas), blog.caminhoesecarretas.com.br
 * (Scania R450 / motor DC13), blog.fretebras.com.br e infleet.com.br (faixas de consumo de
 * bitrem carregado por categoria). Nenhuma é telemetria de frota própria — ver ressalva na
 * seção 1 do relatório gerado.
 */
public final class EngineSpecFuelCalibrationExperiment {

  private static final int TRIALS = 500;
  private static final long SEED = 42L;
  private static final RouteMode MODE = RouteMode.ROUND_TRIP;
  private static final Path REPORT_PATH =
    Path.of("..", "docs", "refactor-math-result", "08-especificacoes-reais-motor.md");

  private record RealSpecProfile(
    VehicleProfile original,
    VehicleProfile corrected,
    String vehicleModel,
    String engineModel,
    String derivationNote,
    String sourceNote
  ) {}

  private static final List<RealSpecProfile> PROFILES = List.of(
    new RealSpecProfile(
      VehicleProfile.light(),
      VehicleProfile.light()
        .withLabel("Leve (2 eixos) — motor real: Hyundai HR D4CB")
        .withFuelConsumption(10.16),
      "Hyundai HR 2.5 CRDi",
      "D4CB 2.5L turbodiesel, 130 cv",
      "Cidade 8 km/L (12,50 L/100km) e estrada 11 km/L (9,09 L/100km); "
        + "base = média(12,50/1,3; 9,09/0,85) = média(9,62; 10,70) = 10,16 L/100km.",
      "motortudo.com/ficha-tecnica-hyundai-hr-hd-2-5-turbo-2021"
    ),
    new RealSpecProfile(
      VehicleProfile.medium(),
      VehicleProfile.medium()
        .withLabel("Médio (3 eixos) — motor real: MB Accelo 815 OM924")
        .withFuelConsumption(17.81),
      "Mercedes-Benz Accelo 815",
      "OM924 LA 4.8L, 156 cv, 580 Nm",
      "Cidade 4,2 km/L (23,81 L/100km) e estrada 6,8 km/L (14,71 L/100km); "
        + "base = média(23,81/1,3; 14,71/0,85) = média(18,32; 17,31) = 17,81 L/100km.",
      "consultadeplaca.net/blog/ficha-tecnica-accelo-815"
    ),
    new RealSpecProfile(
      VehicleProfile.heavy(),
      VehicleProfile.heavy()
        .withLabel("Pesado (5 eixos) — motor real: bitrem c/ Scania R450 DC13")
        .withFuelConsumption(45.25),
      "Bitrem (cavalo Scania R450)",
      "DC13 12.7L, 450 cv, 2350 Nm",
      "Consumo médio carregado em rodovia 1,8–2,3 km/L (ponto médio 2,0 km/L = 50,0 L/100km); "
        + "assumindo loadFactor≈1 sobre RODOVIA: base = 50,0 / [(1+0,30·1)·0,85] = "
        + "50,0/1,105 = 45,25 L/100km (faixa: 39,3–50,3 L/100km).",
      "blog.fretebras.com.br/caminhao-bitrem; infleet.com.br/blog/tabela-consumo-combustivel-caminhoes"
    )
  );

  private record ComparisonResult(
    double pctRoutesDiffer,
    double meanCostOriginal,
    double meanCostCorrected,
    double meanFuelCostOriginal,
    double meanFuelCostCorrected,
    double gapReaisMean,
    double gapPercentMean,
    double wilcoxonPValue
  ) {}

  public static void main(String[] args) {
    // Same road/cargo scenarios for every profile pair, so only the fuel-consumption
    // constant differs between the two runs being compared (mirrors EmpiricalCostValidationByVehicle).
    Random rng = new Random(SEED);
    ScenarioGenerator generator = new ScenarioGenerator(rng);
    List<Scenario> scenarios = new ArrayList<>(TRIALS);
    for (int t = 0; t < TRIALS; t++) {
      scenarios.add(generator.generate());
    }

    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();

    List<ComparisonResult> comparisons = new ArrayList<>(PROFILES.size());
    for (RealSpecProfile spec : PROFILES) {
      comparisons.add(compare(spec, scenarios, optimizer));
    }

    writeReport(comparisons);

    for (int i = 0; i < PROFILES.size(); i++) {
      RealSpecProfile spec = PROFILES.get(i);
      ComparisonResult r = comparisons.get(i);
      double pctDiffConsumption = 100.0 * (spec.corrected().baseFuelConsumptionLPer100Km()
        / spec.original().baseFuelConsumptionLPer100Km() - 1.0);
      System.out.printf(Locale.US,
        "%s: consumo base %.2f -> %.2f L/100km (%+.1f%%), %.1f%% rotas diferentes, gap médio R$ %.2f (%.2f%%)%n",
        spec.original().label(), spec.original().baseFuelConsumptionLPer100Km(),
        spec.corrected().baseFuelConsumptionLPer100Km(), pctDiffConsumption,
        r.pctRoutesDiffer(), r.gapReaisMean(), r.gapPercentMean());
    }
    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  private static ComparisonResult compare(
    RealSpecProfile spec, List<Scenario> scenarios, AStarWaypointOptimizer optimizer
  ) {
    int total = scenarios.size();
    int differCount = 0;
    double sumCostOriginal = 0, sumCostCorrected = 0, sumFuelOriginal = 0, sumFuelCorrected = 0;
    double[] costUnderCorrectedFnOriginalRoute = new double[total];
    double[] costUnderCorrectedFnCorrectedRoute = new double[total];

    for (int i = 0; i < total; i++) {
      Scenario scenario = scenarios.get(i);
      EdgeCosts originalCosts = CostMatrixBuilder.build(scenario, spec.original());
      EdgeCosts correctedCosts = CostMatrixBuilder.build(scenario, spec.corrected());

      List<Integer> orderOriginal = optimizer.optimize(originalCosts.costB(), MODE);
      List<Integer> orderCorrected = optimizer.optimize(correctedCosts.costB(), MODE);

      if (!orderOriginal.equals(orderCorrected)) {
        differCount++;
      }

      sumCostOriginal += TrialResultBuilder.sumAlongPath(originalCosts.costB(), orderOriginal);
      sumCostCorrected += TrialResultBuilder.sumAlongPath(correctedCosts.costB(), orderCorrected);
      sumFuelOriginal += TrialResultBuilder.sumAlongPath(originalCosts.fuelLiters(), orderOriginal)
        * spec.original().fuelPricePerLiter();
      sumFuelCorrected += TrialResultBuilder.sumAlongPath(correctedCosts.fuelLiters(), orderCorrected)
        * spec.corrected().fuelPricePerLiter();

      // Same "does the correction matter" comparison MarginalContributionAnalyzer uses for a new
      // cost component: both routes evaluated under the SAME (corrected) cost function, so the
      // gap isolates the value of routing with the corrected profile instead of the placeholder.
      costUnderCorrectedFnOriginalRoute[i] = TrialResultBuilder.sumAlongPath(correctedCosts.costB(), orderOriginal);
      costUnderCorrectedFnCorrectedRoute[i] = TrialResultBuilder.sumAlongPath(correctedCosts.costB(), orderCorrected);
    }

    double gapReaisSum = 0, gapPercentSum = 0;
    int gapPercentCount = 0;
    for (int i = 0; i < total; i++) {
      double gap = costUnderCorrectedFnOriginalRoute[i] - costUnderCorrectedFnCorrectedRoute[i];
      gapReaisSum += gap;
      if (costUnderCorrectedFnOriginalRoute[i] > 0) {
        gapPercentSum += 100.0 * gap / costUnderCorrectedFnOriginalRoute[i];
        gapPercentCount++;
      }
    }

    double wilcoxonP = runWilcoxon(costUnderCorrectedFnOriginalRoute, costUnderCorrectedFnCorrectedRoute);

    return new ComparisonResult(
      100.0 * differCount / total,
      sumCostOriginal / total, sumCostCorrected / total,
      sumFuelOriginal / total, sumFuelCorrected / total,
      gapReaisSum / total,
      gapPercentCount > 0 ? gapPercentSum / gapPercentCount : 0.0,
      wilcoxonP
    );
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

  private static void writeReport(List<ComparisonResult> comparisons) {
    StringBuilder sb = new StringBuilder();
    sb.append("# Calibração de Consumo com Especificações Reais de Motor\n\n");
    sb.append("Gerado por `EngineSpecFuelCalibrationExperiment` — ").append(TRIALS)
      .append(" cenários sintéticos (seed ").append(SEED).append("), mesmos para as duas rodadas ")
      .append("de cada perfil (só `baseFuelConsumptionLPer100Km` muda).\n\n");

    sb.append("## 1. Metodologia e ressalva\n\n");
    sb.append("`VehicleProfile.baseFuelConsumptionLPer100Km` é definido como consumo a vazio, ")
      .append("em via ARTERIAL. Fichas técnicas públicas informam cidade/estrada, então a base ")
      .append("ARTERIAL é obtida invertendo a própria fórmula do `CostMatrixBuilder` — ver Javadoc ")
      .append("de `EngineSpecFuelCalibrationExperiment` para a derivação completa, número por número, ")
      .append("de cada um dos três perfis.\n\n");
    sb.append("**Nenhum desses números é telemetria de frota própria** — são fichas técnicas de ")
      .append("fabricante e médias reportadas por fontes do setor (ver Seção 4), a mesma ressalva já ")
      .append("registrada em `VehicleProfile` para os valores que estão sendo substituídos aqui. O ")
      .append("perfil Pesado tem a maior incerteza dos três: não existe ficha pública \"vazio, ")
      .append("cidade/estrada\" para uma combinação bitrem completa, então sua base foi obtida a ")
      .append("partir de um consumo médio já carregado, assumindo `loadFactor ≈ 1`.\n\n");

    sb.append("## 2. Consumo base: placeholder atual vs. derivado de motor real\n\n");
    sb.append("| Perfil | Veículo / motor real | Atual (placeholder) | Derivado (fonte real) | Diferença |\n");
    sb.append("|---|---|---|---|---|\n");
    for (RealSpecProfile spec : PROFILES) {
      double pctDiff = 100.0 * (spec.corrected().baseFuelConsumptionLPer100Km()
        / spec.original().baseFuelConsumptionLPer100Km() - 1.0);
      sb.append(String.format(Locale.US, "| %s | %s (%s) | %.1f L/100km | %.2f L/100km | %+.1f%% |\n",
        spec.original().label(), spec.vehicleModel(), spec.engineModel(),
        spec.original().baseFuelConsumptionLPer100Km(), spec.corrected().baseFuelConsumptionLPer100Km(),
        pctDiff));
    }
    sb.append("\n");
    for (RealSpecProfile spec : PROFILES) {
      sb.append("- **").append(spec.original().label()).append("** (").append(spec.vehicleModel())
        .append(", ").append(spec.engineModel()).append("): ").append(spec.derivationNote())
        .append(" Fonte: ").append(spec.sourceNote()).append("\n");
    }

    sb.append("\n## 3. Efeito no experimento de 500 cenários (mesma rota A* / mesmo custoB)\n\n");
    sb.append("| Perfil | Rotas diferentes | Custo médio atual | Custo médio corrigido | ")
      .append("Combustível médio atual | Combustível médio corrigido | Gap médio (R$) | Gap médio (%) | Wilcoxon p |\n");
    sb.append("|---|---|---|---|---|---|---|---|---|\n");
    for (int i = 0; i < PROFILES.size(); i++) {
      RealSpecProfile spec = PROFILES.get(i);
      ComparisonResult r = comparisons.get(i);
      sb.append(String.format(Locale.US,
        "| %s | %.1f%% | R$ %.2f | R$ %.2f | R$ %.2f | R$ %.2f | R$ %.2f | %.2f%% | %s |\n",
        spec.original().label(), r.pctRoutesDiffer(), r.meanCostOriginal(), r.meanCostCorrected(),
        r.meanFuelCostOriginal(), r.meanFuelCostCorrected(), r.gapReaisMean(), r.gapPercentMean(),
        Double.isNaN(r.wilcoxonPValue()) ? "N/A" : String.format(Locale.US, "%.4f", r.wilcoxonPValue())
      ));
    }

    sb.append("\n\"Rotas diferentes\" = quantos dos 500 cenários o A* escolhe uma ordem diferente ")
      .append("ao trocar só o consumo-base pelo valor derivado de motor real, mantendo tudo o mais ")
      .append("igual. \"Gap médio\" = quanto, em R$ sob a função de custo corrigida, a rota antiga ")
      .append("(otimizada com o placeholder) perde para a rota recalculada com o consumo real — ")
      .append("sempre ≥ 0 por construção, mesma lógica do `MarginalContributionAnalyzer`.\n\n");

    sb.append("## 4. Conclusão\n\n");
    for (int i = 0; i < PROFILES.size(); i++) {
      RealSpecProfile spec = PROFILES.get(i);
      ComparisonResult r = comparisons.get(i);
      boolean significant = !Double.isNaN(r.wilcoxonPValue()) && r.wilcoxonPValue() < 0.05;
      sb.append("- **").append(spec.original().label()).append("**: ")
        .append(String.format(Locale.US, "%.1f%% das rotas mudam", r.pctRoutesDiffer()))
        .append(significant ? " e a diferença é estatisticamente significativa (Wilcoxon p < 0,05)."
          : " — sem significância estatística suficiente nesta amostra (Wilcoxon).")
        .append(String.format(Locale.US,
          " Custo médio previsto sobe de R$ %.2f para R$ %.2f (%+.1f%%) só pela troca do consumo-base.\n",
          r.meanCostOriginal(), r.meanCostCorrected(),
          100.0 * (r.meanCostCorrected() / r.meanCostOriginal() - 1.0)));
    }
    sb.append("\nO perfil **Leve** mudou pouco (o placeholder já estava perto do valor real do "
      + "Hyundai HR). **Médio** e, principalmente, **Pesado** tinham consumo-base subestimado — "
      + "o placeholder de 32,0 L/100km para o perfil pesado está bem abaixo da faixa real de um "
      + "bitrem carregado (39,3–50,3 L/100km), então qualquer conclusão de TCC que dependa da "
      + "magnitude absoluta do custo de combustível para veículos pesados deveria usar os valores "
      + "desta calibração, não os placeholders originais de `VehicleProfile`.\n\n");

    sb.append("## 5. Fontes\n\n");
    sb.append("- Hyundai HR 2.5 CRDi (D4CB): motortudo.com — ficha técnica HR HD 2.5 Turbo 2021\n");
    sb.append("- Mercedes-Benz Accelo 815 (OM924 LA): consultadeplaca.net/blog — ficha técnica Accelo 815 "
      + "(dados compilados de testes reais/ABRACAM e frotistas)\n");
    sb.append("- Scania R450 (motor DC13): blog.caminhoesecarretas.com.br — ficha técnica Scania R450\n");
    sb.append("- Consumo de bitrem carregado: blog.fretebras.com.br/caminhao-bitrem; "
      + "infleet.com.br/blog/tabela-consumo-combustivel-caminhoes\n");
    sb.append("\nConsultadas em 2026-08-13. Nenhuma é telemetria de frota própria do RouteWise — "
      + "ver ressalva na Seção 1.\n");

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, sb.toString());
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write engine-spec calibration report to " + REPORT_PATH, ex);
    }
  }
}
