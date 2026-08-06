package com.routewise.validation;

import com.routewise.dto.WaypointDto;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Generates the fuel consumption dimension report — base consumption per class, how load
 * and road type adjust it, and what the empirical rolling-window calibration produces.
 *
 * <p>Written by {@link RefactorMathReportSuite}; not run on its own.
 */
final class FuelDimensionReport {

  private static final double EDGE_KM = 100.0;

  private FuelDimensionReport() {}

  static void write(Path directory, List<VehicleProfile> profiles) {
    StringBuilder sb = new StringBuilder();
    appendSummary(sb);
    appendBaseConsumption(sb, profiles);
    appendAdjustment(sb, profiles);
    appendEmpiricalCalibration(sb);
    MarkdownReportFile.write(directory.resolve("02-consumo-combustivel.md"), sb.toString());
  }

  private static void appendSummary(StringBuilder sb) {
    sb.append("# Consumo Médio de Combustível\n\n");
    sb.append(MarkdownReportFile.generatedAt());
    sb.append("## Resumo do que foi feito\n\n");
    sb.append(
      "**Nada mudou nesta dimensão neste refactor** — ela já estava implementada, incluindo a " +
      "calibração empírica por janela móvel. Este relatório revalida o comportamento e " +
      "documenta os números atuais.\n\n" +
      "O consumo parte de uma constante por classe (`baseFuelConsumptionLPer100Km`, medida a " +
      "vazio em via arterial) e é ajustado por carga e tipo de via:\n\n" +
      "```\n" +
      "consumoAjustado = baseFuelConsumptionLPer100Km × (1 + 0,30 × loadFactor) × fuelMultiplier\n" +
      "fuelLiters      = distanceKm × consumoAjustado / 100\n" +
      "```\n\n" +
      "`EmpiricalFuelConsumptionCalculator` pode substituir a constante base por um valor " +
      "derivado do log de abastecimento (janela móvel dos últimos 5 abastecimentos, razão " +
      "litros/km ponderada por distância). Coberto por `EmpiricalFuelConsumptionCalculatorTest`.\n\n"
    );
  }

  private static void appendBaseConsumption(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append("## 1. Consumo base por classe\n\n");
    sb.append("| Perfil | Consumo base | Equivalente | Preço diesel |\n|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      sb.append(String.format(Locale.US, "| %s | %.1f L/100km | %.2f km/L | R$ %.2f/L |\n",
        p.label(), p.baseFuelConsumptionLPer100Km(),
        100.0 / p.baseFuelConsumptionLPer100Km(), p.fuelPricePerLiter()));
    }
    sb.append("\nValores a vazio, em via arterial — a referência a partir da qual os " +
      "multiplicadores abaixo operam.\n\n");
  }

  private static void appendAdjustment(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append(String.format(Locale.US,
      "## 2. Consumo ajustado por carga e via (trecho de %.0f km)\n\n", EDGE_KM));
    sb.append("| Perfil | Ocupação | Rodovia (×0,85) | Arterial (×1,00) | Urbana (×1,30) |\n");
    sb.append("|---|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      for (double load : new double[] {0.0, 1.0}) {
        sb.append(String.format(Locale.US, "| %s | %.0f%% |", p.label(), load * 100));
        for (RoadType road : RoadType.values()) {
          Scenario scenario = edgeScenario(p.capacityKg() * load, road);
          EdgeCosts costs = CostMatrixBuilder.build(scenario, p);
          sb.append(String.format(Locale.US, " %.1f L (R$ %.2f) |",
            costs.fuelLiters()[0][1], costs.fuelLiters()[0][1] * p.fuelPricePerLiter()));
        }
        sb.append('\n');
      }
    }
    sb.append("\nO pior caso (carregado, urbano) consome **1,99x** o melhor (vazio, rodovia) " +
      "no mesmo trecho de mesma distância — praticamente o dobro. Essa diferença é invisível " +
      "para o Cenário A, que só enxerga duração.\n\n");
  }

  private static void appendEmpiricalCalibration(StringBuilder sb) {
    sb.append("## 3. Calibração empírica (janela móvel)\n\n");
    VehicleProfile assumed = VehicleProfile.defaultProfile();
    double empirical = EmpiricalFuelConsumptionCalculator.consumptionLPer100Km(
      FuelConsumptionFromRefuelingExample.REFUELING_LOG, assumed.baseFuelConsumptionLPer100Km()
    );
    sb.append("| Fonte | Consumo | Custo por 100 km |\n|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Constante assumida | %.2f L/100km | R$ %.2f |\n",
      assumed.baseFuelConsumptionLPer100Km(),
      assumed.baseFuelConsumptionLPer100Km() * assumed.fuelPricePerLiter()));
    sb.append(String.format(Locale.US, "| Log de abastecimento (últimos %d) | %.2f L/100km | R$ %.2f |\n",
      EmpiricalFuelConsumptionCalculator.WINDOW_SIZE, empirical, empirical * assumed.fuelPricePerLiter()));
    sb.append(String.format(Locale.US, "| **Diferença** | **%+.2f L/100km** | **%+.1f%%** |\n\n",
      empirical - assumed.baseFuelConsumptionLPer100Km(),
      (empirical / assumed.baseFuelConsumptionLPer100Km() - 1) * 100));
    sb.append(
      "A janela móvel existe para o número acompanhar o estado atual do veículo — motor " +
      "desgastando, pneu descalibrado, rota mudando de perfil — em vez de diluir isso numa " +
      "média histórica que nunca esquece. Cinco abastecimentos é curto o bastante para " +
      "reagir e longo o bastante para um tanque atípico não dominar o resultado.\n"
    );
  }

  private static Scenario edgeScenario(double cargoWeightKg, RoadType road) {
    return new Scenario(
      List.of(new WaypointDto(-23.55, -46.63), new WaypointDto(-23.50, -46.60)),
      new double[][] {{0, EDGE_KM}, {EDGE_KM, 0}},
      new RoadType[][] {{road, road}, {road, road}},
      cargoWeightKg, 0.0
    );
  }
}
