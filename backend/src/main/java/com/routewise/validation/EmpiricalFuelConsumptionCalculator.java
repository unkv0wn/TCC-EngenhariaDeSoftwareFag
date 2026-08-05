package com.routewise.validation;

import java.util.List;

/**
 * Derives {@code baseFuelConsumptionLPer100Km} from a vehicle's refueling log instead of
 * a fixed {@link VehicleProfile} constant — see
 * {@code docs/superpowers/specs/2026-08-01-empirical-fuel-consumption-rolling-window-design.md}.
 *
 * <p>Uses a rolling window of the last {@value #WINDOW_SIZE} refueling records, weighted by
 * distance ({@code Σ litersRefueled / Σ kmSincePrevious × 100}) rather than a simple average
 * of each record's individual ratio — a tank that covered more km should count for more, not
 * be weighted equally against a tank that covered less (same reasoning
 * {@link FuelConsumptionFromRefuelingExample} already documents for the full-history case).
 *
 * <p>Cold start: with fewer than {@value #WINDOW_SIZE} records, uses whichever records exist
 * (window narrows down to a single record, then to zero); with zero records, falls back to
 * the caller-supplied assumed value (typically {@code VehicleProfile.light/medium/heavy()}'s
 * documented constant) so a newly registered vehicle isn't blocked on empirical data before
 * it can be costed at all.
 */
public final class EmpiricalFuelConsumptionCalculator {

  public static final int WINDOW_SIZE = 5;

  private EmpiricalFuelConsumptionCalculator() {}

  /** One fill-up: liters added, and km driven since the previous fill-up. */
  public record RefuelingRecord(String date, double litersRefueled, double kmSincePrevious) {
    public double consumptionLPer100Km() {
      return litersRefueled / kmSincePrevious * 100.0;
    }
  }

  /**
   * @param log                chronologically ordered refueling records (oldest first)
   * @param fallbackConsumption value to use when {@code log} is empty (cold start)
   */
  public static double consumptionLPer100Km(List<RefuelingRecord> log, double fallbackConsumption) {
    if (log.isEmpty()) {
      return fallbackConsumption;
    }

    List<RefuelingRecord> window = log.subList(Math.max(0, log.size() - WINDOW_SIZE), log.size());
    double totalLiters = window.stream().mapToDouble(RefuelingRecord::litersRefueled).sum();
    double totalKm = window.stream().mapToDouble(RefuelingRecord::kmSincePrevious).sum();
    return totalLiters / totalKm * 100.0;
  }
}
