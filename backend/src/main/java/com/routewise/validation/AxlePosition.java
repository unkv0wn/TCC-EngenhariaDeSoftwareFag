package com.routewise.validation;

/**
 * Where an axle sits on the vehicle, and how much faster or slower its tires wear
 * relative to the vehicle class's rated {@code tireLifeKm}.
 *
 * <p>The three positions wear differently for physical reasons, not noise: the steering
 * axle grinds its tires laterally through every turn and suffers most from alignment
 * error, the drive axle absorbs engine torque, and a trailer axle only carries weight —
 * no torque, no steering. Modelling them as one averaged number (as
 * {@code CostMatrixBuilder} did before) hides that spread.
 *
 * <p>{@link #TRACAO} is the reference (factor 1.0) because the empirical replacement log
 * in {@code empirical-data/tire-replacements-by-vehicle-type.csv} is almost entirely
 * drive-axle events — anchoring there keeps the existing calibration meaningful instead
 * of silently reinterpreting it against a different baseline. The other two factors are
 * documented plausible assumptions, same caveat as every constant in
 * {@link VehicleProfile}: no fleet telemetry separates them yet.
 *
 * @param lifeFactor multiplier on the class's {@code tireLifeKm} for this position —
 *                   below 1.0 means the tire wears out sooner than the class average
 */
public enum AxlePosition {
  /** Steering axle — shortest life; lateral scrub in turns plus alignment sensitivity. */
  DIANTEIRO(0.85),
  /** Drive axle — the reference position; absorbs engine torque. */
  TRACAO(1.00),
  /** Trailer axle — longest life; carries weight without torque or steering input. */
  REBOQUE(1.25);

  public final double lifeFactor;

  AxlePosition(double lifeFactor) {
    this.lifeFactor = lifeFactor;
  }
}
