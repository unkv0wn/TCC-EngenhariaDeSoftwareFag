package com.routewise.validation;

import java.nio.file.Path;
import java.util.List;

/**
 * Generates every report under {@code docs/refactor-math-result/} — one per cost
 * dimension reviewed: capacity (kg/m³), fuel consumption, time and distance, and tire
 * wear by axle position.
 *
 * <p>Only the last of those changed in this refactor; the other three were already
 * implemented and are re-validated here because the corrected tire formula shifts the
 * absolute numbers they report. Each report opens with a summary stating which case it is.
 *
 * <p>Run with: {@code mvn compile exec:java
 * -Dexec.mainClass=com.routewise.validation.RefactorMathReportSuite}
 */
public final class RefactorMathReportSuite {

  private static final Path REPORT_DIR = Path.of("..", "docs", "refactor-math-result");

  private static final List<VehicleProfile> PROFILES =
    List.of(VehicleProfile.light(), VehicleProfile.medium(), VehicleProfile.heavy());

  private RefactorMathReportSuite() {}

  public static void main(String[] args) {
    CapacityDimensionReport.write(REPORT_DIR, PROFILES);
    FuelDimensionReport.write(REPORT_DIR, PROFILES);
    TimeDistanceDimensionReport.write(REPORT_DIR, PROFILES);
    AxleTireWearDimensionReport.write(REPORT_DIR, PROFILES);
  }
}
