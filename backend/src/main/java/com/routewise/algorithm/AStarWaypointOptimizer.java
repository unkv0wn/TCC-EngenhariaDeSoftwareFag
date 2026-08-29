package com.routewise.algorithm;

import com.routewise.model.RouteMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * A* algorithm for multi-waypoint route optimisation (TSP variant).
 *
 * <h2>Problem Definition</h2>
 * Given N waypoints and an N×N cost matrix
 * ({@code costMatrix[i][j]} = OSRM travel duration in seconds from waypoint i
 * to waypoint j), find the optimal visitation order that minimises total
 * travel duration — subject to:
 * <ul>
 *   <li>{@link RouteMode#ROUND_TRIP}: route must return to origin (index 0).</li>
 *   <li>{@link RouteMode#OPEN_ROUTE}: route ends at the last visited waypoint.</li>
 * </ul>
 *
 * <h2>State Space</h2>
 * <pre>
 *   State = (currentWaypointIndex, visitedBitmask)
 *   States = N × 2^N   →   max 10 × 1024 = 10,240 states (for N=10)
 * </pre>
 *
 * <h2>Heuristic (admissible)</h2>
 * For each unvisited waypoint, add the minimum global incoming edge weight
 * from the cost matrix.  This never overestimates the remaining cost because:
 * <ol>
 *   <li>Each unvisited node must be entered at some point.</li>
 *   <li>The cheapest possible entry is its global minimum incoming edge.</li>
 * </ol>
 */
@Component
public class AStarWaypointOptimizer {

  private static final Logger log = LoggerFactory.getLogger(AStarWaypointOptimizer.class);

  /**
   * Default open-set ordering: primarily by {@code f = g + h} ascending (standard
   * A*); ties broken by preferring the <strong>larger</strong> {@code g} (i.e. the
   * state deeper into the search, equivalently the one with the smaller — more
   * "used up" — heuristic remainder). Tie-breaking never changes which cost the
   * search converges to: any expansion order among equal-{@code f} states still
   * finds an optimal path once the heuristic is admissible. What it changes is how
   * many states get expanded before that happens — preferring larger {@code g} on
   * ties biases expansion toward states that have made more concrete progress
   * instead of re-exploring shallower, equally-promising alternatives, which
   * tends to reduce the number of states explored in practice.
   */
  private static final Comparator<double[]> DEFAULT_COMPARATOR =
    Comparator
      .comparingDouble((double[] s) -> s[2] + s[3])                 // f = g + h, ascending
      .thenComparing(Comparator.comparingDouble((double[] s) -> s[2]).reversed()); // tie: larger g first

  // ─────────────────────────────────────────────────────────────────────────
  // Public API
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Finds the optimal waypoint visitation order.
   *
   * @param costMatrix N×N matrix where {@code costMatrix[i][j]} is the OSRM
   *                   driving duration (seconds) from waypoint i to waypoint j
   * @param mode       {@link RouteMode#ROUND_TRIP} or
   *                   {@link RouteMode#OPEN_ROUTE}
   * @return ordered list of waypoint indices (0-based); for ROUND_TRIP the
   *         origin index 0 is appended at the end
   */
  public List<Integer> optimize(double[][] costMatrix, RouteMode mode) {
    return optimize(costMatrix, mode, DEFAULT_COMPARATOR);
  }

  /**
   * Same as {@link #optimize(double[][], RouteMode)}, but with the open-set
   * comparator exposed. Package-private — not part of the public API. Exists so
   * tests can reproduce the search under a different tie-break rule (e.g. the
   * plain {@code f = g + h} comparator this class used before tie-breaking was
   * added) without duplicating the algorithm.
   *
   * @param stateComparator orders entries in the open set; the state that
   *                        compares smallest is expanded next. Each entry is
   *                        {@code [current, visited, gCost, hCost]}.
   */
  List<Integer> optimize(double[][] costMatrix, RouteMode mode, Comparator<double[]> stateComparator) {
    int n = costMatrix.length;
    if (n == 1) {
      return List.of(0);
    }

    boolean roundTrip  = (mode == RouteMode.ROUND_TRIP);
    int     allVisited = (1 << n) - 1;

    // Pre-compute minimum incoming edge for each node (used in heuristic)
    double[] minIncoming = computeMinIncoming(costMatrix, n);

    // ── Priority queue: [current, visited, gCost, hCost] ─────────────────
    PriorityQueue<double[]> openSet = new PriorityQueue<>(stateComparator);

    // ── Closed set: best gCost found for each (current, visited) state ───
    double[][] bestG = new double[n][allVisited + 1];
    for (double[] row : bestG) {
      Arrays.fill(row, Double.MAX_VALUE);
    }

    // ── Parent tracking for path reconstruction ───────────────────────────
    // parent[current][visited] = {prevCurrent, prevVisited}
    int[][][] parent = new int[n][allVisited + 1][2];
    for (int[][] a : parent) {
      for (int[] b : a) {
        Arrays.fill(b, -1);
      }
    }

    // ── Initial state: at waypoint 0, only origin visited ────────────────
    int    startVisited = 1; // bit 0 set
    double hStart       = heuristic(0, startVisited, costMatrix, minIncoming, n, allVisited, roundTrip);

    openSet.offer(new double[]{0, startVisited, 0.0, hStart});
    bestG[0][startVisited] = 0.0;

    int statesExplored = 0;

    // ── Main A* loop ──────────────────────────────────────────────────────
    while (!openSet.isEmpty()) {
      double[] state   = openSet.poll();
      int      current = (int) state[0];
      int      visited = (int) state[1];
      double   gCost   = state[2];

      statesExplored++;

      // Skip stale entries (a better path was already found)
      if (gCost > bestG[current][visited] + 1e-9) {
        continue;
      }

      // ── Goal check: all waypoints visited ────────────────────────────
      if (visited == allVisited) {
        double total = gCost + (roundTrip ? costMatrix[current][0] : 0.0);
        log.info(
          "A* converged: statesExplored={}, totalDurationSec={}, mode={}",
          statesExplored, String.format("%.1f", total), mode
        );
        return reconstructPath(parent, current, visited, n, roundTrip);
      }

      // ── Expand: try visiting each unvisited waypoint ──────────────────
      for (int next = 0; next < n; next++) {
        if ((visited & (1 << next)) != 0) {
          continue; // already visited
        }

        // For FIXED_START_END, do not visit the destination (n-1) until all other nodes are visited
        if (mode == RouteMode.FIXED_START_END && next == n - 1 && visited != (allVisited ^ (1 << (n - 1)))) {
          continue;
        }

        int    nextVisited = visited | (1 << next);
        double nextG       = gCost + costMatrix[current][next];

        if (nextG < bestG[next][nextVisited]) {
          bestG[next][nextVisited]       = nextG;
          parent[next][nextVisited][0]   = current;
          parent[next][nextVisited][1]   = visited;

          double h = heuristic(next, nextVisited, costMatrix, minIncoming, n, allVisited, roundTrip);
          openSet.offer(new double[]{next, nextVisited, nextG, h});
        }
      }
    }

    // Fallback: should never occur for a complete, finite cost matrix
    log.warn("A* exhausted state space without finding a solution; returning sequential order");
    List<Integer> fallback = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      fallback.add(i);
    }
    if (roundTrip) {
      fallback.add(0);
    }
    return fallback;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Private helpers
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Admissible heuristic: sum of minimum incoming edge for each unvisited
   * waypoint, plus potential return cost for round-trips.
   */
  private double heuristic(
    int current, int visited, double[][] costs,
    double[] minIncoming, int n, int allVisited, boolean roundTrip
  ) {
    double h = 0;

    // Add min incoming edge for every unvisited node
    for (int i = 0; i < n; i++) {
      if ((visited & (1 << i)) == 0) {
        h += minIncoming[i];
      }
    }

    // For ROUND_TRIP: add a lower bound for the eventual return to origin
    if (roundTrip) {
      if (visited == allVisited) {
        // All visited — must return directly from current
        h += costs[current][0];
      } else {
        // Optimistic: minimum return cost from current or any unvisited node
        double minReturn = costs[current][0];
        for (int i = 0; i < n; i++) {
          if ((visited & (1 << i)) == 0 && costs[i][0] < minReturn) {
            minReturn = costs[i][0];
          }
        }
        h += minReturn;
      }
    }

    return h;
  }

  /** Pre-computes the global minimum incoming edge weight for each node. */
  private double[] computeMinIncoming(double[][] costs, int n) {
    double[] minIn = new double[n];
    Arrays.fill(minIn, Double.MAX_VALUE);

    for (int j = 0; j < n; j++) {
      for (int i = 0; i < n; i++) {
        if (i != j && costs[i][j] < minIn[j]) {
          minIn[j] = costs[i][j];
        }
      }
      // Guard: if no incoming edge exists, use 0 to avoid inflating heuristic
      if (minIn[j] == Double.MAX_VALUE) {
        minIn[j] = 0;
      }
    }
    return minIn;
  }

  /** Reconstructs the optimal path by following parent pointers backwards. */
  private List<Integer> reconstructPath(
    int[][][] parent, int goalCurrent, int goalVisited, int n, boolean roundTrip
  ) {
    List<Integer> reversePath = new ArrayList<>();
    int cur = goalCurrent;
    int vis = goalVisited;

    while (cur != -1) {
      reversePath.add(cur);
      int prevCur = parent[cur][vis][0];
      int prevVis = parent[cur][vis][1];
      if (prevCur == -1) {
        break; // reached the start node
      }
      cur = prevCur;
      vis = prevVis;
    }

    Collections.reverse(reversePath);

    if (roundTrip) {
      reversePath.add(0); // return to origin
    }

    return reversePath;
  }
}
