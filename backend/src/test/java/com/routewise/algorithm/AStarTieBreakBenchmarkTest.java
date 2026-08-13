package com.routewise.algorithm;

import com.routewise.model.RouteMode;
import com.routewise.validation.MarkdownReportFile;
import com.routewise.validation.TrialResultBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Benchmarks the tie-break change in {@link AStarWaypointOptimizer#DEFAULT_COMPARATOR}
 * (prefer larger {@code g} on an {@code f}-tie) against the plain {@code f = g + h}
 * comparator the class used before it, and writes a comparison report.
 *
 * <p><strong>Why several waypoint counts, not one:</strong> at the production maximum
 * (10 waypoints) the state space is small enough that the tie-break effect is lost in
 * measurement noise (see the report's history). This class sweeps
 * {@link #SIZE_CONFIGS} to show whether — and how — the effect changes as the state
 * space grows. It does <strong>not</strong> reach 30 waypoints: {@code
 * AStarWaypointOptimizer} allocates {@code double[n][2^n]} and {@code
 * int[n][2^n][2]} tables, and at {@code n=30} those alone would require
 * ≈515 GB (30 × 2³⁰ × 8 bytes, twice over) — far beyond what this test, or most
 * hardware, can run. {@code n=18} (262,144 states/row) is the largest size here that
 * still finishes in a reasonable {@code mvn test} run; see the generated report's
 * "Por que não 30 waypoints" section for the full numbers. This is a benchmark
 * scaling limit, not a claim about where the production cap should be — that's a
 * separate decision, driven by UX and OSRM matrix-call cost, not just this algorithm.
 *
 * <p><strong>Why many matrices per size, not one:</strong> tie-breaking only changes
 * anything on instances where multiple open-set states genuinely share the same
 * {@code f}. Whether that helps or hurts wall-clock is instance-dependent — a single
 * matrix can just as easily show a regression as an improvement, and reporting only a
 * favourable seed would be cherry-picking. Each size runs several independently-seeded
 * matrices and reports the aggregate, the same statistical caution the rest of {@code
 * com.routewise.validation} already applies.
 *
 * <p><strong>Deliberate exception to this codebase's assert-only test convention:</strong>
 * every other test class here only asserts; the report-writing scripts under {@code
 * com.routewise.validation} ({@code RefactorMathReportSuite} and friends) are separate
 * {@code main()} entry points, run on demand, not on every {@code mvn test}. This class
 * was explicitly asked to generate {@code docs/refactor-math-result/comparacao-tempos.md}
 * as a side effect of running as a JUnit test, so it does — but that means the file's
 * numbers change on every run and reflect whatever machine last ran the suite, not a
 * fixed, reproducible ground truth. If that file should instead only be regenerated on
 * demand (matching the rest of the package), move this benchmark to a {@code main()}
 * script instead.
 */
@DisplayName("AStarWaypointOptimizer — tie-break benchmark")
class AStarTieBreakBenchmarkTest {

  /** The comparator this class used before Task 3 added the larger-g tie-break. */
  private static final Comparator<double[]> LEGACY_COMPARATOR =
    Comparator.comparingDouble(s -> s[2] + s[3]);

  /**
   * One entry per waypoint count swept. {@code matrixCount}/{@code warmupRuns}/{@code
   * timedRuns} shrink as {@code waypointCount} grows — {@code O(n²·2ⁿ)} makes each
   * individual solve far more expensive at n=18 than at n=10, so keeping the run
   * counts fixed across all sizes would make the suite take minutes instead of seconds.
   */
  private record SizeConfig(int waypointCount, int matrixCount, int warmupRuns, int timedRuns) {}

  private static final List<SizeConfig> SIZE_CONFIGS = List.of(
    new SizeConfig(10, 20, 5, 15),  // production maximum — see RouteRequestDto
    new SizeConfig(15, 10, 3, 8),
    new SizeConfig(18, 5, 2, 5)
  );

  private static final long BASE_SEED = 20260807L;
  private static final double COST_TOLERANCE = 1e-6;

  @Test
  @DisplayName("new tie-break finds the same optimal cost as the legacy comparator on every matrix, at every size; timings are reported")
  void tieBreakComparison_sameOptimalCost_reportGenerated() {
    List<SizeResult> sizeResults = new ArrayList<>(SIZE_CONFIGS.size());

    for (SizeConfig config : SIZE_CONFIGS) {
      List<MatrixResult> matrixResults = new ArrayList<>(config.matrixCount());

      for (int m = 0; m < config.matrixCount(); m++) {
        double[][] costMatrix = randomDurationMatrix(config.waypointCount(), BASE_SEED + m);

        BenchmarkResult legacy = benchmark(config, costMatrix, LEGACY_COMPARATOR);
        BenchmarkResult current = benchmark(config, costMatrix, null);

        assertThat(current.totalCostSec)
          .as("n=%d, matrix #%d: tie-breaking must never change the optimal route cost — "
            + "only which equally-good state is expanded first, since any expansion order "
            + "among f-tied states still converges to an optimal path once the heuristic "
            + "is admissible", config.waypointCount(), m)
          .isCloseTo(legacy.totalCostSec, within(COST_TOLERANCE));

        matrixResults.add(new MatrixResult(legacy, current));
      }

      sizeResults.add(new SizeResult(config, matrixResults));
    }

    writeReport(sizeResults);
  }

  private record BenchmarkResult(double totalCostSec, long medianNanos) {}

  private record MatrixResult(BenchmarkResult legacy, BenchmarkResult current) {}

  private record SizeResult(SizeConfig config, List<MatrixResult> matrices) {}

  /** {@code comparator == null} exercises the public API (current default tie-break). */
  private BenchmarkResult benchmark(SizeConfig config, double[][] costMatrix, Comparator<double[]> comparator) {
    AStarWaypointOptimizer optimizer = new AStarWaypointOptimizer();

    // Warm-up: let the JIT compile hot paths before any timed run, so the first
    // sample isn't dominated by interpreter/compilation overhead.
    for (int i = 0; i < config.warmupRuns(); i++) {
      run(optimizer, costMatrix, comparator);
    }

    long[] samples = new long[config.timedRuns()];
    List<Integer> lastResult = null;
    for (int i = 0; i < config.timedRuns(); i++) {
      long start = System.nanoTime();
      lastResult = run(optimizer, costMatrix, comparator);
      samples[i] = System.nanoTime() - start;
    }
    Arrays.sort(samples);
    long median = samples[samples.length / 2];

    double totalCost = TrialResultBuilder.sumAlongPath(costMatrix, lastResult);
    return new BenchmarkResult(totalCost, median);
  }

  private List<Integer> run(AStarWaypointOptimizer optimizer, double[][] costMatrix, Comparator<double[]> comparator) {
    return comparator == null
      ? optimizer.optimize(costMatrix, RouteMode.ROUND_TRIP)
      : optimizer.optimize(costMatrix, RouteMode.ROUND_TRIP, comparator);
  }

  /**
   * Plausible OSRM-style durations, quantised to a small set of distinct values
   * (multiples of 5 minutes, 5–60 min) rather than drawn continuously.
   *
   * <p>This is deliberate, not a simplification for convenience: with continuous
   * random doubles, two states landing on the exact same {@code f = g + h} is a
   * probability-zero event in practice, so the tie-break rule this benchmark
   * exists to measure would essentially never trigger, and the "before" and
   * "after" comparators would be indistinguishable on every matrix — a true but
   * uninteresting result. Quantised durations are also not unrealistic: OSRM
   * responses round to whole seconds, and any road network with symmetric or
   * repeated segment lengths (grid cities, highway stretches at the same speed
   * limit) produces exact duplicate edge costs in practice.
   */
  private double[][] randomDurationMatrix(int n, long seed) {
    Random rng = new Random(seed);
    int[] distinctMinuteValues = {5, 10, 15, 20, 25, 30, 40, 50, 60};
    double[][] matrix = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          continue;
        }
        int minutes = distinctMinuteValues[rng.nextInt(distinctMinuteValues.length)];
        matrix[i][j] = minutes * 60.0;
      }
    }
    return matrix;
  }

  private void writeReport(List<SizeResult> sizeResults) {
    StringBuilder sb = new StringBuilder();
    sb.append("# A* — Desempate por Maior g: Antes vs. Depois\n\n");
    sb.append(MarkdownReportFile.generatedAt());

    sb.append("## Contexto\n\n");
    sb.append(
      "Comparação entre o `Comparator` antigo da fila de prioridade do A* (`f = g + h`, " +
      "sem desempate) e o novo (`f = g + h`, empate resolvido a favor do maior `g`), " +
      "medida em várias escalas de waypoints — não só o máximo de produção (10) — para " +
      "ver se o efeito do desempate muda conforme o espaço de estados cresce. Durações " +
      "quantizadas em múltiplos de 5 minutos (ver Javadoc de `randomDurationMatrix` — " +
      "com durações contínuas, empates exatos em `f` são estatisticamente improváveis e " +
      "as duas versões convergem para o mesmo desempenho). Modo `ROUND_TRIP`, execuções " +
      "de aquecimento (JIT) descartadas antes de cronometrar cada matriz.\n\n"
    );
    sb.append(
      "**Aviso:** os tempos de parede (wall-clock) abaixo foram medidos na máquina que " +
      "rodou este teste — não são um benchmark de laboratório (tipo JMH) e variam entre " +
      "execuções e entre máquinas. A garantia formal que importa é a igualdade do custo " +
      "total, verificada matriz por matriz e checada por asserção em todas as escalas: o " +
      "desempate nunca muda qual rota é ótima, só a ordem em que estados de mesmo `f` são " +
      "expandidos (ver Javadoc de `AStarWaypointOptimizer.DEFAULT_COMPARATOR`). **O ganho " +
      "de velocidade não é garantido em toda instância** — é um efeito estatístico " +
      "agregado, não uma melhoria universal por requisição.\n\n"
    );

    appendWhyNot30(sb);

    sb.append("## Resultado agregado por escala\n\n");
    sb.append("| n (waypoints) | Estados/linha (2ⁿ) | Matrizes | Antes (µs) | Nova (µs) | Mais rápida em |\n");
    sb.append("|---|---|---|---|---|---|\n");
    for (SizeResult sr : sizeResults) {
      Aggregate agg = aggregate(sr.matrices());
      sb.append(String.format(Locale.US, "| %d | %,d | %d | %.1f | %.1f | %d de %d |\n",
        sr.config().waypointCount(), 1 << sr.config().waypointCount(), sr.matrices().size(),
        agg.meanLegacyMicros, agg.meanCurrentMicros, agg.fasterCount, sr.matrices().size()));
    }

    sb.append("\n## Custo total por matriz e por escala (prova de que o resultado ótimo não muda)\n\n");
    for (SizeResult sr : sizeResults) {
      sb.append(String.format(Locale.US, "### n = %d\n\n", sr.config().waypointCount()));
      sb.append("| Matriz | Custo — antes (s) | Custo — nova (s) | Igual? |\n");
      sb.append("|---|---|---|---|\n");
      for (int i = 0; i < sr.matrices().size(); i++) {
        MatrixResult r = sr.matrices().get(i);
        boolean equal = Math.abs(r.legacy.totalCostSec - r.current.totalCostSec) < COST_TOLERANCE;
        sb.append(String.format(Locale.US, "| %d | %.1f | %.1f | %s |\n",
          i, r.legacy.totalCostSec, r.current.totalCostSec, equal ? "sim" : "**NÃO**"));
      }
      sb.append('\n');
    }

    sb.append(
      "Em todas as matrizes, em todas as escalas testadas, o custo total encontrado foi " +
      "idêntico entre as duas versões (diferença < 1e-06 s), confirmando que o desempate " +
      "não altera a otimalidade — só potencialmente a ordem de expansão. O comportamento " +
      "de velocidade entre as escalas fica registrado na tabela agregada acima; leia-o " +
      "como tendência estatística, não como garantia por requisição.\n"
    );

    MarkdownReportFile.write(
      Path.of("..", "docs", "refactor-math-result", "comparacao-tempos.md"), sb.toString()
    );
  }

  private void appendWhyNot30(StringBuilder sb) {
    sb.append("## Por que não 30 waypoints\n\n");
    sb.append(
      "Foi pedido para rodar este benchmark com 30 waypoints. `AStarWaypointOptimizer` " +
      "aloca `double[n][2ⁿ]` (`bestG`) e `int[n][2ⁿ][2]` (`parent`) — em `n=10` isso é " +
      "10×1024 células, irrelevante; em `n=30`, `2³⁰ = 1.073.741.824`, e cada uma dessas " +
      "duas estruturas sozinha já exigiria:\n\n"
    );
    sb.append(String.format(Locale.US,
      "```\n" +
      "bestG:  30 × 2^30 × 8 bytes (double) ≈ %.1f GB\n" +
      "parent: 30 × 2^30 × 2 × 4 bytes (int) ≈ %.1f GB\n" +
      "total (só estas duas estruturas)     ≈ %.1f GB\n" +
      "```\n\n",
      30.0 * (1L << 30) * 8 / 1e9, 30.0 * (1L << 30) * 8 / 1e9, 2 * 30.0 * (1L << 30) * 8 / 1e9
    ));
    sb.append(
      "Isso é sem contar a fila de prioridade, que no pior caso guarda entradas para uma " +
      "fração grande desses mesmos estados. `n=31` já estouraria o limite de indexação de " +
      "array do Java (`2³¹ > Integer.MAX_VALUE`), então esse desenho de dados (array denso " +
      "indexado pela máscara de bits inteira) tem um teto físico por volta de `n=30`, " +
      "independente de qualquer otimização de constantes.\n\n" +
      "Este teste sobe até `n=18` (a maior escala que ainda roda em segundos, sem risco " +
      "de estourar o heap da JVM). Isso é um limite de **viabilidade deste benchmark**, " +
      "não uma recomendação sobre onde o limite de produção deveria ficar — essa é uma " +
      "decisão separada, guiada por UX e pelo custo da chamada de matriz do OSRM, não só " +
      "por este algoritmo. Se um `n` maior for realmente necessário no futuro, o caminho " +
      "não é mais memória — é trocar a estrutura de estado densa (array indexado pela " +
      "máscara inteira) por uma esparsa (`Map<Long, Double>` ou similar), já que a imensa " +
      "maioria dos `2ⁿ` estados nunca chega a ser visitada de fato.\n\n"
    );
  }

  private record Aggregate(double meanLegacyMicros, double meanCurrentMicros, int fasterCount) {}

  private Aggregate aggregate(List<MatrixResult> matrices) {
    double totalLegacyNanos = 0;
    double totalCurrentNanos = 0;
    int fasterCount = 0;
    for (MatrixResult r : matrices) {
      totalLegacyNanos += r.legacy.medianNanos;
      totalCurrentNanos += r.current.medianNanos;
      if (r.current.medianNanos < r.legacy.medianNanos) {
        fasterCount++;
      }
    }
    return new Aggregate(
      totalLegacyNanos / matrices.size() / 1_000.0,
      totalCurrentNanos / matrices.size() / 1_000.0,
      fasterCount
    );
  }
}
