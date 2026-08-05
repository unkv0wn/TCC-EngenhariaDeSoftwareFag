package com.routewise.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link CargoOccupancy} — the "peso x cubagem" logic that decides
 * which of a load's weight (kg) or volume (m³) is the binding constraint against a
 * {@link VehicleProfile}'s capacity, and whether the load is feasible at all.
 */
@DisplayName("CargoOccupancy")
class CargoOccupancyTest {

  // Medium truck (Toco): 4000 kg / 25 m3 capacity.
  private static final VehicleProfile MEDIUM = VehicleProfile.medium();

  @Test
  @DisplayName("dense, low-volume cargo is weight-bound")
  void denseCargo_isWeightBound() {
    // 3600 kg (90% of 4000 kg) in only 5 m3 (20% of 25 m3) — e.g. steel bars.
    CargoOccupancy occupancy = CargoOccupancy.compute(3600.0, 5.0, MEDIUM);

    assertThat(occupancy.weightFraction()).isCloseTo(0.90, within(1e-9));
    assertThat(occupancy.volumeFraction()).isCloseTo(0.20, within(1e-9));
    assertThat(occupancy.volumeBound()).isFalse();
    assertThat(occupancy.effectiveLoadFactor()).isCloseTo(0.90, within(1e-9));
    assertThat(occupancy.feasible()).isTrue();
  }

  @Test
  @DisplayName("light, bulky cargo is volume-bound")
  void bulkyCargo_isVolumeBound() {
    // 800 kg (20% of 4000 kg) but 22.5 m3 (90% of 25 m3) — e.g. packing foam.
    CargoOccupancy occupancy = CargoOccupancy.compute(800.0, 22.5, MEDIUM);

    assertThat(occupancy.weightFraction()).isCloseTo(0.20, within(1e-9));
    assertThat(occupancy.volumeFraction()).isCloseTo(0.90, within(1e-9));
    assertThat(occupancy.volumeBound()).isTrue();
    assertThat(occupancy.effectiveLoadFactor()).isCloseTo(0.90, within(1e-9));
    assertThat(occupancy.feasible()).isTrue();
  }

  @Test
  @DisplayName("cargo exceeding weight capacity is infeasible even if volume fits")
  void overWeightCapacity_isInfeasible() {
    // 5000 kg is 125% of the 4000 kg limit; volume (10 m3 of 25 m3) is well within capacity.
    CargoOccupancy occupancy = CargoOccupancy.compute(5000.0, 10.0, MEDIUM);

    assertThat(occupancy.weightFraction()).isCloseTo(1.25, within(1e-9));
    assertThat(occupancy.feasible()).isFalse();
    // The cost-formula input is clamped to 1.0 even though the raw fraction is 1.25 —
    // the fuel/tire-wear formulas are only calibrated for the [0, 1] range.
    assertThat(occupancy.effectiveLoadFactor()).isCloseTo(1.0, within(1e-9));
  }

  @Test
  @DisplayName("cargo exceeding volume capacity is infeasible even if weight fits")
  void overVolumeCapacity_isInfeasible() {
    // 30 m3 is 120% of the 25 m3 limit; weight (500 kg of 4000 kg) is well within capacity.
    CargoOccupancy occupancy = CargoOccupancy.compute(500.0, 30.0, MEDIUM);

    assertThat(occupancy.volumeFraction()).isCloseTo(1.20, within(1e-9));
    assertThat(occupancy.volumeBound()).isTrue();
    assertThat(occupancy.feasible()).isFalse();
    assertThat(occupancy.effectiveLoadFactor()).isCloseTo(1.0, within(1e-9));
  }

  @Test
  @DisplayName("empty cargo is fully feasible with zero occupancy")
  void emptyCargo_isFeasible() {
    CargoOccupancy occupancy = CargoOccupancy.compute(0.0, 0.0, MEDIUM);

    assertThat(occupancy.weightFraction()).isZero();
    assertThat(occupancy.volumeFraction()).isZero();
    assertThat(occupancy.effectiveLoadFactor()).isZero();
    assertThat(occupancy.feasible()).isTrue();
  }

  @Test
  @DisplayName("CostMatrixBuilder produces higher fuel and tire-wear cost for a fuller load")
  void costMatrixBuilder_higherOccupancyRaisesFuelAndWearCost() {
    java.util.List<com.routewise.dto.WaypointDto> waypoints = java.util.List.of(
      new com.routewise.dto.WaypointDto(-23.55, -46.63), new com.routewise.dto.WaypointDto(-23.50, -46.60)
    );
    Scenario lightLoad = new Scenario(
      waypoints, new double[][]{{0, 100}, {100, 0}},
      new RoadType[][]{{RoadType.ARTERIAL, RoadType.ARTERIAL}, {RoadType.ARTERIAL, RoadType.ARTERIAL}},
      400.0, 2.5 // 10% weight, 10% volume
    );
    Scenario heavyLoad = new Scenario(
      waypoints, new double[][]{{0, 100}, {100, 0}},
      new RoadType[][]{{RoadType.ARTERIAL, RoadType.ARTERIAL}, {RoadType.ARTERIAL, RoadType.ARTERIAL}},
      3800.0, 23.75 // 95% weight, 95% volume
    );

    EdgeCosts lightCosts = CostMatrixBuilder.build(lightLoad, MEDIUM);
    EdgeCosts heavyCosts = CostMatrixBuilder.build(heavyLoad, MEDIUM);

    assertThat(heavyCosts.fuelLiters()[0][1]).isGreaterThan(lightCosts.fuelLiters()[0][1]);
    assertThat(heavyCosts.tireWearReais()[0][1]).isGreaterThan(lightCosts.tireWearReais()[0][1]);
    // Duration (Cenário A / production behaviour today) must not depend on cargo at all.
    assertThat(heavyCosts.durationSec()[0][1]).isCloseTo(lightCosts.durationSec()[0][1], within(1e-9));
  }
}
