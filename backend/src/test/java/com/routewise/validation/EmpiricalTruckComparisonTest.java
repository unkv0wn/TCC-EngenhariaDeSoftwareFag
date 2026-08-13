package com.routewise.validation;

import com.routewise.algorithm.HaversineUtil;
import com.routewise.dto.WaypointDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Compares tire wear, travel time, average fuel consumption, and max load capacity
 * across {@link VehicleProfile#light()}, {@link VehicleProfile#medium()}, and
 * {@link VehicleProfile#heavy()} on the same 15-waypoint route — same distance
 * matrix, per-edge road types, and cargo for all three, so only the vehicle changes
 * and the comparison is apples-to-apples (route choice doesn't confound it).
 *
 * <p>15 waypoints — larger than the production cap of 10 (see {@code
 * RouteRequestDto}) — matches the scale settled on for {@link
 * AStarTieBreakBenchmarkTest}'s sweep; reused here for the same reason: it's a
 * meaningfully sized route without being expensive to compute.
 *
 * <p>Same deliberate exception to this codebase's assert-only test convention as
 * {@link AStarTieBreakBenchmarkTest}: this test also writes {@code
 * docs/refactor-math-result/comparacao_15.md} as a side effect, on every run — see
 * that class's Javadoc for the full rationale and caveat (numbers reflect whichever
 * seed/machine last ran the suite, not a fixed ground truth; move to a {@code
 * main()} script if that's undesirable).
 */
@DisplayName("Empirical truck comparison — tire wear, time, fuel, capacity across 3 profiles")
class EmpiricalTruckComparisonTest {

  private static final long SEED = 20260807L;
  private static final int WAYPOINT_COUNT = 15;
  private static final double DURATION_TOLERANCE = 1e-9;

  private record TruckMetrics(
    String label,
    double capacityKg,
    double durationMin,
    double fuelLiters,
    double avgFuelLPer100Km,
    double tireWearReais
  ) {}

  @Test
  @DisplayName("time is identical across profiles on the same route; capacity, fuel, and tire wear grow with truck class")
  void compareThreeTrucks_onSameFifteenWaypointRoute() {
    Scenario scenario = generateScenario(WAYPOINT_COUNT, SEED);
    List<Integer> route = IntStream.range(0, WAYPOINT_COUNT).boxed().toList();
    double totalDistanceKm = TrialResultBuilder.sumAlongPath(scenario.distanceKm(), route);

    TruckMetrics light = metricsFor(VehicleProfile.light(), scenario, route, totalDistanceKm);
    TruckMetrics medium = metricsFor(VehicleProfile.medium(), scenario, route, totalDistanceKm);
    TruckMetrics heavy = metricsFor(VehicleProfile.heavy(), scenario, route, totalDistanceKm);

    // Travel time depends only on distance and road type (RoadType.avgSpeedKmh) —
    // CostMatrixBuilder never factors the vehicle profile into durationSec — so all
    // three trucks take exactly as long on the same route.
    assertThat(medium.durationMin)
      .as("medium() duration should match light() exactly — duration doesn't depend on vehicle")
      .isCloseTo(light.durationMin, within(DURATION_TOLERANCE));
    assertThat(heavy.durationMin)
      .as("heavy() duration should match light() exactly — duration doesn't depend on vehicle")
      .isCloseTo(light.durationMin, within(DURATION_TOLERANCE));

    // Max load capacity is a static profile fact: always light < medium < heavy.
    assertThat(light.capacityKg).isLessThan(medium.capacityKg);
    assertThat(medium.capacityKg).isLessThan(heavy.capacityKg);

    // heavy()'s base fuel consumption (32 L/100km) and per-tire replacement cost
    // (R$2200) so far exceed light()'s (10 L/100km, R$900) that heavy() costs more on
    // both dimensions even at the same load — see EmpiricalCostSimulationTest for the
    // load-factor analysis behind why this holds regardless of how loaded each truck is.
    assertThat(heavy.fuelLiters).isGreaterThan(light.fuelLiters);
    assertThat(heavy.avgFuelLPer100Km).isGreaterThan(light.avgFuelLPer100Km);
    assertThat(heavy.tireWearReais).isGreaterThan(light.tireWearReais);

    // Every component must be physically meaningful — never zero or negative.
    for (TruckMetrics truck : List.of(light, medium, heavy)) {
      assertThat(truck.durationMin).as("%s duration", truck.label).isGreaterThan(0.0);
      assertThat(truck.fuelLiters).as("%s fuel liters", truck.label).isGreaterThan(0.0);
      assertThat(truck.avgFuelLPer100Km).as("%s avg fuel rate", truck.label).isGreaterThan(0.0);
      assertThat(truck.tireWearReais).as("%s tire wear", truck.label).isGreaterThan(0.0);
    }

    writeReport(scenario, totalDistanceKm, List.of(light, medium, heavy));
  }

  /**
   * Generates a scenario the same way {@link ScenarioGenerator} does — waypoints
   * scattered around São Paulo, Haversine distances, random per-edge road type,
   * random cargo within its usual bounds — but without {@code ScenarioGenerator}'s
   * {@code n <= 10} guard, which is intentionally tied to the production waypoint
   * cap and shared by other experiments in this package. 15 waypoints only needs
   * to exceed that cap here; it shouldn't loosen it for everything else that calls
   * {@code ScenarioGenerator}.
   */
  private Scenario generateScenario(int n, long seed) {
    Random rng = new Random(seed);
    double centerLat = -23.5505;
    double centerLng = -46.6333;
    double spreadDegrees = 0.35;

    List<WaypointDto> waypoints = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      double lat = centerLat + (rng.nextDouble() * 2 - 1) * spreadDegrees;
      double lng = centerLng + (rng.nextDouble() * 2 - 1) * spreadDegrees;
      waypoints.add(new WaypointDto(lat, lng));
    }

    double[][] distanceKm = new double[n][n];
    RoadType[][] roadType = new RoadType[n][n];
    RoadType[] roadTypes = RoadType.values();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          distanceKm[i][j] = 0.0;
          roadType[i][j] = RoadType.ARTERIAL;
          continue;
        }
        WaypointDto a = waypoints.get(i);
        WaypointDto b = waypoints.get(j);
        distanceKm[i][j] = HaversineUtil.distanceKm(a.lat(), a.lng(), b.lat(), b.lng());
        roadType[i][j] = roadTypes[rng.nextInt(roadTypes.length)];
      }
    }

    double cargoWeightKg = 50.0 + rng.nextDouble() * (4_500.0 - 50.0);
    double cargoVolumeM3 = 0.3 + rng.nextDouble() * (28.0 - 0.3);

    return new Scenario(waypoints, distanceKm, roadType, cargoWeightKg, cargoVolumeM3);
  }

  private TruckMetrics metricsFor(
    VehicleProfile profile, Scenario scenario, List<Integer> route, double totalDistanceKm
  ) {
    EdgeCosts costs = CostMatrixBuilder.build(scenario, profile);
    double durationMin = TrialResultBuilder.sumAlongPath(costs.durationSec(), route) / 60.0;
    double fuelLiters = TrialResultBuilder.sumAlongPath(costs.fuelLiters(), route);
    double avgFuelLPer100Km = fuelLiters / totalDistanceKm * 100.0;
    double tireWearReais = TrialResultBuilder.sumAlongPath(costs.tireWearReais(), route);

    return new TruckMetrics(
      profile.label(), profile.capacityKg(), durationMin, fuelLiters, avgFuelLPer100Km, tireWearReais
    );
  }

  private void writeReport(Scenario scenario, double totalDistanceKm, List<TruckMetrics> trucks) {
    StringBuilder sb = new StringBuilder();
    sb.append("# Comparação entre 3 Caminhões — 15 Waypoints\n\n");
    sb.append(MarkdownReportFile.generatedAt());

    sb.append("## Contexto\n\n");
    sb.append(String.format(Locale.US,
      "`light()`, `medium()` e `heavy()` ([`VehicleProfile`](../../backend/src/main/java/" +
      "com/routewise/validation/VehicleProfile.java)) comparados na **mesma rota** de %d " +
      "waypoints — mesma matriz de distância, mesmos tipos de via por aresta, mesma carga " +
      "para os três, só o veículo muda. Rota sequencial `0→1→...→%d` (não a ótima de cada " +
      "perfil), justamente para isolar o efeito do veículo do efeito da escolha de rota. " +
      "Seed fixa %d, distância total percorrida %.1f km, carga do cenário %.0f kg / %.1f m³.\n\n",
      WAYPOINT_COUNT, WAYPOINT_COUNT - 1, SEED, totalDistanceKm,
      scenario.cargoWeightKg(), scenario.cargoVolumeM3()
    ));

    sb.append("## Resultado\n\n");
    sb.append("| Métrica | Leve (2 eixos) | Médio (3 eixos) | Pesado (5 eixos) |\n");
    sb.append("|---|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Peso máximo (kg) | %.0f | %.0f | %.0f |\n",
      trucks.get(0).capacityKg, trucks.get(1).capacityKg, trucks.get(2).capacityKg));
    sb.append(String.format(Locale.US, "| Tempo de viagem (min) | %.1f | %.1f | %.1f |\n",
      trucks.get(0).durationMin, trucks.get(1).durationMin, trucks.get(2).durationMin));
    sb.append(String.format(Locale.US, "| Combustível total (L) | %.2f | %.2f | %.2f |\n",
      trucks.get(0).fuelLiters, trucks.get(1).fuelLiters, trucks.get(2).fuelLiters));
    sb.append(String.format(Locale.US, "| Média de combustível (L/100km) | %.2f | %.2f | %.2f |\n",
      trucks.get(0).avgFuelLPer100Km, trucks.get(1).avgFuelLPer100Km, trucks.get(2).avgFuelLPer100Km));
    sb.append(String.format(Locale.US, "| Desgaste de pneu (R$) | %.2f | %.2f | %.2f |\n",
      trucks.get(0).tireWearReais, trucks.get(1).tireWearReais, trucks.get(2).tireWearReais));

    sb.append(String.format(Locale.US,
      "\nO tempo de viagem é **idêntico** entre os três (%.1f min) — `durationSec` em " +
      "`CostMatrixBuilder` depende só de distância e tipo de via (`RoadType.avgSpeedKmh`), " +
      "nunca do perfil do veículo, então nenhum caminhão \"anda mais rápido\" que o outro " +
      "na mesma via. Peso máximo é um dado estático do perfil (`VehicleProfile.capacityKg`), " +
      "não depende da rota. Combustível e desgaste de pneu crescem de leve para pesado: o " +
      "consumo-base do pesado (32 L/100km) e o custo de troca por pneu (R$2.200) são tão " +
      "maiores que os do leve (10 L/100km, R$900) que essa ordem se mantém mesmo quando o " +
      "leve está no seu próprio limite de carga e o pesado está com folga — ver " +
      "`EmpiricalCostSimulationTest` para a análise completa dessa robustez a `loadFactor`.\n",
      trucks.get(0).durationMin
    ));

    MarkdownReportFile.write(
      Path.of("..", "docs", "refactor-math-result", "comparacao_15.md"), sb.toString()
    );
  }
}
