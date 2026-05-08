package com.routewise.algorithm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link HaversineUtil}.
 */
@DisplayName("HaversineUtil")
class HaversineUtilTest {

  /** São Paulo (IATA coords): lat=-23.5505, lng=-46.6333 */
  private static final double SAO_PAULO_LAT = -23.5505;
  private static final double SAO_PAULO_LNG = -46.6333;

  /** Rio de Janeiro (IATA coords): lat=-22.9068, lng=-43.1729 */
  private static final double RIO_LAT = -22.9068;
  private static final double RIO_LNG = -43.1729;

  /** Known great-circle distance SP → RJ ≈ 357 km (±5 km tolerance) */
  private static final double SP_TO_RJ_KM = 357.0;

  @Test
  @DisplayName("Distance between São Paulo and Rio de Janeiro ≈ 357 km")
  void spToRio_approximatelyCorrect() {
    double dist = HaversineUtil.distanceKm(SAO_PAULO_LAT, SAO_PAULO_LNG, RIO_LAT, RIO_LNG);
    assertThat(dist).isCloseTo(SP_TO_RJ_KM, within(5.0));
  }

  @Test
  @DisplayName("Distance from a point to itself is 0")
  void samePoint_distanceIsZero() {
    double dist = HaversineUtil.distanceKm(SAO_PAULO_LAT, SAO_PAULO_LNG, SAO_PAULO_LAT, SAO_PAULO_LNG);
    assertThat(dist).isCloseTo(0.0, within(1e-9));
  }

  @Test
  @DisplayName("Distance is symmetric: dist(A, B) == dist(B, A)")
  void symmetry_distanceIsEqual() {
    double ab = HaversineUtil.distanceKm(SAO_PAULO_LAT, SAO_PAULO_LNG, RIO_LAT, RIO_LNG);
    double ba = HaversineUtil.distanceKm(RIO_LAT, RIO_LNG, SAO_PAULO_LAT, SAO_PAULO_LNG);
    assertThat(ab).isCloseTo(ba, within(1e-9));
  }

  @Test
  @DisplayName("toMetres converts kilometres correctly")
  void toMetres_convertsCorrectly() {
    assertThat(HaversineUtil.toMetres(1.0)).isEqualTo(1000.0);
    assertThat(HaversineUtil.toMetres(0.5)).isEqualTo(500.0);
  }

  @Test
  @DisplayName("Distance is always non-negative")
  void distance_isAlwaysNonNegative() {
    double dist = HaversineUtil.distanceKm(0, 0, -90, 180);
    assertThat(dist).isGreaterThanOrEqualTo(0.0);
  }
}
