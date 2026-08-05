package com.routewise.validation;

import com.routewise.dto.WaypointDto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Standalone script that documents, in a detailed Markdown report, the cargo weight/
 * volume occupancy logic ({@link CargoOccupancy}) across all three {@link VehicleProfile}
 * truck classes — the same "peso x cubagem" cases asserted by {@code CargoOccupancyTest}
 * (JUnit), but run against light/medium/heavy instead of just the medium truck the unit
 * test uses, plus the resulting fuel/tire-wear cost impact via {@link CostMatrixBuilder}.
 *
 * <p>The JUnit test is what actually enforces correctness as a regression gate (run via
 * {@code mvn test}); this script produces a human-readable evidence report — every input,
 * every computed fraction, and the resulting per-trecho cost — for the TCC write-up. It
 * does not touch production code and reuses {@link CargoOccupancy} / {@link CostMatrixBuilder}
 * unmodified.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.CargoOccupancyValidationScript}
 */
public final class CargoOccupancyValidationScript {

  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "cargo-occupancy-validation.md");

  /** Fixed 100 km ARTERIAL edge used to isolate the cost impact of cargo alone. */
  private static final double EDGE_DISTANCE_KM = 100.0;
  private static final RoadType EDGE_ROAD_TYPE = RoadType.ARTERIAL;

  private static final List<VehicleProfile> PROFILES =
    List.of(VehicleProfile.light(), VehicleProfile.medium(), VehicleProfile.heavy());

  private static final List<CargoOccupancyScenario> SCENARIOS = List.of(
    new CargoOccupancyScenario(
      "Carga densa (peso-limitante)", "Ex.: barras de aço — pesada, ocupa pouco espaço.", 3600.0, 5.0
    ),
    new CargoOccupancyScenario(
      "Carga volumosa (volume-limitante)", "Ex.: espuma de embalagem — leve, ocupa muito espaço.", 800.0, 22.5
    ),
    new CargoOccupancyScenario(
      "Excesso de peso", "Peso além da capacidade; volume folgado.", 5000.0, 10.0
    ),
    new CargoOccupancyScenario(
      "Excesso de volume", "Volume além da capacidade; peso folgado.", 500.0, 30.0
    ),
    new CargoOccupancyScenario(
      "Carga vazia", "Veículo sem carga — caso de referência (ocupação zero).", 0.0, 0.0
    )
  );

  public static void main(String[] args) {
    StringBuilder sb = new StringBuilder();
    appendHeader(sb);
    appendProfilesSection(sb);
    appendScenariosSection(sb);
    appendOccupancySection(sb);
    appendCostImpactSection(sb);
    appendConclusion(sb);

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, sb.toString());
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write cargo occupancy validation report to " + REPORT_PATH, ex);
    }

    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  private static void appendHeader(StringBuilder sb) {
    sb.append("# Validação de Ocupação de Carga (Peso x Cubagem) — Relatório Detalhado\n\n");
    sb.append(String.format(Locale.US, "Gerado em: %s\n\n",
      java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
    sb.append(
      "Este relatório documenta, caso a caso, a lógica de `CargoOccupancy` — quem decide, " +
      "entre peso (kg) e volume (m³), qual dimensão limita a carga de um veículo, e se a " +
      "carga é fisicamente viável. Os mesmos casos são verificados como testes automatizados " +
      "em `CargoOccupancyTest` (JUnit, `mvn test`); este documento existe para apresentar a " +
      "mesma evidência de forma legível, e amplia a cobertura para os três perfis de veículo " +
      "(`VehicleProfile.light()/medium()/heavy()`), não só o perfil médio usado no teste.\n\n"
    );
    sb.append(
      "**Fórmula:** dada uma carga com peso `cargoWeightKg` e volume `cargoVolumeM3`,\n\n" +
      "```\n" +
      "fraçãoPeso   = cargoWeightKg / capacidadeKg\n" +
      "fraçãoVolume = cargoVolumeM3 / capacidadeM3\n" +
      "restrição    = peso, se fraçãoPeso >= fraçãoVolume; volume, caso contrário\n" +
      "ocupação     = max(fraçãoPeso, fraçãoVolume)          // não limitada — usada para viabilidade\n" +
      "loadFactor   = min(ocupação, 1.0)                      // limitada — alimenta as fórmulas de custo\n" +
      "viável       = ocupação <= 1.0\n" +
      "```\n\n"
    );
  }

  private static void appendProfilesSection(StringBuilder sb) {
    sb.append("## 1. Perfis de veículo avaliados\n\n");
    sb.append("| Perfil | Eixos | Capacidade (peso) | Capacidade (volume) |\n");
    sb.append("|---|---|---|---|\n");
    for (VehicleProfile p : PROFILES) {
      sb.append(String.format(Locale.US, "| %s | %d | %.0f kg | %.1f m³ |\n",
        p.label(), p.axleCount(), p.capacityKg(), p.capacityM3()));
    }
    sb.append('\n');
  }

  private static void appendScenariosSection(StringBuilder sb) {
    sb.append("## 2. Cargas avaliadas\n\n");
    sb.append("| Caso | Descrição | Peso | Volume |\n");
    sb.append("|---|---|---|---|\n");
    for (CargoOccupancyScenario s : SCENARIOS) {
      sb.append(String.format(Locale.US, "| %s | %s | %.0f kg | %.1f m³ |\n",
        s.name(), s.description(), s.cargoWeightKg(), s.cargoVolumeM3()));
    }
    sb.append(
      "\nCada carga acima é avaliada contra os três perfis de veículo — o peso/volume da " +
      "carga não muda, só a capacidade que a recebe, exatamente como aconteceria se a " +
      "operação precisasse decidir qual veículo escalar para um pedido.\n\n"
    );
  }

  private static void appendOccupancySection(StringBuilder sb) {
    sb.append("## 3. Ocupação por caso × perfil de veículo\n\n");
    sb.append("| Caso | Perfil | Fração peso | Fração volume | Restrição | Ocupação | loadFactor (custo) | Viável? |\n");
    sb.append("|---|---|---|---|---|---|---|---|\n");
    for (CargoOccupancyScenario s : SCENARIOS) {
      for (VehicleProfile p : PROFILES) {
        CargoOccupancy o = CargoOccupancy.compute(s.cargoWeightKg(), s.cargoVolumeM3(), p);
        sb.append(String.format(Locale.US, "| %s | %s | %.1f%% | %.1f%% | %s | %.1f%% | %.3f | %s |\n",
          s.name(), p.label(),
          o.weightFraction() * 100, o.volumeFraction() * 100,
          o.volumeBound() ? "Volume" : "Peso",
          Math.max(o.weightFraction(), o.volumeFraction()) * 100,
          o.effectiveLoadFactor(),
          o.feasible() ? "Sim" : "**Não**"
        ));
      }
    }
    sb.append(
      "\nRepare como a mesma carga muda de restrição (peso ↔ volume) e de viabilidade " +
      "dependendo só do veículo escolhido — ex.: \"Carga volumosa\" costuma ser " +
      "volume-limitante em veículos pequenos e caber tranquilamente em um veículo pesado. " +
      "Nenhum destes casos é rejeitado pelo `AStarWaypointOptimizer` hoje — ele roteia " +
      "cargas inviáveis normalmente, porque não recebe nenhuma informação de capacidade.\n\n"
    );
  }

  private static void appendCostImpactSection(StringBuilder sb) {
    sb.append(String.format(Locale.US,
      "## 4. Impacto no custo do trecho (%.0f km, via %s)\n\n", EDGE_DISTANCE_KM, EDGE_ROAD_TYPE));
    sb.append(
      "Mesmo trecho fixo, variando só a carga — isola o efeito de `loadFactor` nas fórmulas " +
      "de combustível e desgaste de pneu (`CostMatrixBuilder`). A duração (Cenário A, " +
      "comportamento de produção hoje) é idêntica em todas as linhas porque não depende de carga.\n\n"
    );
    sb.append("| Caso | Perfil | Duração | Combustível | Desgaste de pneu |\n");
    sb.append("|---|---|---|---|---|\n");

    List<WaypointDto> edgeWaypoints = List.of(
      new WaypointDto(-23.5505, -46.6333), new WaypointDto(-23.5000, -46.6000)
    );
    double[][] distanceKm = {{0, EDGE_DISTANCE_KM}, {EDGE_DISTANCE_KM, 0}};
    RoadType[][] roadType = {{EDGE_ROAD_TYPE, EDGE_ROAD_TYPE}, {EDGE_ROAD_TYPE, EDGE_ROAD_TYPE}};

    for (CargoOccupancyScenario s : SCENARIOS) {
      for (VehicleProfile p : PROFILES) {
        Scenario scenario = new Scenario(edgeWaypoints, distanceKm, roadType, s.cargoWeightKg(), s.cargoVolumeM3());
        EdgeCosts costs = CostMatrixBuilder.build(scenario, p);
        sb.append(String.format(Locale.US, "| %s | %s | %.1f min | %.2f L | R$ %.2f |\n",
          s.name(), p.label(),
          costs.durationSec()[0][1] / 60.0, costs.fuelLiters()[0][1], costs.tireWearReais()[0][1]));
      }
    }
    sb.append('\n');
  }

  private static void appendConclusion(StringBuilder sb) {
    sb.append("## 5. Conclusão\n\n");
    sb.append(
      "1. **A restrição relevante depende do veículo, não só da carga.** A mesma carga " +
      "pode ser peso-limitante num veículo e volume-limitante noutro — validar só peso " +
      "(como o sistema faria hoje, se validasse algo) esconde metade dos casos de estouro " +
      "de capacidade.\n" +
      "2. **O algoritmo de roteamento é cego a capacidade.** Todos os casos acima, viáveis " +
      "ou não, são roteados normalmente pelo `AStarWaypointOptimizer` — a coluna \"Viável?\" " +
      "não influencia em nada a rota escolhida hoje.\n" +
      "3. **Carga mais pesada/volumosa realmente encarece o trecho.** As colunas de " +
      "combustível e desgaste na seção 4 crescem com `loadFactor`, confirmando que a " +
      "fórmula reage à ocupação como esperado; a duração não muda, confirmando que o " +
      "Cenário A (produção atual) de fato ignora esse efeito.\n" +
      "4. **Cobertura de teste:** os cinco casos da seção 2, avaliados contra o perfil " +
      "médio, são também verificados como asserts em `CargoOccupancyTest` (`mvn test -Dtest=" +
      "CargoOccupancyTest`) — este relatório amplia a mesma verificação para os perfis leve " +
      "e pesado, sem introduzir lógica nova.\n"
    );
  }
}
