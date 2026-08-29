package com.routewise.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simulates 3 vehicle profiles operating over 3 route sizes with randomised
 * empirical inputs (cargo weight/volume, per-edge road type), exercising {@link
 * CostMatrixBuilder} end to end rather than unit-testing its pieces in isolation
 * (that's {@link AxleTireWearTest}, {@link CargoOccupancyTest}, and friends).
 *
 * <p>Scenario generation reuses {@link ScenarioGenerator} — the same synthetic-data
 * source the Monte Carlo experiments in this package already use — with a fixed
 * seed so the test is reproducible instead of flaky.
 */
@DisplayName("Empirical cost simulation — 3 vehicles x 3 route sizes")
class EmpiricalCostSimulationTest {

  private static final long SEED = 20260807L;
  private static final int[] ROUTE_SIZES = {4, 6, 8};
  private static final List<VehicleProfile> PROFILES =
    List.of(VehicleProfile.light(), VehicleProfile.medium(), VehicleProfile.heavy());

  @Test
  @DisplayName("costA and costB are strictly positive for every edge, profile, and route size")
  void costs_areAlwaysPositive_acrossProfilesAndRouteSizes() {
    Random rng = new Random(SEED);
    ScenarioGenerator generator = new ScenarioGenerator(rng);

    for (int n : ROUTE_SIZES) {
      Scenario scenario = generator.generate(n);

      for (VehicleProfile profile : PROFILES) {
        EdgeCosts costs = CostMatrixBuilder.build(scenario, profile);

        for (int i = 0; i < n; i++) {
          for (int j = 0; j < n; j++) {
            if (i == j) {
              continue;
            }
            assertThat(costs.costA()[i][j])
              .as("costA[%d][%d], n=%d, profile=%s", i, j, n, profile.label())
              .isGreaterThan(0.0);
            assertThat(costs.costB()[i][j])
              .as("costB[%d][%d], n=%d, profile=%s", i, j, n, profile.label())
              .isGreaterThan(0.0);
          }
        }
      }
    }
  }

  @Test
  @DisplayName("heavy() costs strictly more than light() on the same distance matrix, even at light()'s own max load")
  void heavyProfile_costsMoreThanLight_sameScenarioMaxLoad() {
    Random rng = new Random(SEED);
    Scenario base = new ScenarioGenerator(rng).generate(6);

    // Same waypoints, distances, and per-edge road types for both profiles — only
    // the vehicle changes. Cargo is fixed at light()'s own capacity: light() is
    // fully loaded (loadFactor = 1.0, the load-dependent cost terms at their
    // maximum), while the same physical shipment leaves heavy() mostly empty
    // (loadFactor = 1500/12000 = 0.125). This is deliberately the least
    // favourable case for the assertion — light() carrying its maximum multiplier
    // against heavy() carrying almost none — so a pass here isn't a coincidence
    // of a lucky random draw.
    Scenario maxLoadForLight = new Scenario(
      base.waypoints(), base.distanceKm(), base.roadType(),
      VehicleProfile.light().capacityKg(), VehicleProfile.light().capacityM3()
    );
    List<Integer> route = IntStream.range(0, maxLoadForLight.size()).boxed().toList();

    double lightTotal = TrialResultBuilder.sumAlongPath(
      CostMatrixBuilder.build(maxLoadForLight, VehicleProfile.light()).costB(), route);
    double heavyTotal = TrialResultBuilder.sumAlongPath(
      CostMatrixBuilder.build(maxLoadForLight, VehicleProfile.heavy()).costB(), route);

    assertThat(heavyTotal)
      .as("heavy() should cost more per trip than light() even fully loaded, since heavy()'s base fuel "
        + "consumption, driver cost, and tire replacement cost are all higher on their own")
      .isGreaterThan(lightTotal);
  }
}
