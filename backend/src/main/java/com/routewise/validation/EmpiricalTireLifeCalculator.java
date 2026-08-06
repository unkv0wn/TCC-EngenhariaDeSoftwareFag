package com.routewise.validation;

import java.util.List;

/**
 * Derives {@code VehicleProfile.tireLifeKm} from a fleet's tire-replacement log instead of
 * a fixed constant — see
 * {@code docs/superpowers/specs/2026-08-01-empirical-tire-wear-by-axle-position-design.md}.
 *
 * <p>Callers are expected to pass a log already scoped to one {@code (classeVeiculo,
 * posicaoEixo)} group — same convention {@link EmpiricalFuelConsumptionCalculator} uses for
 * "one vehicle's log": grouping is the caller's responsibility, not this class's.
 *
 * <p>Uses a rolling window of the last {@value #WINDOW_SIZE} replacement events, filtered to
 * {@link TireReplacementReason#DESGASTE_NORMAL} only — a tire replaced after
 * {@link TireReplacementReason#DANO_ACIDENTE} (pothole, curb strike) measures an accident,
 * not wear, and is excluded before windowing rather than relying on it standing out in the
 * average (unlike a fuel log typo, it's real data about a different phenomenon, not an
 * obvious outlier).
 *
 * <p>Unlike {@link EmpiricalFuelConsumptionCalculator}, the window average is a simple
 * (unweighted) mean of {@code kmUsado} per event, not a distance-weighted ratio: each
 * replacement is already one complete "how far did this tire last" measurement, not a
 * partial rate that needs weighting by how much distance it covers. The window is smaller
 * ({@value #WINDOW_SIZE} vs. fuel's 5) because replacement is a rare, terminal event even
 * after aggregating across a fleet class — a wider window would starve the sample further
 * for a recency benefit that matters less here (a fresh tire's wear is independent of the
 * previous tire's, unlike fuel consumption which reflects the same engine's current state).
 */
public final class EmpiricalTireLifeCalculator {

  public static final int WINDOW_SIZE = 2;

  private EmpiricalTireLifeCalculator() {}

  public enum TireReplacementReason { DESGASTE_NORMAL, DANO_ACIDENTE }

  /** One tire's full lifespan: odometer at install, odometer at replacement, why, and where. */
  public record TireReplacementRecord(
    double kmInstalacao, double kmTroca, TireReplacementReason motivoTroca, AxlePosition posicaoEixo
  ) {
    public double kmUsado() {
      return kmTroca - kmInstalacao;
    }
  }

  /**
   * @param log                  chronologically ordered replacement records (oldest first),
   *                             already scoped to one {@code (classeVeiculo, posicaoEixo)} group
   * @param fallbackTireLifeKm   value to use when no {@code DESGASTE_NORMAL} event exists (cold start)
   */
  public static double tireLifeKm(List<TireReplacementRecord> log, double fallbackTireLifeKm) {
    List<TireReplacementRecord> wearEvents = log.stream()
      .filter(r -> r.motivoTroca() == TireReplacementReason.DESGASTE_NORMAL)
      .toList();

    if (wearEvents.isEmpty()) {
      return fallbackTireLifeKm;
    }

    List<TireReplacementRecord> window =
      wearEvents.subList(Math.max(0, wearEvents.size() - WINDOW_SIZE), wearEvents.size());

    return window.stream().mapToDouble(TireReplacementRecord::kmUsado).average().orElseThrow();
  }
}
