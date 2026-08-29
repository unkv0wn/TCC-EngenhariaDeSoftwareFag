package com.routewise.validation;

import com.routewise.dto.WaypointDto;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Generates the time and distance dimension report. The two are covered together because
 * they are not independent inputs — duration is derived from distance and road type, so
 * separating them would document the same formula twice.
 *
 * <p>Written by {@link RefactorMathReportSuite}; not run on its own.
 */
final class TimeDistanceDimensionReport {

  private TimeDistanceDimensionReport() {}

  static void write(Path directory, List<VehicleProfile> profiles) {
    StringBuilder sb = new StringBuilder();
    appendSummary(sb);
    appendSpeedTable(sb);
    appendDistanceScaling(sb, profiles);
    appendCostComposition(sb, profiles);
    MarkdownReportFile.write(directory.resolve("03-tempo-e-quilometragem.md"), sb.toString());
  }

  private static void appendSummary(StringBuilder sb) {
    sb.append("# Tempo e Quilometragem\n\n");
    sb.append(MarkdownReportFile.generatedAt());
    sb.append("## Resumo do que foi feito\n\n");
    sb.append(
      "**Nada mudou nestas dimensões neste refactor** — ambas já estavam implementadas. Este " +
      "relatório revalida o comportamento e mostra a composição do custo sob a matemática de " +
      "pneu corrigida (ver `04-desgaste-pneu-por-eixo.md`).\n\n" +
      "As duas estão no mesmo documento porque **não são entradas independentes**: a duração é " +
      "derivada da distância e do tipo de via, não medida separadamente.\n\n" +
      "```\n" +
      "durationSec   = distanceKm / roadType.avgSpeedKmh × 3600\n" +
      "timeCostReais = (durationSec / 3600) × driverCostPerHourReais\n" +
      "```\n\n" +
      "A quilometragem é a entrada primária — entra em todas as três parcelas de custo " +
      "(motorista via duração, combustível, pneu). O tempo só entra no custo de motorista. " +
      "**A duração é o único componente que o Cenário A (produção hoje) enxerga.**\n\n"
    );
  }

  private static void appendSpeedTable(StringBuilder sb) {
    sb.append("## 1. Velocidade por tipo de via\n\n");
    sb.append("| Via | Velocidade média | Tempo por 100 km |\n|---|---|---|\n");
    for (RoadType road : RoadType.values()) {
      sb.append(String.format(Locale.US, "| %s | %.0f km/h | %.0f min |\n",
        road, road.avgSpeedKmh, 100.0 / road.avgSpeedKmh * 60));
    }
    sb.append("\nMesma distância, tempos muito diferentes: 100 km urbanos levam **3,2x** o " +
      "tempo de 100 km em rodovia. É essa não-proporcionalidade entre distância e duração que " +
      "torna o experimento informativo — se tempo e distância fossem proporcionais, otimizar " +
      "por um seria idêntico a otimizar pelo outro.\n\n");
  }

  private static void appendDistanceScaling(StringBuilder sb, List<VehicleProfile> profiles) {
    VehicleProfile profile = profiles.get(1); // medium — representative
    sb.append(String.format(Locale.US,
      "## 2. Escala com a distância (%s, vazio, via arterial)\n\n", profile.label()));
    sb.append("| Distância | Duração | Custo motorista | Combustível | Pneu | Total |\n");
    sb.append("|---|---|---|---|---|---|\n");
    for (double km : new double[] {10, 50, 100, 250, 500}) {
      EdgeCosts costs = CostMatrixBuilder.build(edgeScenario(km, RoadType.ARTERIAL, 0.0), profile);
      sb.append(String.format(Locale.US, "| %,.0f km | %.0f min | R$ %.2f | R$ %.2f | R$ %.2f | R$ %.2f |\n",
        km, costs.durationSec()[0][1] / 60,
        costs.durationSec()[0][1] / 3600 * profile.driverCostPerHourReais(),
        costs.fuelLiters()[0][1] * profile.fuelPricePerLiter(),
        costs.tireWearReais()[0][1], costs.costB()[0][1]));
    }
    sb.append("\nTodas as parcelas escalam linearmente com a distância, então a **proporção** " +
      "entre elas é constante para um dado tipo de via. O que muda a proporção é o tipo de via " +
      "e a carga — não o comprimento do trecho.\n\n");
  }

  private static void appendCostComposition(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append("## 3. Composição do custo por tipo de via (100 km, vazio)\n\n");
    sb.append("| Perfil | Via | Motorista | Combustível | Pneu | Total |\n");
    sb.append("|---|---|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      for (RoadType road : RoadType.values()) {
        EdgeCosts costs = CostMatrixBuilder.build(edgeScenario(100.0, road, 0.0), p);
        double total = costs.costB()[0][1];
        double driver = costs.durationSec()[0][1] / 3600 * p.driverCostPerHourReais();
        double fuel = costs.fuelLiters()[0][1] * p.fuelPricePerLiter();
        double tire = costs.tireWearReais()[0][1];
        sb.append(String.format(Locale.US, "| %s | %s | %.0f%% | %.0f%% | %.0f%% | R$ %.2f |\n",
          p.label(), road, driver / total * 100, fuel / total * 100, tire / total * 100, total));
      }
    }
    sb.append("\nConforme a via piora, a parcela de motorista cresce e a de combustível " +
      "encolhe — a duração dispara enquanto a distância é a mesma. Mas o ponto de virada " +
      "depende da classe: no perfil leve o motorista passa a dominar em via urbana (56%), " +
      "enquanto no pesado o combustível continua sendo a maior parcela mesmo no urbano (52%), " +
      "porque o consumo de uma carreta é alto demais para o tempo compensar. Em rodovia o " +
      "combustível é a maior parcela em todas as classes.\n\n" +
      "**É exatamente por isso que otimizar por duração pura pode escolher uma rota diferente " +
      "de otimizar por custo real** — as duas ordenam os trechos por critérios que não são " +
      "proporcionais entre si, e o desalinhamento muda de tamanho conforme o veículo.\n");
  }

  private static Scenario edgeScenario(double km, RoadType road, double cargoWeightKg) {
    return new Scenario(
      List.of(new WaypointDto(-23.55, -46.63), new WaypointDto(-23.50, -46.60)),
      new double[][] {{0, km}, {km, 0}},
      new RoadType[][] {{road, road}, {road, road}},
      cargoWeightKg, 0.0
    );
  }
}
