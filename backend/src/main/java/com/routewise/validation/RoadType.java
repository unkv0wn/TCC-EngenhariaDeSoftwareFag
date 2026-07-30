package com.routewise.validation;

/**
 * Synthetic road-type classification used by {@link ScenarioGenerator} to give
 * edges non-uniform speed/consumption/wear characteristics.
 *
 * <p>Without this, distance and duration would be perfectly proportional across
 * every edge, making Cenário A (time-only) and Cenário B (time + empirical)
 * always pick the identical route — a trivial, uninformative experiment.
 *
 * @param avgSpeedKmh    average travel speed for this road type
 * @param fuelMultiplier multiplier applied to base fuel consumption
 * @param wearMultiplier multiplier applied to base tire wear
 */
public enum RoadType {
  RODOVIA(80.0, 0.85, 0.8),
  ARTERIAL(50.0, 1.0, 1.0),
  URBANA(25.0, 1.3, 1.4);

  public final double avgSpeedKmh;
  public final double fuelMultiplier;
  public final double wearMultiplier;

  RoadType(double avgSpeedKmh, double fuelMultiplier, double wearMultiplier) {
    this.avgSpeedKmh = avgSpeedKmh;
    this.fuelMultiplier = fuelMultiplier;
    this.wearMultiplier = wearMultiplier;
  }
}
