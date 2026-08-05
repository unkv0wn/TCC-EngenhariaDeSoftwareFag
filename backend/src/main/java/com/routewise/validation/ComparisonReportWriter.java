package com.routewise.validation;

import com.routewise.dto.WaypointDto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Writes a detailed, single-scenario side-by-side comparison between Cenário A
 * (time-only, today's production behaviour) and Cenário B (combined empirical cost)
 * for one fixed set of waypoints — a worked example to complement the aggregate
 * statistical report from {@link ReportWriter}.
 */
public final class ComparisonReportWriter {

  private ComparisonReportWriter() {}

  public static void write(
    Path outputPath, Scenario scenario, EdgeCosts costs,
    List<Integer> orderA, List<Integer> orderB, VehicleProfile profile, long seed
  ) {
    String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    boolean routesDiffer = !orderA.equals(orderB);

    double costBUnderOrderA = TrialResultBuilder.sumAlongPath(costs.costB(), orderA);
    double costBUnderOrderB = TrialResultBuilder.sumAlongPath(costs.costB(), orderB);
    double gapReais = costBUnderOrderA - costBUnderOrderB;
    double gapPercent = costBUnderOrderA > 0 ? 100.0 * gapReais / costBUnderOrderA : 0.0;

    StringBuilder sb = new StringBuilder();
    sb.append("# Exemplo de Comparação — Cenário A vs Cenário B (").append(scenario.size()).append(" pontos)\n\n");
    sb.append("Gerado em: ").append(generatedAt).append(" · seed: ").append(seed).append("\n\n");
    sb.append("Cenário fixo e reprodutível (não faz parte da amostragem estatística de 500 ")
      .append("trials — é um exemplo isolado, escolhido entre várias seeds por ser um caso onde ")
      .append("a rota realmente muda, com os mesmos ").append(scenario.size())
      .append(" waypoints usados nas duas simulações).\n\n");

    sb.append("## 1. Waypoints\n\n| # | Latitude | Longitude |\n|---|---|---|\n");
    List<WaypointDto> wps = scenario.waypoints();
    for (int i = 0; i < wps.size(); i++) {
      sb.append(String.format(Locale.US, "| %d | %.6f | %.6f |\n", i, wps.get(i).lat(), wps.get(i).lng()));
    }
    sb.append('\n');

    sb.append("## 2. Ordem escolhida\n\n");
    sb.append("- **Cenário A (só tempo, produção atual):** ").append(formatOrder(orderA)).append('\n');
    sb.append("- **Cenário B (tempo + combustível + desgaste de pneu):** ").append(formatOrder(orderB)).append('\n');
    sb.append("- Rotas diferentes? **").append(routesDiffer ? "Sim" : "Não").append("**\n\n");

    sb.append("## 3. Métricas agregadas de cada rota\n\n");
    sb.append("| Métrica | Rota do Cenário A | Rota do Cenário B |\n|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Distância total | %.2f km | %.2f km |\n",
      TrialResultBuilder.sumAlongPath(scenario.distanceKm(), orderA), TrialResultBuilder.sumAlongPath(scenario.distanceKm(), orderB)));
    sb.append(String.format(Locale.US, "| Duração total | %.1f min | %.1f min |\n",
      TrialResultBuilder.sumAlongPath(costs.durationSec(), orderA) / 60.0, TrialResultBuilder.sumAlongPath(costs.durationSec(), orderB) / 60.0));
    sb.append(String.format(Locale.US, "| Combustível total | %.2f L | %.2f L |\n",
      TrialResultBuilder.sumAlongPath(costs.fuelLiters(), orderA), TrialResultBuilder.sumAlongPath(costs.fuelLiters(), orderB)));
    sb.append(String.format(Locale.US, "| Custo de desgaste de pneu | R$ %.2f | R$ %.2f |\n",
      TrialResultBuilder.sumAlongPath(costs.tireWearReais(), orderA), TrialResultBuilder.sumAlongPath(costs.tireWearReais(), orderB)));
    sb.append(String.format(Locale.US, "| Custo total (fórmula combinada, R$) | R$ %.2f | R$ %.2f |\n",
      costBUnderOrderA, costBUnderOrderB));
    sb.append('\n');
    sb.append(String.format(Locale.US,
      "**Gap:** seguir a rota do Cenário A custaria R$ %.2f a mais (%.2f%%) do que a rota do Cenário B, " +
      "avaliadas as duas sob a mesma fórmula de custo combinada.\n\n",
      gapReais, gapPercent
    ));

    sb.append("## 4. Detalhamento por trecho — Cenário A\n\n").append(edgeTable(scenario, costs, orderA)).append('\n');
    sb.append("## 5. Detalhamento por trecho — Cenário B\n\n").append(edgeTable(scenario, costs, orderB)).append('\n');

    CargoOccupancy occupancy = CargoOccupancy.compute(scenario.cargoWeightKg(), scenario.cargoVolumeM3(), profile);
    sb.append("## 6. Perfil de veículo usado\n\n");
    sb.append(String.format(Locale.US,
      "Eixos: %d · Capacidade: %.0f kg / %.1f m³ · Carga neste cenário: %.0f kg (%.0f%% peso), %.1f m³ (%.0f%% volume) · " +
      "Consumo base: %.1f L/100km · Preço do litro: R$ %.2f · Custo do pneu: R$ %.2f · Vida útil do pneu: %.0f km · " +
      "Custo-hora: R$ %.2f\n",
      profile.axleCount(), profile.capacityKg(), profile.capacityM3(),
      scenario.cargoWeightKg(), occupancy.weightFraction() * 100,
      scenario.cargoVolumeM3(), occupancy.volumeFraction() * 100,
      profile.baseFuelConsumptionLPer100Km(), profile.fuelPricePerLiter(),
      profile.tireReplacementCostPerTire(), profile.tireLifeKm(), profile.driverCostPerHourReais()
    ));

    try {
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, sb.toString());
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write comparison report to " + outputPath, ex);
    }
  }

  private static String formatOrder(List<Integer> order) {
    return order.stream().map(String::valueOf).collect(Collectors.joining(" → "));
  }

  private static String edgeTable(Scenario scenario, EdgeCosts costs, List<Integer> order) {
    StringBuilder sb = new StringBuilder();
    sb.append("| De | Para | Tipo de via | Distância (km) | Duração (min) | Combustível (L) | Desgaste (R$) | Custo do trecho (R$) |\n");
    sb.append("|---|---|---|---|---|---|---|---|\n");
    for (int k = 0; k < order.size() - 1; k++) {
      int i = order.get(k);
      int j = order.get(k + 1);
      sb.append(String.format(Locale.US, "| %d | %d | %s | %.2f | %.1f | %.2f | %.2f | %.2f |\n",
        i, j, scenario.roadType()[i][j],
        scenario.distanceKm()[i][j], costs.durationSec()[i][j] / 60.0,
        costs.fuelLiters()[i][j], costs.tireWearReais()[i][j], costs.costB()[i][j]
      ));
    }
    return sb.toString();
  }
}
