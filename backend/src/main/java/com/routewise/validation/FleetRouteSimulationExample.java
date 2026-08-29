package com.routewise.validation;

import com.routewise.algorithm.AStarWaypointOptimizer;
import com.routewise.algorithm.HaversineUtil;
import com.routewise.model.RouteMode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Worked example: splitting 9 delivery stops across a 3-vehicle fleet (Van, VUC, Truck),
 * each with its own fuel cost (R$/km), driver cost (R$/hour) and top speed, comparing a
 * scenario where every vehicle is assumed to travel at a flat 60 km/h against one where
 * each vehicle is capped at its own {@code Vl_Max}.
 *
 * <p><strong>Reuses {@link AStarWaypointOptimizer} unmodified — no production code is
 * touched.</strong> This class only adds a fleet layer on top of it:
 * <ol>
 *   <li>The 9 delivery points are split into 3 geographic clusters via an angular sweep
 *       around the depot (P0) — a standard, deterministic VRP clustering heuristic. Cluster
 *       membership is fixed; it does not depend on which vehicle later serves it.</li>
 *   <li>{@link AStarWaypointOptimizer} runs once per cluster (depot + 3 stops, ROUND_TRIP)
 *       to find the visitation order that minimises total distance. Because every stop in a
 *       cluster is served by a single vehicle at a single speed, minimising distance and
 *       minimising duration are the same problem here — so this order does not change
 *       between the two scenarios below, only the cost of driving it does.</li>
 *   <li>The 3 clusters are assigned to the 3 vehicles by brute-forcing all {@code 3! = 6}
 *       permutations and keeping the one with the lowest total fleet cost — small enough to
 *       search exactly, no heuristic needed. This is done independently for each scenario,
 *       which is what lets Cenário 2 hand the Truck's cluster to a faster vehicle instead of
 *       keeping the Cenário 1 assignment.</li>
 * </ol>
 *
 * <p><strong>What is and isn't "real" here:</strong> the coordinates are the ones supplied
 * for this exercise; distance between any two points is Haversine (straight-line) scaled by
 * a fixed circuity factor to approximate real street distance, same simplification every
 * other script in this package uses — no live OSRM call is made from this class. The OSRM
 * validation URLs in the generated report are meant to be opened by hand against the public
 * OSRM API to check the simulated distances against real road geometry.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.FleetRouteSimulationExample}
 */
public final class FleetRouteSimulationExample {

  private static final Path REPORT_PATH =
    Path.of("..", "docs", "validation-reports", "fleet-route-simulation.md");

  /** Straight-line-to-road-distance approximation, same rationale as {@code ToledoRouteComparisonExample}. */
  private static final double CIRCUITY_FACTOR = 1.3;

  private static final double UNIFORM_SPEED_KMH = 60.0;

  record Vehicle(String label, double fuelPerKm, double driverPerHour, double vMaxKmh, String color) {
    double effectiveSpeedKmh(boolean applyVMax) {
      return applyVMax ? Math.min(UNIFORM_SPEED_KMH, vMaxKmh) : UNIFORM_SPEED_KMH;
    }
  }

  static final List<Vehicle> FLEET = List.of(
    new Vehicle("Van (Leve)", 0.80, 25.0, 100.0, "#ff0000"),
    new Vehicle("VUC (Médio)", 1.20, 30.0, 70.0, "#00ff00"),
    new Vehicle("Truck (Grande)", 1.80, 40.0, 50.0, "#0000ff")
  );

  static final List<NamedWaypoint> WAYPOINTS = List.of(
    new NamedWaypoint("P0", -23.5505, -46.6333),
    new NamedWaypoint("P1", -23.5510, -46.6340),
    new NamedWaypoint("P2", -23.5520, -46.6355),
    new NamedWaypoint("P3", -23.5530, -46.6320),
    new NamedWaypoint("P4", -23.5490, -46.6310),
    new NamedWaypoint("P5", -23.5485, -46.6360),
    new NamedWaypoint("P6", -23.5500, -46.6380),
    new NamedWaypoint("P7", -23.5515, -46.6400),
    new NamedWaypoint("P8", -23.5535, -46.6390),
    new NamedWaypoint("P9", -23.5550, -46.6370)
  );

  /** One geographic cluster: the depot-relative indices (into {@link #WAYPOINTS}) it covers. */
  record Cluster(String id, List<Integer> waypointIndices) {}

  /** A cluster after A* has picked its visitation order (list of {@link #WAYPOINTS} indices, depot-to-depot). */
  record RoutedCluster(Cluster cluster, List<Integer> order, double distanceKm) {}

  /** One vehicle assigned to one routed cluster, costed under a specific scenario. */
  record Assignment(
    Vehicle vehicle, RoutedCluster route, double speedKmh,
    double hours, double fuelCostReais, double driverCostReais, double totalCostReais
  ) {}

  public static void main(String[] args) {
    double[][] fullDistanceKm = buildRoadDistanceMatrix();

    List<Cluster> clusters = sweepCluster();
    List<RoutedCluster> routedClusters = clusters.stream()
      .map(c -> routeCluster(c, fullDistanceKm))
      .collect(Collectors.toList());

    List<Assignment> scenario1 = bestAssignment(routedClusters, false);
    List<Assignment> scenario2 = bestAssignment(routedClusters, true);

    String report = buildReport(routedClusters, scenario1, scenario2);

    try {
      Files.createDirectories(REPORT_PATH.getParent());
      Files.writeString(REPORT_PATH, report);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write fleet route simulation report to " + REPORT_PATH, ex);
    }

    System.out.println("Relatório: " + REPORT_PATH.toAbsolutePath().normalize());
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Distance matrix
  // ─────────────────────────────────────────────────────────────────────────

  static double[][] buildRoadDistanceMatrix() {
    int n = WAYPOINTS.size();
    double[][] km = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) continue;
        NamedWaypoint a = WAYPOINTS.get(i);
        NamedWaypoint b = WAYPOINTS.get(j);
        km[i][j] = HaversineUtil.distanceKm(a.lat(), a.lng(), b.lat(), b.lng()) * CIRCUITY_FACTOR;
      }
    }
    return km;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Clustering — angular sweep around the depot (index 0)
  // ─────────────────────────────────────────────────────────────────────────

  static List<Cluster> sweepCluster() {
    List<Integer> deliveryIndices = new ArrayList<>();
    for (int i = 1; i < WAYPOINTS.size(); i++) deliveryIndices.add(i);

    NamedWaypoint depot = WAYPOINTS.get(0);
    deliveryIndices.sort((a, b) -> Double.compare(bearingFromDepot(depot, a), bearingFromDepot(depot, b)));

    List<Cluster> clusters = new ArrayList<>();
    int perCluster = deliveryIndices.size() / 3; // 3
    for (int c = 0; c < 3; c++) {
      List<Integer> slice = new ArrayList<>(deliveryIndices.subList(c * perCluster, (c + 1) * perCluster));
      clusters.add(new Cluster("Cluster " + (char) ('A' + c), slice));
    }
    return clusters;
  }

  private static double bearingFromDepot(NamedWaypoint depot, int idx) {
    NamedWaypoint p = WAYPOINTS.get(idx);
    double dLng = p.lng() - depot.lng();
    double dLat = p.lat() - depot.lat();
    double angle = Math.atan2(dLat, dLng);
    return angle < 0 ? angle + 2 * Math.PI : angle;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Per-cluster route order via A* (reused from production, unmodified)
  // ─────────────────────────────────────────────────────────────────────────

  static RoutedCluster routeCluster(Cluster cluster, double[][] fullDistanceKm) {
    // Local indices: 0 = depot, 1..k = cluster.waypointIndices()
    int k = cluster.waypointIndices().size();
    int[] globalIndex = new int[k + 1];
    globalIndex[0] = 0;
    for (int i = 0; i < k; i++) globalIndex[i + 1] = cluster.waypointIndices().get(i);

    double[][] subMatrix = new double[k + 1][k + 1];
    for (int i = 0; i <= k; i++) {
      for (int j = 0; j <= k; j++) {
        if (i == j) continue;
        subMatrix[i][j] = fullDistanceKm[globalIndex[i]][globalIndex[j]];
      }
    }

    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();
    List<Integer> localOrder = optimizer.optimize(subMatrix, RouteMode.ROUND_TRIP);

    List<Integer> globalOrder = localOrder.stream().map(i -> globalIndex[i]).collect(Collectors.toList());
    double distanceKm = TrialResultBuilder.sumAlongPath(fullDistanceKm, globalOrder);

    return new RoutedCluster(cluster, globalOrder, distanceKm);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Fleet assignment — exact search over all 3! permutations
  // ─────────────────────────────────────────────────────────────────────────

  static List<Assignment> bestAssignment(List<RoutedCluster> routedClusters, boolean applyVMax) {
    List<List<Vehicle>> permutations = permute(FLEET);
    List<Assignment> best = null;
    double bestCost = Double.MAX_VALUE;

    for (List<Vehicle> perm : permutations) {
      List<Assignment> candidate = new ArrayList<>();
      double totalCost = 0;
      for (int i = 0; i < routedClusters.size(); i++) {
        Assignment a = cost(perm.get(i), routedClusters.get(i), applyVMax);
        candidate.add(a);
        totalCost += a.totalCostReais();
      }
      if (totalCost < bestCost) {
        bestCost = totalCost;
        best = candidate;
      }
    }
    return best;
  }

  static Assignment cost(Vehicle vehicle, RoutedCluster route, boolean applyVMax) {
    double speed = vehicle.effectiveSpeedKmh(applyVMax);
    double hours = route.distanceKm() / speed;
    double fuelCost = route.distanceKm() * vehicle.fuelPerKm();
    double driverCost = hours * vehicle.driverPerHour();
    return new Assignment(vehicle, route, speed, hours, fuelCost, driverCost, fuelCost + driverCost);
  }

  static List<List<Vehicle>> permute(List<Vehicle> items) {
    List<List<Vehicle>> result = new ArrayList<>();
    permuteHelper(new ArrayList<>(items), 0, result);
    return result;
  }

  private static void permuteHelper(List<Vehicle> items, int k, List<List<Vehicle>> result) {
    if (k == items.size()) {
      result.add(new ArrayList<>(items));
      return;
    }
    for (int i = k; i < items.size(); i++) {
      java.util.Collections.swap(items, k, i);
      permuteHelper(items, k + 1, result);
      java.util.Collections.swap(items, k, i);
    }
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Report (markdown, tables + OSRM URLs + GeoJSON)
  // ─────────────────────────────────────────────────────────────────────────

  private static String buildReport(
    List<RoutedCluster> routedClusters, List<Assignment> scenario1, List<Assignment> scenario2
  ) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Simulação de frota multi-veículo — Cenário 1 vs Cenário 2\n\n");
    sb.append(String.format(Locale.US, "Gerado em: %s\n\n",
      java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

    sb.append("## 1. Metodologia\n\n");
    sb.append(
      "Os 9 pontos de entrega são divididos em 3 clusters geográficos por varredura angular " +
      "em torno do depósito (P0) — heurística padrão de VRP, determinística. A composição de " +
      "cada cluster não muda entre os cenários. Para cada cluster, `AStarWaypointOptimizer` " +
      "(código de produção, reaproveitado sem alteração) resolve o TSP local — depósito + 3 " +
      "paradas, `ROUND_TRIP` — minimizando distância; como cada cluster é sempre percorrido " +
      "por um único veículo a uma única velocidade, minimizar distância equivale a minimizar " +
      "duração, então a ordem de visita não muda entre os cenários, só o custo de percorrê-la. " +
      "A atribuição dos 3 veículos aos 3 clusters é resolvida por busca exaustiva sobre as " +
      "3! = 6 permutações possíveis, escolhendo a de menor custo total — pequeno o bastante " +
      "para busca exata, sem necessidade de heurística. Distâncias são Haversine (linha reta) " +
      "escaladas por um fator de sinuosidade de " + CIRCUITY_FACTOR + " para aproximar distância " +
      "real de rua — nenhuma chamada real ao OSRM é feita por este script; as URLs na seção 4 " +
      "servem para validar manualmente contra o roteamento real.\n\n"
    );

    sb.append("## 2. Clusters e ordem de visita (A*)\n\n");
    sb.append("| Cluster | Pontos | Ordem de visita | Distância (km) |\n|---|---|---|---|\n");
    for (RoutedCluster rc : routedClusters) {
      sb.append(String.format(Locale.US, "| %s | %s | %s | %.2f |\n",
        rc.cluster().id(),
        rc.cluster().waypointIndices().stream().map(i -> WAYPOINTS.get(i).name()).collect(Collectors.joining(", ")),
        formatOrder(rc.order()),
        rc.distanceKm()
      ));
    }
    sb.append('\n');

    sb.append("## 3. Cenário 1 — velocidade uniforme (60 km/h)\n\n").append(assignmentTable(scenario1)).append('\n');
    sb.append("## 4. Cenário 2 — com Vl_Max por veículo\n\n").append(assignmentTable(scenario2)).append('\n');

    double total1 = scenario1.stream().mapToDouble(Assignment::totalCostReais).sum();
    double total2 = scenario2.stream().mapToDouble(Assignment::totalCostReais).sum();
    sb.append("## 5. Comparação\n\n");
    sb.append("| Métrica | Cenário 1 (sem Vl_Max) | Cenário 2 (com Vl_Max) |\n|---|---|---|\n");
    sb.append(String.format(Locale.US, "| Custo total da frota | R$ %.2f | R$ %.2f |\n", total1, total2));
    for (int i = 0; i < scenario1.size(); i++) {
      sb.append(String.format(Locale.US, "| %s serve | %s | %s |\n",
        "veículo " + (i + 1),
        scenario1.get(i).vehicle().label() + " → " + scenario1.get(i).route().cluster().id(),
        scenario2.get(i).vehicle().label() + " → " + scenario2.get(i).route().cluster().id()
      ));
    }
    boolean assignmentChanged = !sameAssignment(scenario1, scenario2);
    sb.append(String.format(Locale.US,
      "\nA atribuição veículo→cluster %s entre os cenários. Diferença de custo total: R$ %.2f (%.2f%%).\n\n",
      assignmentChanged ? "muda" : "não muda",
      total2 - total1, total1 > 0 ? 100.0 * (total2 - total1) / total1 : 0.0
    ));

    sb.append("## 6. Validação OSRM (URLs reais, clicáveis)\n\n");
    sb.append("Cenário 1:\n\n").append(osrmUrls(scenario1)).append('\n');
    sb.append("Cenário 2:\n\n").append(osrmUrls(scenario2)).append('\n');

    sb.append("## 7. GeoJSON simulado — Cenário 1\n\n```json\n")
      .append(geoJson(scenario1)).append("\n```\n\n");
    sb.append("## 8. GeoJSON simulado — Cenário 2\n\n```json\n")
      .append(geoJson(scenario2)).append("\n```\n");

    return sb.toString();
  }

  private static boolean sameAssignment(List<Assignment> a, List<Assignment> b) {
    for (int i = 0; i < a.size(); i++) {
      if (!a.get(i).vehicle().label().equals(b.get(i).vehicle().label())
        || !a.get(i).route().cluster().id().equals(b.get(i).route().cluster().id())) {
        return false;
      }
    }
    return true;
  }

  private static String formatOrder(List<Integer> order) {
    return order.stream().map(i -> WAYPOINTS.get(i).name()).collect(Collectors.joining(" → "));
  }

  private static String assignmentTable(List<Assignment> assignments) {
    StringBuilder sb = new StringBuilder();
    sb.append("| Veículo | Cluster | Ordem | Distância (km) | Velocidade (km/h) | Tempo (h) | Combustível (R$) | Motorista (R$) | Total (R$) |\n");
    sb.append("|---|---|---|---|---|---|---|---|---|\n");
    for (Assignment a : assignments) {
      sb.append(String.format(Locale.US, "| %s | %s | %s | %.2f | %.0f | %.2f | R$ %.2f | R$ %.2f | R$ %.2f |\n",
        a.vehicle().label(), a.route().cluster().id(), formatOrder(a.route().order()),
        a.route().distanceKm(), a.speedKmh(), a.hours(), a.fuelCostReais(), a.driverCostReais(), a.totalCostReais()
      ));
    }
    double total = assignments.stream().mapToDouble(Assignment::totalCostReais).sum();
    sb.append(String.format(Locale.US, "\n**Custo total da frota: R$ %.2f**\n", total));
    return sb.toString();
  }

  private static String osrmUrls(List<Assignment> assignments) {
    StringBuilder sb = new StringBuilder();
    for (Assignment a : assignments) {
      String coords = a.route().order().stream()
        .map(i -> WAYPOINTS.get(i))
        .map(w -> String.format(Locale.US, "%.4f,%.4f", w.lng(), w.lat()))
        .collect(Collectors.joining(";"));
      sb.append(String.format(Locale.US, "- **%s** (%s): http://router.project-osrm.org/route/v1/driving/%s?geometries=geojson&annotations=true\n",
        a.vehicle().label(), a.route().cluster().id(), coords));
    }
    return sb.toString();
  }

  private static String geoJson(List<Assignment> assignments) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n");
    for (int i = 0; i < assignments.size(); i++) {
      Assignment a = assignments.get(i);
      sb.append("    {\n      \"type\": \"Feature\",\n      \"properties\": {\n");
      sb.append(String.format(Locale.US, "        \"vehicle\": \"%s\",\n", a.vehicle().label()));
      sb.append(String.format(Locale.US, "        \"cluster\": \"%s\",\n", a.route().cluster().id()));
      sb.append(String.format(Locale.US, "        \"stroke\": \"%s\",\n", a.vehicle().color()));
      sb.append("        \"stroke-width\": 4,\n");
      sb.append(String.format(Locale.US, "        \"distance_km\": %.2f,\n", a.route().distanceKm()));
      sb.append(String.format(Locale.US, "        \"total_cost_reais\": %.2f\n", a.totalCostReais()));
      sb.append("      },\n      \"geometry\": {\n        \"type\": \"LineString\",\n        \"coordinates\": [\n");
      List<String> coordLines = a.route().order().stream()
        .map(idx -> WAYPOINTS.get(idx))
        .map(w -> String.format(Locale.US, "          [%.4f, %.4f]", w.lng(), w.lat()))
        .collect(Collectors.toList());
      sb.append(String.join(",\n", coordLines));
      sb.append("\n        ]\n      }\n    }");
      sb.append(i < assignments.size() - 1 ? ",\n" : "\n");
    }
    sb.append("  ]\n}");
    return sb.toString();
  }
}
