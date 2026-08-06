package com.routewise.validation;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Generates the capacity (kg / m³) dimension report — how a load's weight and volume
 * resolve into the single {@code loadFactor} that drives the fuel and tire formulas.
 *
 * <p>Written by {@link RefactorMathReportSuite}; not run on its own.
 */
final class CapacityDimensionReport {

  private static final double EDGE_KM = 100.0;
  private static final RoadType EDGE_ROAD = RoadType.ARTERIAL;

  private CapacityDimensionReport() {}

  static void write(Path directory, List<VehicleProfile> profiles) {
    StringBuilder sb = new StringBuilder();
    appendSummary(sb);
    appendCapacities(sb, profiles);
    appendBindingConstraint(sb, profiles);
    appendPropagation(sb, profiles);
    MarkdownReportFile.write(directory.resolve("01-capacidade-peso-volume.md"), sb.toString());
  }

  private static void appendSummary(StringBuilder sb) {
    sb.append("# Capacidade do Veículo — Peso (kg) e Volume (m³)\n\n");
    sb.append(MarkdownReportFile.generatedAt());
    sb.append("## Resumo do que foi feito\n\n");
    sb.append(
      "**Nada mudou nesta dimensão neste refactor** — ela já estava implementada. Este " +
      "relatório revalida o comportamento sob a matemática de pneu corrigida (ver " +
      "`04-desgaste-pneu-por-eixo.md`), que altera os valores absolutos da última seção.\n\n" +
      "O modelo segue a prática de frete de \"peso x cubagem\": uma carga é limitada pela " +
      "dimensão mais restritiva, não pelo peso sozinho. Carga leve e volumosa enche o baú " +
      "muito antes de atingir o limite de peso; carga densa faz o inverso.\n\n" +
      "```\n" +
      "fraçãoPeso   = cargoWeightKg / capacityKg\n" +
      "fraçãoVolume = cargoVolumeM3 / capacityM3\n" +
      "ocupação     = max(fraçãoPeso, fraçãoVolume)   // sem limite — decide viabilidade\n" +
      "loadFactor   = min(ocupação, 1,0)              // limitado — alimenta as fórmulas de custo\n" +
      "```\n\n" +
      "`loadFactor` é limitado a 1,0 porque as fórmulas de combustível e desgaste só foram " +
      "calibradas nessa faixa — extrapolá-las para uma carga inviável produziria número sem " +
      "lastro. Implementado em `CargoOccupancy`, coberto por `CargoOccupancyTest`.\n\n"
    );
  }

  private static void appendCapacities(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append("## 1. Capacidade por classe\n\n");
    sb.append("| Perfil | Capacidade (peso) | Capacidade (volume) | Densidade de equilíbrio |\n");
    sb.append("|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      sb.append(String.format(Locale.US, "| %s | %,.0f kg | %.1f m³ | %.0f kg/m³ |\n",
        p.label(), p.capacityKg(), p.capacityM3(), p.capacityKg() / p.capacityM3()));
    }
    sb.append("\nA **densidade de equilíbrio** é a densidade de carga em que peso e volume " +
      "esgotam juntos. Carga mais densa que isso é limitada por peso; menos densa, por volume. " +
      "Repare que ela cai conforme o veículo cresce — o baú cresce mais rápido que a capacidade " +
      "de carga, então caminhão grande satura por peso com mais facilidade.\n\n");
  }

  private static void appendBindingConstraint(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append("## 2. Qual dimensão limita, por carga × veículo\n\n");
    sb.append("| Carga | Perfil | Fração peso | Fração volume | Restrição | loadFactor | Viável? |\n");
    sb.append("|---|---|---|---|---|---|---|\n");
    List<CargoOccupancyScenario> cargos = List.of(
      new CargoOccupancyScenario("Densa", "aço", 3600.0, 5.0),
      new CargoOccupancyScenario("Volumosa", "espuma", 800.0, 22.5),
      new CargoOccupancyScenario("Excesso de peso", "", 5000.0, 10.0),
      new CargoOccupancyScenario("Excesso de volume", "", 500.0, 30.0)
    );
    for (CargoOccupancyScenario c : cargos) {
      for (VehicleProfile p : profiles) {
        CargoOccupancy o = CargoOccupancy.compute(c.cargoWeightKg(), c.cargoVolumeM3(), p);
        sb.append(String.format(Locale.US, "| %s (%,.0f kg / %.1f m³) | %s | %.0f%% | %.0f%% | %s | %.2f | %s |\n",
          c.name(), c.cargoWeightKg(), c.cargoVolumeM3(), p.label(),
          o.weightFraction() * 100, o.volumeFraction() * 100,
          o.volumeBound() ? "Volume" : "Peso", o.effectiveLoadFactor(),
          o.feasible() ? "Sim" : "**Não**"));
      }
    }
    sb.append("\nA mesma carga troca de restrição conforme o veículo — validar só peso deixaria " +
      "passar metade dos casos de estouro. O `AStarWaypointOptimizer` continua cego a isso: " +
      "roteia cargas inviáveis normalmente, porque não recebe informação de capacidade.\n\n");
  }

  private static void appendPropagation(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append(String.format(Locale.US,
      "## 3. Como `loadFactor` propaga para o custo (%.0f km, via %s)\n\n", EDGE_KM, EDGE_ROAD));
    sb.append("| Perfil | loadFactor | Combustível | Custo pneu | Custo total do trecho |\n");
    sb.append("|---|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      for (double load : new double[] {0.0, 0.5, 1.0}) {
        Scenario scenario = edgeScenario(p.capacityKg() * load, 0.0);
        EdgeCosts costs = CostMatrixBuilder.build(scenario, p);
        sb.append(String.format(Locale.US, "| %s | %.2f | %.2f L | R$ %.2f | R$ %.2f |\n",
          p.label(), load, costs.fuelLiters()[0][1], costs.tireWearReais()[0][1], costs.costB()[0][1]));
      }
    }
    sb.append("\nCarga cheia acrescenta 30% ao combustível e 50% ao desgaste de pneu. O custo " +
      "de motorista não se move — só depende da duração, que independe da carga. É por isso " +
      "que o Cenário A (duração pura, comportamento de produção hoje) é completamente " +
      "insensível a quanto o caminhão está carregado.\n");
  }

  private static Scenario edgeScenario(double cargoWeightKg, double cargoVolumeM3) {
    return new Scenario(
      List.of(new com.routewise.dto.WaypointDto(-23.55, -46.63),
              new com.routewise.dto.WaypointDto(-23.50, -46.60)),
      new double[][] {{0, EDGE_KM}, {EDGE_KM, 0}},
      new RoadType[][] {{EDGE_ROAD, EDGE_ROAD}, {EDGE_ROAD, EDGE_ROAD}},
      cargoWeightKg, cargoVolumeM3
    );
  }
}
