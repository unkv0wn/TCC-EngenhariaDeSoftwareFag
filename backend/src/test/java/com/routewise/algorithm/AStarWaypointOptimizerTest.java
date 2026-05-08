package com.routewise.algorithm;

import com.routewise.model.RouteMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AStarWaypointOptimizer}.
 *
 * <p>Validates correctness of A* route optimisation for known cost matrices
 * where the optimal ordering can be determined analytically.
 */
@DisplayName("AStarWaypointOptimizer")
class AStarWaypointOptimizerTest {

  private AStarWaypointOptimizer optimizer;

  @BeforeEach
  void setUp() {
    optimizer = new AStarWaypointOptimizer();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // OPEN_ROUTE tests
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("OPEN_ROUTE: finds optimal order for 3 waypoints")
  void openRoute_threeWaypoints_findsOptimalOrder() {
    // Cost matrix: cheapest path is 0 → 2 → 1 (cost = 10 + 5 = 15)
    //             vs  0 → 1 → 2 (cost = 30 + 5 = 35)
    double[][] costs = {
      {0,  30, 10},   // from 0: to 1 costs 30, to 2 costs 10
      {30,  0,  5},   // from 1: to 0 costs 30, to 2 costs 5
      {10,  5,  0}    // from 2: to 0 costs 10, to 1 costs 5
    };

    List<Integer> result = optimizer.optimize(costs, RouteMode.OPEN_ROUTE);

    // Optimal: 0 → 2 → 1 (total = 10 + 5 = 15)
    assertThat(result).containsExactly(0, 2, 1);
    assertThat(result).hasSize(3);
  }

  @Test
  @DisplayName("OPEN_ROUTE: 2 waypoints returns [0, 1]")
  void openRoute_twoWaypoints_returnsSimplePath() {
    double[][] costs = {
      {0, 100},
      {100, 0}
    };

    List<Integer> result = optimizer.optimize(costs, RouteMode.OPEN_ROUTE);

    assertThat(result).containsExactly(0, 1);
  }

  @Test
  @DisplayName("OPEN_ROUTE: single waypoint returns [0]")
  void openRoute_oneWaypoint_returnsOrigin() {
    double[][] costs = {{0}};

    List<Integer> result = optimizer.optimize(costs, RouteMode.OPEN_ROUTE);

    assertThat(result).containsExactly(0);
  }

  @Test
  @DisplayName("OPEN_ROUTE: does NOT append origin at end")
  void openRoute_doesNotReturnToOrigin() {
    double[][] costs = {
      {0, 10, 20},
      {10,  0, 10},
      {20, 10,  0}
    };

    List<Integer> result = optimizer.optimize(costs, RouteMode.OPEN_ROUTE);

    // Last element must NOT be 0 (origin) unless optimal path ends there
    // For symmetric costs above, optimal is 0→1→2 or 0→2→1 at cost 20
    assertThat(result.get(0)).isEqualTo(0); // always starts at origin
    assertThat(result).hasSize(3);              // no extra element
  }

  // ─────────────────────────────────────────────────────────────────────────
  // ROUND_TRIP tests
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("ROUND_TRIP: appends origin at end")
  void roundTrip_appendsOriginAtEnd() {
    double[][] costs = {
      {0, 10, 20},
      {10,  0, 10},
      {20, 10,  0}
    };

    List<Integer> result = optimizer.optimize(costs, RouteMode.ROUND_TRIP);

    assertThat(result.get(0)).isEqualTo(0);
    assertThat(result.get(result.size() - 1)).isEqualTo(0);
    assertThat(result).hasSize(4); // 3 waypoints + return to origin
  }

  @Test
  @DisplayName("ROUND_TRIP: finds optimal cyclic order for asymmetric costs")
  void roundTrip_asymmetricMatrix_findsOptimalCycle() {
    // Asymmetric cost: going 0→1→2→0 costs 10+5+3  = 18
    //                 going 0→2→1→0 costs 20+5+10  = 35
    double[][] costs = {
      {0,  10, 20},   // from 0
      {10,  0,  5},   // from 1
      { 3,  5,  0}    // from 2 → returning to 0 costs only 3
    };

    List<Integer> result = optimizer.optimize(costs, RouteMode.ROUND_TRIP);

    assertThat(result.get(0)).isEqualTo(0);
    assertThat(result.get(result.size() - 1)).isEqualTo(0);
    // Optimal cycle: 0 → 1 → 2 → 0 (cost = 10 + 5 + 3 = 18)
    assertThat(result).containsExactly(0, 1, 2, 0);
  }

  @Test
  @DisplayName("ROUND_TRIP: 2 waypoints returns [0, 1, 0]")
  void roundTrip_twoWaypoints_returnsCycle() {
    double[][] costs = {
      {0, 50},
      {50, 0}
    };

    List<Integer> result = optimizer.optimize(costs, RouteMode.ROUND_TRIP);

    assertThat(result).containsExactly(0, 1, 0);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Edge cases
  // ─────────────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("All waypoints at the same location (zero-cost matrix)")
  void zeroCostMatrix_returnsValidPath() {
    double[][] costs = {
      {0, 0, 0},
      {0, 0, 0},
      {0, 0, 0}
    };

    List<Integer> open  = optimizer.optimize(costs, RouteMode.OPEN_ROUTE);
    List<Integer> round = optimizer.optimize(costs, RouteMode.ROUND_TRIP);

    assertThat(open).hasSize(3);
    assertThat(round).hasSize(4);
    assertThat(round.get(round.size() - 1)).isEqualTo(0);
  }

  @Test
  @DisplayName("4 waypoints: result has all indices exactly once (plus origin for ROUND_TRIP)")
  void fourWaypoints_allIndicesPresentExactlyOnce() {
    double[][] costs = {
      { 0, 20, 42,  8},
      {20,  0, 30, 15},
      {42, 30,  0, 25},
      { 8, 15, 25,  0}
    };

    List<Integer> result = optimizer.optimize(costs, RouteMode.OPEN_ROUTE);

    assertThat(result).hasSize(4);
    assertThat(result).containsExactlyInAnyOrder(0, 1, 2, 3);
    assertThat(result.get(0)).isEqualTo(0);
  }
}
