package com.routewise.validation;

/**
 * How much of a vehicle's capacity a cargo load occupies, by weight and by volume.
 *
 * <p>Mirrors real freight practice ("peso x cubagem"): a load is constrained by
 * whichever dimension — weight or volume — is more restrictive, not by weight alone.
 * A load of light, bulky cargo can fill a truck's volume long before it reaches its
 * weight limit, and vice versa for dense cargo.
 *
 * @param weightFraction    cargoWeightKg / vehicle capacityKg, uncapped (can exceed 1.0)
 * @param volumeFraction    cargoVolumeM3 / vehicle capacityM3, uncapped (can exceed 1.0)
 * @param effectiveLoadFactor max(weightFraction, volumeFraction), clamped to [0, 1] — fed into
 *                          the fuel/tire-wear formulas, which are calibrated for that range
 * @param volumeBound       true if volume, not weight, is the binding constraint
 * @param feasible          true if the load fits within both capacity limits
 */
public record CargoOccupancy(
  double weightFraction,
  double volumeFraction,
  double effectiveLoadFactor,
  boolean volumeBound,
  boolean feasible
) {

  public static CargoOccupancy compute(double cargoWeightKg, double cargoVolumeM3, VehicleProfile profile) {
    double weightFraction = cargoWeightKg / profile.capacityKg();
    double volumeFraction = cargoVolumeM3 / profile.capacityM3();
    double rawOccupancy = Math.max(weightFraction, volumeFraction);

    return new CargoOccupancy(
      weightFraction,
      volumeFraction,
      Math.min(rawOccupancy, 1.0),
      volumeFraction > weightFraction,
      rawOccupancy <= 1.0
    );
  }
}
