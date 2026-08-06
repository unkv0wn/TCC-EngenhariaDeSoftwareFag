package com.routewise.validation;

import com.routewise.dto.WaypointDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for the per-axle-position tire wear model — {@link AxlePosition},
 * {@link AxleLayout}, and the summation {@link CostMatrixBuilder} performs over them.
 *
 * <p>Replaces the earlier flat model, where one averaged tire life was multiplied by
 * {@code axleCount}. Both halves of that were wrong: every axle wore at the same rate,
 * and axle count stood in for tire count.
 */
@DisplayName("Per-axle tire wear")
class AxleTireWearTest {

  private static final double TOLERANCE = 1e-9;

  /** Fixed 100 km ARTERIAL edge — wearMultiplier 1.0, so road type drops out of the math. */
  private static final double EDGE_KM = 100.0;

  private static Scenario edgeScenario(double cargoWeightKg, double cargoVolumeM3) {
    return new Scenario(
      List.of(new WaypointDto(-23.55, -46.63), new WaypointDto(-23.50, -46.60)),
      new double[][] {{0, EDGE_KM}, {EDGE_KM, 0}},
      new RoadType[][] {{RoadType.ARTERIAL, RoadType.ARTERIAL}, {RoadType.ARTERIAL, RoadType.ARTERIAL}},
      cargoWeightKg,
      cargoVolumeM3
    );
  }

  @Test
  @DisplayName("tire counts follow standard Brazilian axle configurations")
  void tireCounts_matchVehicleClass() {
    assertThat(VehicleProfile.light().axleLayout().totalTires()).isEqualTo(6);
    assertThat(VehicleProfile.medium().axleLayout().totalTires()).isEqualTo(10);
    assertThat(VehicleProfile.heavy().axleLayout().totalTires()).isEqualTo(18);
  }

  @Test
  @DisplayName("only the articulated heavy profile has trailer tires")
  void trailerTires_onlyOnHeavyProfile() {
    assertThat(VehicleProfile.light().axleLayout().tireCount(AxlePosition.REBOQUE)).isZero();
    assertThat(VehicleProfile.medium().axleLayout().tireCount(AxlePosition.REBOQUE)).isZero();
    assertThat(VehicleProfile.heavy().axleLayout().tireCount(AxlePosition.REBOQUE)).isEqualTo(8);
  }

  @Test
  @DisplayName("wear per km sums each position's own tire life")
  void wearPerKm_sumsPerPosition() {
    // Medium: 60,000 km rated life, 2 steering + 8 drive tires.
    //   2 / (60000 * 0.85) + 8 / (60000 * 1.00)
    double expected = 2 / (60_000.0 * 0.85) + 8 / 60_000.0;

    assertThat(CostMatrixBuilder.baseTireWearPerKm(VehicleProfile.medium()))
      .isCloseTo(expected, within(TOLERANCE));
  }

  @Test
  @DisplayName("trailer tires wear slower, so they add less per tire than drive tires")
  void trailerTires_wearSlowerThanDriveTires() {
    VehicleProfile heavy = VehicleProfile.heavy();
    // Same count (8) at both positions, so any difference is the life factor alone.
    double drivePerTire = 1 / (heavy.tireLifeKm() * AxlePosition.TRACAO.lifeFactor);
    double trailerPerTire = 1 / (heavy.tireLifeKm() * AxlePosition.REBOQUE.lifeFactor);

    assertThat(trailerPerTire).isLessThan(drivePerTire);
  }

  @Test
  @DisplayName("steering tires wear fastest per tire")
  void steeringTires_wearFastest() {
    assertThat(AxlePosition.DIANTEIRO.lifeFactor)
      .isLessThan(AxlePosition.TRACAO.lifeFactor)
      .isLessThan(AxlePosition.REBOQUE.lifeFactor);
  }

  @Test
  @DisplayName("edge tire cost equals wear per km x distance x price per tire")
  void edgeTireCost_matchesFormula() {
    VehicleProfile medium = VehicleProfile.medium();
    // Empty load — isolates the axle model from the load adjustment.
    EdgeCosts costs = CostMatrixBuilder.build(edgeScenario(0.0, 0.0), medium);

    double expected =
      EDGE_KM * CostMatrixBuilder.baseTireWearPerKm(medium) * medium.tireReplacementCostPerTire();

    assertThat(costs.tireWearReais()[0][1]).isCloseTo(expected, within(TOLERANCE));
  }

  @Test
  @DisplayName("a full load raises tire wear by the 50% load factor")
  void fullLoad_raisesWearByLoadFactor() {
    VehicleProfile medium = VehicleProfile.medium();
    double empty = CostMatrixBuilder.build(edgeScenario(0.0, 0.0), medium).tireWearReais()[0][1];
    // 4000 kg is exactly the medium truck's rated capacity -> loadFactor 1.0.
    double full = CostMatrixBuilder.build(edgeScenario(4000.0, 0.0), medium).tireWearReais()[0][1];

    assertThat(full).isCloseTo(empty * 1.5, within(TOLERANCE));
  }

  @Test
  @DisplayName("axle count is no longer a stand-in for tire count")
  void axleCount_isNotTireCount() {
    // The regression this model fixes: the old formula multiplied by axleCount (3),
    // where the truck actually rolls on 10 tires — understating wear ~3x.
    VehicleProfile medium = VehicleProfile.medium();

    assertThat(medium.axleLayout().totalTires()).isGreaterThan(medium.axleCount());
    assertThat(CostMatrixBuilder.baseTireWearPerKm(medium))
      .isGreaterThan(medium.axleCount() / medium.tireLifeKm());
  }
}
