package com.routewise.validation;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Generates the tire-wear-by-axle-position report — the one dimension this refactor
 * actually changed. Documents the new per-position summation against the flat model it
 * replaced, and decomposes where each vehicle class's wear cost comes from.
 *
 * <p>Written by {@link RefactorMathReportSuite}; not run on its own.
 */
final class AxleTireWearDimensionReport {

  private static final double EDGE_KM = 100.0;
  private static final RoadType EDGE_ROAD = RoadType.ARTERIAL;

  private AxleTireWearDimensionReport() {}

  static void write(Path directory, List<VehicleProfile> profiles) {
    StringBuilder sb = new StringBuilder();
    appendSummary(sb);
    appendConstants(sb, profiles);
    appendBeforeAfter(sb, profiles);
    appendDecomposition(sb, profiles);
    appendLoadSensitivity(sb, profiles);
    MarkdownReportFile.write(directory.resolve("04-desgaste-pneu-por-eixo.md"), sb.toString());
  }

  private static void appendSummary(StringBuilder sb) {
    sb.append("# Desgaste de Pneu por Posição de Eixo\n\n");
    sb.append(MarkdownReportFile.generatedAt());
    sb.append("## Resumo do que foi feito\n\n");
    sb.append(
      "Esta era a **única lacuna real** entre as dimensões revisadas — as outras já estavam " +
      "implementadas. O `CostMatrixBuilder` calculava desgaste de pneu com uma vida útil única " +
      "multiplicada pelo número de **eixos**:\n\n" +
      "```\n" +
      "tireWearFraction = (1 / tireLifeKm) × axleCount × (1 + 0,5 × loadFactor) × wearMultiplier\n" +
      "```\n\n" +
      "Duas coisas estavam erradas aí:\n\n" +
      "1. **Todo eixo desgastava igual.** Dianteiro, tração e reboque têm desgaste fisicamente " +
      "diferente (esterçamento, torque, e só peso, respectivamente) — tratá-los como um número " +
      "médio apaga essa diferença.\n" +
      "2. **`axleCount` fazia o papel de quantidade de pneus.** Um caminhão de 3 eixos roda com " +
      "10 pneus, não 3. Como o resultado é multiplicado pelo custo de **um** pneu, o modelo " +
      "antigo subestimava o custo de pneu em cerca de 3x.\n\n" +
      "A fórmula passou a somar por posição, cada uma com sua vida útil e sua contagem real de pneus:\n\n" +
      "```\n" +
      "tireWearFraction = Σ_posição [ (1 / (tireLifeKm × fatorVida_posição)) × nºPneus_posição ]\n" +
      "                    × (1 + 0,5 × loadFactor) × wearMultiplier\n" +
      "```\n\n" +
      "Implementado em `AxlePosition` (fatores de vida), `AxleLayout` (contagem de pneus) e " +
      "`CostMatrixBuilder.baseTireWearPerKm`. Coberto por `AxleTireWearTest` (8 testes, `mvn test`).\n\n"
    );
  }

  private static void appendConstants(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append("## 1. Constantes do modelo\n\n");
    sb.append("### Fator de vida útil por posição\n\n");
    sb.append("Multiplicador sobre o `tireLifeKm` da classe. Abaixo de 1,0 = pneu dura menos.\n\n");
    sb.append("| Posição | Fator | Razão |\n|---|---|---|\n");
    sb.append("| `DIANTEIRO` | ").append(fmt(AxlePosition.DIANTEIRO.lifeFactor))
      .append(" | Esterçamento e sensibilidade a alinhamento |\n");
    sb.append("| `TRACAO` | ").append(fmt(AxlePosition.TRACAO.lifeFactor))
      .append(" | Referência — absorve o torque do motor |\n");
    sb.append("| `REBOQUE` | ").append(fmt(AxlePosition.REBOQUE.lifeFactor))
      .append(" | Só carrega peso, sem torque nem esterçamento |\n\n");
    sb.append(
      "`TRACAO` é a referência (1,00) porque o log empírico em " +
      "`empirical-data/tire-replacements-by-vehicle-type.csv` é quase todo de eixo de tração — " +
      "ancorar ali mantém a calibração empírica existente válida sem reinterpretá-la contra " +
      "outra base. Os outros dois fatores são suposições plausíveis documentadas, mesmo caveat " +
      "de todas as constantes de `VehicleProfile`: nenhuma telemetria de frota os separa ainda.\n\n"
    );

    sb.append("### Configuração de pneus por classe\n\n");
    sb.append("| Perfil | Eixos | Dianteiro | Tração | Reboque | Total de pneus |\n");
    sb.append("|---|---|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      AxleLayout l = p.axleLayout();
      sb.append(String.format(Locale.US, "| %s | %d | %d | %d | %d | **%d** |\n",
        p.label(), p.axleCount(), l.dianteiro(), l.tracao(), l.reboque(), l.totalTires()));
    }
    sb.append("\nConfiguração brasileira padrão: eixo direcional simples (2 pneus) e eixos " +
      "traseiros em rodado duplo (4 pneus cada). Fixa por classe — calibração empírica muda " +
      "quanto um pneu dura, nunca quantos pneus o caminhão tem.\n\n");
  }

  private static void appendBeforeAfter(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append(String.format(Locale.US,
      "## 2. Modelo anterior vs. atual (trecho de %.0f km, via %s, vazio)\n\n", EDGE_KM, EDGE_ROAD));
    sb.append(
      "A coluna \"anterior\" reconstrói a fórmula antiga apenas para comparação — ela não " +
      "existe mais no código.\n\n"
    );
    sb.append("| Perfil | Anterior (R$) | Atual (R$) | Diferença | Fator |\n");
    sb.append("|---|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      double oldWearPerKm = p.axleCount() / p.tireLifeKm();
      double oldCost = EDGE_KM * oldWearPerKm * p.tireReplacementCostPerTire();
      double newCost = EDGE_KM * CostMatrixBuilder.baseTireWearPerKm(p) * p.tireReplacementCostPerTire();
      sb.append(String.format(Locale.US, "| %s | R$ %.2f | R$ %.2f | +R$ %.2f | %.2fx |\n",
        p.label(), oldCost, newCost, newCost - oldCost, newCost / oldCost));
    }
    sb.append("\nO salto vem quase todo da troca de `axleCount` por contagem real de pneus — " +
      "o fator de cada classe fica perto da sua razão pneus/eixos (leve 6/2 = 3,0; médio " +
      "10/3 = 3,3; pesado 18/5 = 3,6). Os fatores de vida por posição modulam isso nos dois " +
      "sentidos: o eixo dianteiro empurra o fator para cima (dura 15% menos), enquanto os 8 " +
      "pneus de reboque do perfil pesado o puxam para baixo (duram 25% mais), e é por isso que " +
      "o pesado sobe menos que o médio apesar de ter mais pneus por eixo.\n\n");
  }

  private static void appendDecomposition(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append(String.format(Locale.US,
      "## 3. De onde vem o desgaste (trecho de %.0f km, via %s, vazio)\n\n", EDGE_KM, EDGE_ROAD));
    sb.append("| Perfil | Posição | Pneus | Vida útil efetiva | Custo (R$) | % do total |\n");
    sb.append("|---|---|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      double total = EDGE_KM * CostMatrixBuilder.baseTireWearPerKm(p) * p.tireReplacementCostPerTire();
      for (AxlePosition position : AxlePosition.values()) {
        int tires = p.axleLayout().tireCount(position);
        if (tires == 0) {
          continue;
        }
        double effectiveLifeKm = p.tireLifeKm() * position.lifeFactor;
        double cost = EDGE_KM * (tires / effectiveLifeKm) * p.tireReplacementCostPerTire();
        sb.append(String.format(Locale.US, "| %s | `%s` | %d | %,.0f km | R$ %.2f | %.1f%% |\n",
          p.label(), position, tires, effectiveLifeKm, cost, cost / total * 100));
      }
    }
    sb.append("\nO eixo de tração é a maior parcela em todas as classes — é onde estão a " +
      "maioria dos pneus — mas no perfil pesado ele fica abaixo de 50%, porque os 8 pneus de " +
      "reboque respondem por outros ~38%. O dianteiro pesa pouco no total apesar de ser o que " +
      "desgasta mais rápido por pneu, porque são só 2.\n\n");
  }

  private static void appendLoadSensitivity(StringBuilder sb, List<VehicleProfile> profiles) {
    sb.append("## 4. Sensibilidade à carga e ao tipo de via\n\n");
    sb.append(String.format(Locale.US,
      "Custo de pneu num trecho de %.0f km, variando ocupação e tipo de via.\n\n", EDGE_KM));
    sb.append("| Perfil | Ocupação | Rodovia | Arterial | Urbana |\n");
    sb.append("|---|---|---|---|---|\n");
    for (VehicleProfile p : profiles) {
      for (double load : new double[] {0.0, 0.5, 1.0}) {
        sb.append(String.format(Locale.US, "| %s | %.0f%% |", p.label(), load * 100));
        for (RoadType road : RoadType.values()) {
          double cost = EDGE_KM * CostMatrixBuilder.baseTireWearPerKm(p)
            * (1 + 0.5 * load) * road.wearMultiplier * p.tireReplacementCostPerTire();
          sb.append(String.format(Locale.US, " R$ %.2f |", cost));
        }
        sb.append('\n');
      }
    }
    sb.append("\nCarga cheia acrescenta 50% ao desgaste; via urbana acrescenta 40% sobre a " +
      "arterial. Combinados, um trecho urbano carregado desgasta **2,6x** o que o mesmo trecho " +
      "em rodovia vazio desgasta (2,1x se a comparação for contra a arterial vazia) — a razão " +
      "de o custo de pneu conseguir mudar a rota escolhida.\n");
  }

  private static String fmt(double value) {
    return String.format(Locale.US, "%.2f", value);
  }
}
