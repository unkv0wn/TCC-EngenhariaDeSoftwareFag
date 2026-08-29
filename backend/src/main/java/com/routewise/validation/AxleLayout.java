package com.routewise.validation;

/**
 * How many tires a vehicle class carries at each {@link AxlePosition}.
 *
 * <p>Fixed per vehicle class — empirical calibration changes how long a tire lasts, never
 * how many tires the truck has. Counts follow standard Brazilian configurations: a single
 * steering axle (2 tires) plus dual-mounted rear axles (4 tires each).
 *
 * <p>This replaces the earlier use of {@code VehicleProfile.axleCount} as a stand-in for
 * tire quantity in the wear formula. Axle count and tire count are not interchangeable —
 * a 3-axle truck carries 10 tires, not 3 — so the old formula understated tire wear by
 * roughly a factor of three.
 *
 * @param dianteiro tires on the steering axle
 * @param tracao    tires on the drive axle(s)
 * @param reboque   tires on trailer axle(s); zero for non-articulated vehicles
 */
public record AxleLayout(int dianteiro, int tracao, int reboque) {

  /** VUC / light truck — 2 axles: single steering axle + one dual-mounted drive axle. */
  public static AxleLayout light() {
    return new AxleLayout(2, 4, 0);
  }

  /** Medium truck — 3 axles: single steering axle + two dual-mounted rear axles. */
  public static AxleLayout medium() {
    return new AxleLayout(2, 8, 0);
  }

  /** Articulated heavy truck — 5 axles: 3-axle tractor + 2-axle semi-trailer. */
  public static AxleLayout heavy() {
    return new AxleLayout(2, 8, 8);
  }

  public int tireCount(AxlePosition position) {
    return switch (position) {
      case DIANTEIRO -> dianteiro;
      case TRACAO -> tracao;
      case REBOQUE -> reboque;
    };
  }

  public int totalTires() {
    return dianteiro + tracao + reboque;
  }
}
