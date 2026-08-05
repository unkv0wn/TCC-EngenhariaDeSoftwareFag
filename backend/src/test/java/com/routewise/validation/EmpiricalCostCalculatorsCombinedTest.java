package com.routewise.validation;

import com.routewise.dto.WaypointDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.routewise.validation.EmpiricalFuelConsumptionCalculator.RefuelingRecord;
import static com.routewise.validation.EmpiricalTireLifeCalculator.AxlePosition.TRACAO;
import static com.routewise.validation.EmpiricalTireLifeCalculator.TireReplacementReason.DESGASTE_NORMAL;
import static com.routewise.validation.EmpiricalTireLifeCalculator.TireReplacementRecord;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link EmpiricalFuelConsumptionCalculator} and {@link EmpiricalTireLifeCalculator}
 * together, feeding both derived values into the same {@link VehicleProfile} and confirming
 * {@link CostMatrixBuilder} reacts to each independently — same "formula doesn't change, only
 * what feeds it" property both design specs rely on
 * ({@code docs/superpowers/specs/2026-08-01-empirical-fuel-consumption-rolling-window-design.md},
 * {@code docs/superpowers/specs/2026-08-01-empirical-tire-wear-by-axle-position-design.md}).
 */
@DisplayName("Empirical fuel + tire calculators, combined")
class EmpiricalCostCalculatorsCombinedTest {

  private static final VehicleProfile ASSUMED = VehicleProfile.medium(); // 15.0 L/100km, 60,000 km tire life

  // Higher-than-assumed fuel consumption, last 5 refuels (window narrower than the log).
  private static final List<RefuelingRecord> FUEL_LOG = List.of(
    new RefuelingRecord("2026-01-08", 150.0, 1200.0), // outside the window
    new RefuelingRecord("2026-01-22", 205.0, 1150.0),
    new RefuelingRecord("2026-02-05", 190.0, 1030.0),
    new RefuelingRecord("2026-02-19", 222.0, 1190.0),
    new RefuelingRecord("2026-03-05", 208.0, 1120.0),
    new RefuelingRecord("2026-03-19", 214.0, 1160.0)
  );

  // Lower-than-assumed tire life (tires wearing out faster than the assumed 60,000 km),
  // last 2 DESGASTE_NORMAL replacements for the TRACAO axle of this vehicle class.
  private static final List<TireReplacementRecord> TIRE_LOG = List.of(
    new TireReplacementRecord(0.0, 58_000.0, DESGASTE_NORMAL, TRACAO), // outside the window
    new TireReplacementRecord(58_000.0, 103_000.0, DESGASTE_NORMAL, TRACAO), // outside the window, 45,000 km
    new TireReplacementRecord(103_000.0, 145_000.0, DESGASTE_NORMAL, TRACAO), // 42,000 km
    new TireReplacementRecord(145_000.0, 193_000.0, DESGASTE_NORMAL, TRACAO)  // 48,000 km
  );

  @Test
  @DisplayName("empirical fuel and tire values both move CostMatrixBuilder's output, independently")
  void bothEmpiricalValues_shiftCostInExpectedDirections() {
    double empiricalConsumption = EmpiricalFuelConsumptionCalculator.consumptionLPer100Km(
      FUEL_LOG, ASSUMED.baseFuelConsumptionLPer100Km()
    );
    double empiricalTireLifeKm = EmpiricalTireLifeCalculator.tireLifeKm(
      TIRE_LOG, ASSUMED.tireLifeKm()
    );

    // Sanity-check the inputs actually diverge from the assumed constants in the directions
    // this test relies on — otherwise the assertions below would pass vacuously.
    assertThat(empiricalConsumption).isGreaterThan(ASSUMED.baseFuelConsumptionLPer100Km());
    assertThat(empiricalTireLifeKm).isLessThan(ASSUMED.tireLifeKm());

    VehicleProfile empirical = new VehicleProfile(
      ASSUMED.label() + " (empírico)", ASSUMED.axleCount(),
      ASSUMED.capacityKg(), ASSUMED.capacityM3(),
      empiricalConsumption, ASSUMED.fuelPricePerLiter(),
      ASSUMED.tireReplacementCostPerTire(), empiricalTireLifeKm,
      ASSUMED.driverCostPerHourReais()
    );

    List<WaypointDto> waypoints = List.of(new WaypointDto(-23.55, -46.63), new WaypointDto(-23.50, -46.60));
    Scenario scenario = new Scenario(
      waypoints, new double[][]{{0, 100}, {100, 0}},
      new RoadType[][]{{RoadType.ARTERIAL, RoadType.ARTERIAL}, {RoadType.ARTERIAL, RoadType.ARTERIAL}},
      2000.0, 12.5 // 50% weight, 50% volume
    );

    EdgeCosts assumedCosts = CostMatrixBuilder.build(scenario, ASSUMED);
    EdgeCosts empiricalCosts = CostMatrixBuilder.build(scenario, empirical);

    // Higher empirical consumption -> more fuel burned over the same edge.
    assertThat(empiricalCosts.fuelLiters()[0][1]).isGreaterThan(assumedCosts.fuelLiters()[0][1]);
    // Shorter empirical tire life -> higher wear cost over the same edge.
    assertThat(empiricalCosts.tireWearReais()[0][1]).isGreaterThan(assumedCosts.tireWearReais()[0][1]);
    // Duration never depends on the vehicle profile, empirical or assumed.
    assertThat(empiricalCosts.durationSec()[0][1]).isEqualTo(assumedCosts.durationSec()[0][1]);
    // Both effects compound into a higher total R$ cost, not just partially.
    assertThat(empiricalCosts.costB()[0][1]).isGreaterThan(assumedCosts.costB()[0][1]);
  }
}
