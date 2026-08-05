package com.routewise.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.routewise.validation.EmpiricalTireLifeCalculator.AxlePosition.TRACAO;
import static com.routewise.validation.EmpiricalTireLifeCalculator.TireReplacementReason.DANO_ACIDENTE;
import static com.routewise.validation.EmpiricalTireLifeCalculator.TireReplacementReason.DESGASTE_NORMAL;
import static com.routewise.validation.EmpiricalTireLifeCalculator.TireReplacementRecord;
import static com.routewise.validation.EmpiricalTireLifeCalculator.tireLifeKm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link EmpiricalTireLifeCalculator} — see
 * {@code docs/superpowers/specs/2026-08-01-empirical-tire-wear-by-axle-position-design.md}
 * for the rolling-window + wear-only-filter + cold-start policy these tests verify.
 *
 * <p>All records here are pre-scoped to a single {@code (classeVeiculo, posicaoEixo)} group
 * — grouping itself is the caller's responsibility, not the calculator's.
 */
@DisplayName("EmpiricalTireLifeCalculator")
class EmpiricalTireLifeCalculatorTest {

  private static final double FALLBACK = 60_000.0; // VehicleProfile.medium()'s assumed constant

  @Test
  @DisplayName("empty log falls back to the assumed constant (cold start)")
  void emptyLog_usesFallback() {
    assertThat(tireLifeKm(List.of(), FALLBACK)).isEqualTo(FALLBACK);
  }

  @Test
  @DisplayName("log with only accident replacements falls back — no wear data exists")
  void onlyAccidentReplacements_usesFallback() {
    List<TireReplacementRecord> log = List.of(
      new TireReplacementRecord(0.0, 8_000.0, DANO_ACIDENTE, TRACAO),
      new TireReplacementRecord(8_000.0, 15_000.0, DANO_ACIDENTE, TRACAO)
    );

    assertThat(tireLifeKm(log, FALLBACK)).isEqualTo(FALLBACK);
  }

  @Test
  @DisplayName("log narrower than the window uses whichever wear events exist (partial window)")
  void partialLog_usesAvailableWearEvents() {
    List<TireReplacementRecord> log = List.of(
      new TireReplacementRecord(0.0, 58_000.0, DESGASTE_NORMAL, TRACAO)
    );

    assertThat(tireLifeKm(log, FALLBACK)).isCloseTo(58_000.0, within(1e-9));
  }

  @Test
  @DisplayName("log exactly the window size uses all of it")
  void exactWindowLog_usesAllWearEvents() {
    List<TireReplacementRecord> log = List.of(
      new TireReplacementRecord(0.0, 55_000.0, DESGASTE_NORMAL, TRACAO),
      new TireReplacementRecord(55_000.0, 117_000.0, DESGASTE_NORMAL, TRACAO)
    );
    double expected = (55_000.0 + 62_000.0) / 2.0;

    assertThat(tireLifeKm(log, FALLBACK)).isCloseTo(expected, within(1e-9));
  }

  @Test
  @DisplayName("log longer than the window only considers the last 2 wear events")
  void longLog_ignoresWearEventsOlderThanWindow() {
    List<TireReplacementRecord> log = List.of(
      new TireReplacementRecord(0.0, 20_000.0, DESGASTE_NORMAL, TRACAO), // outside the window
      new TireReplacementRecord(20_000.0, 75_000.0, DESGASTE_NORMAL, TRACAO), // outside the window
      new TireReplacementRecord(75_000.0, 137_000.0, DESGASTE_NORMAL, TRACAO),
      new TireReplacementRecord(137_000.0, 194_000.0, DESGASTE_NORMAL, TRACAO)
    );
    double expected = (62_000.0 + 57_000.0) / 2.0; // only the last two wear events

    assertThat(tireLifeKm(log, FALLBACK)).isCloseTo(expected, within(1e-9));
  }

  @Test
  @DisplayName("an accident replacement is skipped, not counted as a short-lived wear event")
  void accidentReplacement_isExcludedFromWindow() {
    List<TireReplacementRecord> withAccidentMixedIn = List.of(
      new TireReplacementRecord(0.0, 55_000.0, DESGASTE_NORMAL, TRACAO),
      new TireReplacementRecord(55_000.0, 58_000.0, DANO_ACIDENTE, TRACAO), // 3,000 km, pothole — not wear
      new TireReplacementRecord(58_000.0, 120_000.0, DESGASTE_NORMAL, TRACAO),
      new TireReplacementRecord(120_000.0, 177_000.0, DESGASTE_NORMAL, TRACAO)
    );
    List<TireReplacementRecord> sameWearEventsWithoutAccident = List.of(
      new TireReplacementRecord(0.0, 55_000.0, DESGASTE_NORMAL, TRACAO),
      new TireReplacementRecord(55_000.0, 117_000.0, DESGASTE_NORMAL, TRACAO), // 62,000 km — accident gap removed
      new TireReplacementRecord(117_000.0, 174_000.0, DESGASTE_NORMAL, TRACAO)
    );

    assertThat(tireLifeKm(withAccidentMixedIn, FALLBACK))
      .isCloseTo(tireLifeKm(sameWearEventsWithoutAccident, FALLBACK), within(1e-9));
  }

  @Test
  @DisplayName("uses a simple average within the window, not weighted by anything")
  void windowAverage_isUnweighted() {
    List<TireReplacementRecord> log = List.of(
      new TireReplacementRecord(0.0, 40_000.0, DESGASTE_NORMAL, TRACAO),  // 40,000 km
      new TireReplacementRecord(40_000.0, 120_000.0, DESGASTE_NORMAL, TRACAO) // 80,000 km
    );

    // Plain (40,000 + 80,000) / 2 — no distance- or count-based weighting applies here,
    // unlike EmpiricalFuelConsumptionCalculator's Σlitros/Σkm.
    assertThat(tireLifeKm(log, FALLBACK)).isCloseTo(60_000.0, within(1e-9));
  }
}
