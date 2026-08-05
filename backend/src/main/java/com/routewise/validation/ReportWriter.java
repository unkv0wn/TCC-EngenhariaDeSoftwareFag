package com.routewise.validation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Writes the {@link Summary} of an experiment run to a Markdown report. */
public final class ReportWriter {

  private ReportWriter() {}

  public static void write(Path outputPath, Summary s, VehicleProfile profile) {
    String content = render(s, profile);
    try {
      Files.createDirectories(outputPath.getParent());
      Files.writeString(outputPath, content);
    } catch (IOException ex) {
      throw new UncheckedIOException("Failed to write validation report to " + outputPath, ex);
    }
  }

  /** Builds the report content without writing it — lets callers compose multiple profiles into one file. */
  public static String render(Summary s, VehicleProfile profile) {
    String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    return String.format(Locale.US, """
      # Validação Empírica do Custo de Rota — Relatório (Perfil: %s)

      Gerado em: %s
      Trials: %d

      ## 1. Metodologia e suposições

      Experimento sintético — nenhum dado é telemetria real de frota. Cenário A replica
      o comportamento atual de produção (minimizar apenas duração). Cenário B minimiza
      um custo combinado em R$ (tempo + combustível + desgaste de pneu), usando o
      `AStarWaypointOptimizer` já existente, sem alterações, em ambos os casos.

      Perfil de veículo assumido (constante, fixo para todos os trials):

      | Parâmetro | Valor |
      |---|---|
      | Eixos | %d |
      | Capacidade (peso) | %.0f kg |
      | Capacidade (volume) | %.1f m³ |
      | Consumo base | %.1f L/100km |
      | Preço do litro | R$ %.2f |
      | Custo de reposição do pneu | R$ %.2f |
      | Vida útil do pneu | %.0f km |
      | Custo-hora do motorista | R$ %.2f |

      A carga de cada trial é sorteada em kg e m³ de forma independente da capacidade
      do veículo (mesma faixa bruta reaproveitada entre perfis, para comparação justa —
      ver `ScenarioGenerator`). A ocupação efetiva usada nas fórmulas de consumo e
      desgaste é `min(max(peso/capacidadeKg, volume/capacidadeM3), 1)` — ou seja, a
      dimensão mais restritiva (peso OU cubagem) é que manda, como na prática real de
      frete. Cargas cuja ocupação bruta excede 100%% em qualquer dimensão são marcadas
      como inviáveis (ver seção 2) — hoje o algoritmo de roteamento não rejeita nem
      sinaliza esse caso, ele só é detectado por este experimento.

      Cada trecho sintético recebe um tipo de via (RODOVIA/ARTERIAL/URBANA) sorteado
      independentemente por par ordenado, com velocidade e multiplicadores de
      consumo/desgaste próprios — isso evita que o experimento seja circular (se tudo
      fosse proporcional só à distância, as duas rotas seriam sempre idênticas).

      ## 2. Resultados agregados

      | Métrica | Valor |
      |---|---|
      | Rotas que mudaram entre Cenário A e B | %.1f%% |
      | Cargas inviáveis (peso ou volume > 100%% da capacidade) | %.1f%% |
      | Trials em que o volume (cubagem), não o peso, foi a restrição | %.1f%% |
      | Gap de custo — média | R$ %.2f (%.2f%%) |
      | Gap de custo — mediana | R$ %.2f (%.2f%%) |
      | Gap de custo — desvio padrão | R$ %.2f (%.2f pp) |
      | Gap de custo — mínimo | R$ %.2f (%.2f%%) |
      | Gap de custo — máximo | R$ %.2f (%.2f%%) |

      "Gap" = quanto a operação deixa de economizar, em R$, seguindo a rota do Cenário A
      em vez da rota do Cenário B — sempre ≥ 0, pois a rota do Cenário B é ótima por
      construção sob o próprio custo do Cenário B.

      "Cargas inviáveis" não são excluídas dos trials — elas continuam sendo roteadas
      normalmente (o `AStarWaypointOptimizer` não sabe nada sobre capacidade), o que é
      exatamente o ponto: hoje não existe nenhuma validação de capacidade na pipeline de
      produção, então esse percentual mede a exposição real a esse gap de funcionalidade.

      ## 3. Teste estatístico

      Wilcoxon signed-rank (pareado, custo do Cenário B avaliado na rota A vs na rota B,
      excluindo pares com diferença zero, aproximação normal para amostra grande):

      | | |
      |---|---|
      | Estatística (W) | %s |
      | p-valor | %s |

      %s

      ## 4. Correlações (Spearman) com o tamanho do gap (%%)

      | Variável | Correlação |
      |---|---|
      | Fração de peso ocupada (peso carga / capacidade kg) | %.3f |
      | Fração de volume ocupada (volume carga / capacidade m³) | %.3f |
      | Ocupação efetiva (a mais restritiva das duas acima) | %.3f |
      | Fração de trechos urbanos na rota | %.3f |
      | Número de waypoints | %.3f |

      ### 4.1 Detalhamento por número de waypoints

      %s

      ### 4.2 Detalhamento por nível de ocupação do veículo (peso ou volume, o que for maior)

      %s

      ## 5. Conclusão

      %s
      """,
      profile.label(), generatedAt, s.totalTrials(),
      profile.axleCount(), profile.capacityKg(), profile.capacityM3(), profile.baseFuelConsumptionLPer100Km(),
      profile.fuelPricePerLiter(), profile.tireReplacementCostPerTire(), profile.tireLifeKm(),
      profile.driverCostPerHourReais(),
      s.pctRoutesDiffer(), s.pctInfeasible(), s.pctVolumeBound(),
      s.gapReaisMean(), s.gapPercentMean(),
      s.gapReaisMedian(), s.gapPercentMedian(),
      s.gapReaisStd(), s.gapPercentStd(),
      s.gapReaisMin(), s.gapPercentMin(),
      s.gapReaisMax(), s.gapPercentMax(),
      formatStat(s.wilcoxonStatistic()), formatPValue(s.wilcoxonPValue()),
      interpretWilcoxon(s.wilcoxonPValue()),
      s.corrWeightFraction(), s.corrVolumeFraction(), s.corrOccupancyFraction(),
      s.corrUrbanFraction(), s.corrWaypointCount(),
      buildWaypointCountTable(s.byWaypointCount()),
      buildOccupancyLevelTable(s.byOccupancyLevel()),
      buildConclusion(s)
    );
  }

  private static String buildWaypointCountTable(java.util.List<WaypointCountBucket> buckets) {
    StringBuilder sb = new StringBuilder();
    sb.append("| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |\n");
    sb.append("|---|---|---|---|\n");
    for (WaypointCountBucket b : buckets) {
      sb.append(String.format(Locale.US, "| %d | %d | %.1f%% | %.2f%% |\n",
        b.n(), b.trialCount(), b.pctRoutesDiffer(), b.gapPercentMean()));
    }
    return sb.toString();
  }

  private static String buildOccupancyLevelTable(java.util.List<OccupancyLevelBucket> buckets) {
    StringBuilder sb = new StringBuilder();
    sb.append("| Ocupação (% da capacidade) | Trials | Rotas diferentes | Gap médio (%) |\n");
    sb.append("|---|---|---|---|\n");
    for (OccupancyLevelBucket b : buckets) {
      sb.append(String.format(Locale.US, "| %s | %d | %.1f%% | %.2f%% |\n",
        b.label(), b.trialCount(), b.pctRoutesDiffer(), b.gapPercentMean()));
    }
    return sb.toString();
  }

  private static String formatStat(double v) {
    return Double.isNaN(v) ? "N/A" : String.format(Locale.US, "%.2f", v);
  }

  private static String formatPValue(double v) {
    return Double.isNaN(v) ? "N/A" : String.format(Locale.US, "%.4f", v);
  }

  private static String interpretWilcoxon(double pValue) {
    if (Double.isNaN(pValue)) {
      return "Pares insuficientes com diferença não-nula para rodar o teste.";
    }
    return pValue < 0.05
      ? "p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral."
      : "p >= 0.05 — não há evidência estatística suficiente de que a diferença seja sistemática (pode ser ruído amostral).";
  }

  private static String buildConclusion(Summary s) {
    StringBuilder sb = new StringBuilder();

    sb.append("1. **Correlação com o custo:** ");
    double maxAbsCorr = Math.max(Math.abs(s.corrOccupancyFraction()),
      Math.max(Math.abs(s.corrUrbanFraction()), Math.abs(s.corrWaypointCount())));
    if (maxAbsCorr >= 0.3) {
      sb.append("pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.\n");
    } else {
      sb.append("nenhum dos indicadores testados mostrou correlação forte com o tamanho do gap nesta amostra.\n");
    }

    sb.append("2. **Muda a rota escolhida?** ");
    sb.append(String.format(Locale.US, "sim, em %.1f%% dos trials a ordem ótima mudou entre os dois cenários.\n", s.pctRoutesDiffer()));

    sb.append("3. **É estatisticamente relevante?** ");
    sb.append(Double.isNaN(s.wilcoxonPValue())
      ? "não foi possível testar (pares insuficientes).\n"
      : (s.wilcoxonPValue() < 0.05
        ? "sim — ver teste de Wilcoxon na seção 3.\n"
        : "não há evidência suficiente nesta amostra — ver teste de Wilcoxon na seção 3.\n"));

    sb.append("4. **Quais indicadores importam mais?** ");
    sb.append("ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.\n");

    sb.append("5. **Capacidade é respeitada?** ");
    sb.append(String.format(Locale.US,
      "não — o algoritmo não valida capacidade hoje; %.1f%% das cargas sorteadas excederam o peso e/ou o " +
      "volume máximo deste veículo e ainda assim foram roteadas normalmente.\n", s.pctInfeasible()));

    return sb.toString();
  }
}
