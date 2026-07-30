# Validação Algorítmica de Dados Empíricos no Custo de Rota — Design Spec

Data: 2026-07-29
Escopo: `backend/` (novo pacote `com.routewise.validation`)

## Contexto

O `AStarWaypointOptimizer` hoje minimiza uma matriz N×N de custo puramente baseada em
duração (segundos, vinda do OSRM `/table`). O algoritmo é agnóstico ao significado do
"custo" — ele só minimiza o que a matriz contém, então nada nele precisa mudar para
suportar outras funções de custo.

O objetivo real deste trabalho, antes de qualquer mudança em produção, é validar se
faz sentido considerar dados empíricos de operação (desgaste de pneu, consumo de
combustível, quilometragem, capacidade do veículo) na função de custo. Esta spec cobre
**só a validação** — um script standalone que roda um experimento estatístico e produz
um relatório. Não altera `RouteController`, `RouteOptimizerServiceImpl` nem o frontend.

Uma eventual segunda etapa (integrar a função de custo validada na pipeline de produção)
fica fora de escopo aqui e vira uma spec própria se a validação indicar que vale a pena.

## Perguntas que o experimento precisa responder

1. Os indicadores empíricos têm correlação significativa com o custo total da operação?
2. A inclusão deles muda a rota escolhida pelo algoritmo?
3. A diferença observada é estatisticamente relevante ou é ruído?
4. Quais indicadores pesam mais no resultado?

## Cenário A vs Cenário B

- **Cenário A (baseline):** exatamente o que a produção faz hoje — minimizar só a
  duração. Não é uma invenção nova para o experimento; é o comportamento atual do
  sistema.
- **Cenário B (combinado):** matriz de custo em **R$**, por trecho `(i, j)`:

  ```
  custoTempo(i,j)     = duração_h(i,j) × custoHoraMotorista
  custoCombustível(i,j) = distância_km(i,j) × consumoAjustado(i,j) × preçoLitro
  custoDesgastePneu(i,j) = distância_km(i,j) × (1 / vidaÚtilKm) × nºEixos
                           × fatorCarga × preçoPneu

  custoTotalB(i,j) = custoTempo(i,j) + custoCombustível(i,j) + custoDesgastePneu(i,j)
  ```

  `consumoAjustado` = consumo base (L/100km) ajustado por carga (`% capacidade`) e por
  multiplicador do tipo de via (ver seção seguinte).

Nenhum desses números é dado de campo real (não há telemetria de frota disponível) —
são fórmulas com constantes assumidas e **documentadas explicitamente como suposições**
no relatório final. Isso é adequado para esta etapa: valida a lógica algorítmica, não
substitui uma calibração com dados reais (que seria um passo futuro).

## Geração dos cenários (sintéticos)

Cada trial gera, aleatoriamente:

- `n` waypoints, uniforme em `[2, 10]` (mesmo limite da produção).
- Posições 2D aleatórias num raio fixo (ex: 50km × 50km), com distância euclidiana em
  km entre cada par — vira a "distância real" simulada da matriz.
- Um **tipo de via** por trecho `(i,j)`, sorteado entre `RODOVIA`, `ARTERIAL`, `URBANA`,
  cada um com velocidade média e multiplicador de consumo/desgaste próprios (rodovia:
  mais rápida, mais eficiente por km; urbana: mais lenta, mais desgaste/consumo por km).
  Isso evita que o experimento seja trivialmente circular — se tudo fosse proporcional
  só à distância, Cenário A e B sempre escolheriam a mesma rota.
- Um **fator de carga** do veículo (`cargaAtual / capacidade`, 0–100%), sorteado por
  trial — usado depois na análise de correlação.

Constantes fixas (não sorteadas, documentadas no relatório como suposições): nº de
eixos, capacidade do veículo, consumo base, preço do litro, preço do pneu, vida útil do
pneu, custo-hora do motorista, multiplicadores de velocidade/consumo/desgaste por tipo
de via.

## O que cada trial mede

1. Roda `AStarWaypointOptimizer.optimize()` com a matriz do Cenário A → `ordemA`.
2. Roda de novo com a matriz do Cenário B → `ordemB`.
3. `rotasDiferem = ordemA != ordemB`.
4. Avalia **ambas as ordens** sob a função de custo completa do Cenário B:
   `custoB(ordemA)` e `custoB(ordemB)` (este último é o mínimo, por construção).
5. `gap = custoB(ordemA) - custoB(ordemB)` — quanto dinheiro "fica na mesa" por trial
   se a operação ignorar os fatores empíricos. Sempre ≥ 0.
6. Guarda também: distância total, duração total, litros de combustível, custo de
   desgaste, carga%, % de trechos urbanos — para a etapa de correlação.

## Agregação estatística (N = 500 trials)

- `% de trials com rotasDiferem = true`.
- Estatística descritiva do `gap` (em R$ e em % do `custoB(ordemA)`): média, mediana,
  desvio padrão, mínimo, máximo.
- **Teste de Wilcoxon signed-rank** sobre o `gap` (via Apache Commons Math —
  `org.apache.commons:commons-math3`, única dependência nova no `pom.xml`). Como o
  `gap` é ≥ 0 por construção, o teste não serve para provar "existe diferença" (isso é
  trivial) — serve para reportar a magnitude/consistência do efeito de forma rigorosa,
  não achismo.
- **Correlação de Spearman** entre `gap%` e: fator de carga, % de trechos urbanos, `n`
  waypoints — aponta qual variável mais influencia o tamanho do efeito (responde à
  pergunta 4).

## Estrutura de código

Novo pacote `backend/src/main/java/com/routewise/validation/`:

| Classe | Responsabilidade |
|---|---|
| `VehicleProfile` | record com as constantes do veículo (eixos, capacidade, consumo, preços) |
| `RoadType` | enum (RODOVIA/ARTERIAL/URBANA) com multiplicadores de velocidade/consumo/desgaste |
| `ScenarioGenerator` | gera waypoints sintéticos + matriz de distância + tipo de via por trecho + carga do trial |
| `CostMatrixBuilder` | constrói a matriz do Cenário A (duração) e do Cenário B (R$ combinado) a partir do cenário gerado |
| `TrialResult` | record com as métricas de um trial (ordens, gap, distância, combustível, etc.) |
| `StatisticalAnalyzer` | agrega a lista de `TrialResult` → estatística descritiva, Wilcoxon, correlações |
| `ReportWriter` | escreve o relatório final em Markdown |
| `EmpiricalCostValidationExperiment` | classe com `main()`, orquestra tudo |

Cada classe tem uma responsabilidade e pode ser entendida/testada isoladamente.
`AStarWaypointOptimizer` e `HaversineUtil` (já existentes) são reaproveitados sem
alteração.

## Execução e output

- Roda via `mvn compile exec:java -Dexec.mainClass=com.routewise.validation.EmpiricalCostValidationExperiment`
  (`exec-maven-plugin` adicionado ao `pom.xml` só para isso).
- Relatório escrito em `docs/validation-reports/empirical-cost-validation.md`
  (sobrescrito a cada execução), contendo: metodologia e suposições assumidas,
  resultados agregados, resultado do teste estatístico, correlações, e uma conclusão
  objetiva em relação às 4 perguntas do topo desta spec.

## Fora de escopo

- Qualquer alteração em `RouteController`, `RouteOptimizerServiceImpl`, DTOs de
  produção ou no frontend.
- Dados reais de telemetria de frota (não disponíveis nesta etapa).
- Calibração final de pesos para uso em produção — depende do resultado deste
  experimento indicar que vale a pena.
