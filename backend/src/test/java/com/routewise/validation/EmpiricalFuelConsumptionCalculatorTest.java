package com.routewise.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.routewise.validation.EmpiricalFuelConsumptionCalculator.RefuelingRecord;
import static com.routewise.validation.EmpiricalFuelConsumptionCalculator.consumptionLPer100Km;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link EmpiricalFuelConsumptionCalculator} — see
 * {@code docs/superpowers/specs/2026-08-01-empirical-fuel-consumption-rolling-window-design.md}
 * for the rolling-window + cold-start policy these tests verify.
 */
@DisplayName("EmpiricalFuelConsumptionCalculator")
class EmpiricalFuelConsumptionCalculatorTest {

  private static final double FALLBACK = 15.0; // VehicleProfile.medium()'s assumed constant

  // Same synthetic log as FuelConsumptionFromRefuelingExample, oldest first.
  private static final List<RefuelingRecord> FULL_LOG = List.of(
    new RefuelingRecord("2026-01-08", 215.0, 1200.0),
    new RefuelingRecord("2026-01-22", 198.0, 1080.0),
    new RefuelingRecord("2026-02-05", 230.0, 1250.0),
    new RefuelingRecord("2026-02-19", 205.0, 1150.0), // first record inside the last-5 window
    new RefuelingRecord("2026-03-05", 190.0, 1030.0),
    new RefuelingRecord("2026-03-19", 222.0, 1190.0),
    new RefuelingRecord("2026-04-02", 208.0, 1120.0),
    new RefuelingRecord("2026-04-16", 214.0, 1160.0)
  );

  @Test
  @DisplayName("empty log falls back to the assumed constant (cold start)")
  void emptyLog_usesFallback() {
    assertThat(consumptionLPer100Km(List.of(), FALLBACK)).isEqualTo(FALLBACK);
  }

  @Test
  @DisplayName("log narrower than the window uses whichever records exist (partial window)")
  void partialLog_usesAvailableRecords() {
    List<RefuelingRecord> firstThree = FULL_LOG.subList(0, 3);
    // (215 + 198 + 230) / (1200 + 1080 + 1250) * 100
    double expected = 643.0 / 3530.0 * 100.0;

    assertThat(consumptionLPer100Km(firstThree, FALLBACK)).isCloseTo(expected, within(1e-9));
  }

  @Test
  @DisplayName("log exactly the window size uses all of it")
  void exactWindowLog_usesAllRecords() {
    List<RefuelingRecord> fiveRecords = FULL_LOG.subList(3, 8);
    // (205 + 190 + 222 + 208 + 214) / (1150 + 1030 + 1190 + 1120 + 1160) * 100
    double expected = 1039.0 / 5650.0 * 100.0;

    assertThat(consumptionLPer100Km(fiveRecords, FALLBACK)).isCloseTo(expected, within(1e-9));
  }

  @Test
  @DisplayName("log longer than the window only considers the last 5 records")
  void longLog_ignoresRecordsOlderThanWindow() {
    double expected = 1039.0 / 5650.0 * 100.0; // same as exactWindowLog_usesAllRecords

    assertThat(consumptionLPer100Km(FULL_LOG, FALLBACK)).isCloseTo(expected, within(1e-9));
  }

  @Test
  @DisplayName("a record older than the window does not influence the result")
  void recordOutsideWindow_hasNoEffect() {
    List<RefuelingRecord> withOutlierAtStart = List.of(
      new RefuelingRecord("2025-01-01", 10_000.0, 1.0), // absurd ratio, but outside the window
      FULL_LOG.get(3), FULL_LOG.get(4), FULL_LOG.get(5), FULL_LOG.get(6), FULL_LOG.get(7)
    );
    List<RefuelingRecord> withoutOutlier = FULL_LOG.subList(3, 8);

    assertThat(consumptionLPer100Km(withOutlierAtStart, FALLBACK))
      .isCloseTo(consumptionLPer100Km(withoutOutlier, FALLBACK), within(1e-9));
  }

  @Test
  @DisplayName("the most recent record shifts the window average, unlike one dropped from the tail")
  void recentRecord_changesResult() {
    List<RefuelingRecord> withNewOutlierAtEnd = List.of(
      FULL_LOG.get(3), FULL_LOG.get(4), FULL_LOG.get(5), FULL_LOG.get(6), FULL_LOG.get(7),
      new RefuelingRecord("2026-05-01", 10_000.0, 1.0) // absurd ratio, now inside the window
    );

    assertThat(consumptionLPer100Km(withNewOutlierAtEnd, FALLBACK))
      .isNotCloseTo(consumptionLPer100Km(FULL_LOG.subList(3, 8), FALLBACK), within(1.0));
  }

  @Test
  @DisplayName("weights by km covered, not a simple average of each tank's ratio")
  void weightsByDistance_notSimpleAverageOfRatios() {
    // Tank A: 10 L/100km over 1000 km. Tank B: 30 L/100km over only 100 km.
    RefuelingRecord highDistanceTank = new RefuelingRecord("2026-01-01", 100.0, 1000.0);
    RefuelingRecord lowDistanceTank = new RefuelingRecord("2026-01-15", 30.0, 100.0);
    List<RefuelingRecord> log = List.of(highDistanceTank, lowDistanceTank);

    double simpleAverageOfRatios = (highDistanceTank.consumptionLPer100Km() + lowDistanceTank.consumptionLPer100Km()) / 2.0; // 20.0
    double weighted = consumptionLPer100Km(log, FALLBACK);

    // (100 + 30) / (1000 + 100) * 100 = 11.818..., much closer to the high-distance tank's
    // own ratio (10.0) than the naive 50/50 average (20.0) would be.
    assertThat(weighted).isCloseTo(130.0 / 1100.0 * 100.0, within(1e-9));
    assertThat(weighted).isLessThan(simpleAverageOfRatios);
  }
}
