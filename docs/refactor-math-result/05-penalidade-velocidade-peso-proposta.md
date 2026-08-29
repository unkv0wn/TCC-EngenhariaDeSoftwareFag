# Penalidade de Velocidade por Peso — Proposta (ainda não implementada)

Escrito em: 2026-08-07
Status: **proposta em discussão** — não existe no código ainda. Este documento registra o
problema encontrado, a fórmula candidata (em duas variantes) e um exemplo numérico, para
decidir qual variante implementar e por qual caminho (ver Seção 4).

## 1. Problema encontrado

Em `CostMatrixBuilder.build()` (`backend/src/main/java/com/routewise/validation/CostMatrixBuilder.java`,
linha 79):

```java
double duration = distanceKm / roadType.avgSpeedKmh * 3600.0;
```

`duration` depende só de `distanceKm` e `roadType.avgSpeedKmh` — **nunca** de
`VehicleProfile` nem de `loadFactor`. Resultado: `light()`, `medium()` e `heavy()` levam
**exatamente o mesmo tempo** no mesmo trecho, não importa o peso do veículo ou da carga.
Isso é fisicamente questionável — caminhão pesado carregado acelera, freia e sobe ladeira
mais devagar que uma van vazia na mesma via — e é o comportamento que
`EmpiricalTruckComparisonTest` hoje **documenta e assert-a** como esperado (porque
espelha fielmente o que a produção faz: o OSRM também não recebe peso do veículo).

`loadFactor` já é usado em `fuelLiters` e `tireWearReais` (Seção 5.1 de
`docs/MATH_VALIDATION_REVIEW.md`) — a assimetria é que ele nunca alcança `duration`.

## 2. Fórmula matemática candidata

Ideia: substituir `roadType.avgSpeedKmh` por uma **velocidade efetiva**, reduzida em
função do `loadFactor` do cenário, mantendo a mesma estrutura de "constante documentada,
não calibrada" já usada em `LOAD_FUEL_FACTOR` (0,30) e `LOAD_WEAR_FACTOR` (0,50).

$$
\text{effectiveSpeedKmh}_{ij} = \text{roadType}_{ij}.\text{avgSpeedKmh} \times (1 - k \cdot \text{loadFactor})
$$

$$
\text{durationSec}_{ij} = \frac{\text{distanceKm}_{ij}}{\text{effectiveSpeedKmh}_{ij}} \times 3600
$$

Duas variantes para $k$:

### Variante A — constante única

$$
k = 0{,}15 \quad \text{(mesmo } k \text{ para RODOVIA, ARTERIAL, URBANA — ilustrativo, não calibrado)}
$$

Mais simples de justificar (1 constante nova), mas ignora que peso pesa mais em
arranca-e-para urbano do que em velocidade de cruzeiro na rodovia.

### Variante B — constante por tipo de via

$$
k_{\text{roadType}} =
\begin{cases}
0{,}10 & \text{RODOVIA (cruzeiro, menos sensível a peso em via plana)} \\
0{,}15 & \text{ARTERIAL} \\
0{,}25 & \text{URBANA (parada/arranque frequente, mais sensível a peso)}
\end{cases}
$$

Análogo ao padrão que `RoadType.fuelMultiplier`/`wearMultiplier` já usam (3 constantes
por tipo de via). Mais realista fisicamente, mas mais suposição pra documentar.

**Observação em aberto:** `loadFactor` é uma fração *relativa* à capacidade do próprio
veículo (`cargoWeightKg / capacityKg`), não o peso absoluto. Isso significa que, com a
mesma carga, o perfil `heavy()` (capacidade maior) recebe uma penalidade *menor* que o
`light()` — mesmo carregando fisicamente mais massa no chassi. O exemplo da Seção 3
mostra esse efeito. Se o objetivo é capturar "caminhão mais pesado é mais lento em
termos absolutos", talvez a penalidade devesse depender também do peso vazio do veículo
(não modelado hoje em `VehicleProfile`), não só do `loadFactor` relativo — vale decidir
isso antes de implementar.

## 3. Exemplo numérico

### 3.1 Trecho único — 20 km, via ARTERIAL, mesma carga do exemplo anterior (1000 kg / 5 m³)

Baseline = fórmula atual (sem penalidade). Variante A usa $k=0{,}15$ fixo. Como o trecho
é ARTERIAL, Variante B coincide com Variante A neste exemplo específico ($k_{\text{ARTERIAL}}=0{,}15$
nas duas) — a diferença entre as variantes só aparece variando o tipo de via (Seção 3.2).

| Perfil | loadFactor | Baseline (sem penalidade) | Com penalidade ($k=0{,}15$) | Δ tempo |
|---|---|---|---|---|
| Leve | 0,667 | 1440 s (24,00 min) | effSpeed 45,00 km/h → 1600,0 s (26,67 min) | +160,0 s (+11,1%) |
| Médio | 0,250 | 1440 s (24,00 min) | effSpeed 48,13 km/h → 1496,1 s (24,94 min) | +56,1 s (+3,9%) |
| Pesado | 0,083 | 1440 s (24,00 min) | effSpeed 49,38 km/h → 1458,0 s (24,30 min) | +18,0 s (+1,3%) |

Repare no efeito contraintuitivo citado na Seção 2: o perfil **leve** é o mais penalizado
em tempo, porque a mesma carga de 1000 kg ocupa 66,7% da sua capacidade contra só 8,3% da
capacidade do pesado — é exatamente a observação em aberto sobre `loadFactor` relativo
vs. peso absoluto.

### 3.2 Sensibilidade por tipo de via (Variante B) — perfil médio, trecho de 100 km

| Tipo de via | avgSpeedKmh | $k$ | loadFactor 0% | loadFactor 50% | loadFactor 100% | Δ (0%→100%) |
|---|---|---|---|---|---|---|
| RODOVIA | 80,0 | 0,10 | 75,0 min | 78,9 min | 83,3 min | +8,3 min (+11,1%) |
| ARTERIAL | 50,0 | 0,15 | 120,0 min | 129,7 min | 141,2 min | +21,2 min (+17,6%) |
| URBANA | 25,0 | 0,25 | 240,0 min | 274,3 min | 320,0 min | +80,0 min (+33,3%) |

A Variante B captura o que a Variante A não consegue: carga cheia custa proporcionalmente
mais tempo em via urbana (+33,3%) do que em rodovia (+11,1%) — plausível fisicamente
(arranca/freia em semáforo penaliza mais um veículo pesado do que manter velocidade de
cruzeiro numa reta). A Variante A, por usar o mesmo $k=0{,}15$ nas três vias, produziria a
mesma variação percentual (+15% no denominador da velocidade) em RODOVIA, ARTERIAL e
URBANA — perde essa diferenciação por tipo de via.

## 4. Caminhos de implementação em aberto

Duas formas de trazer isso pro código sem quebrar a garantia de que "Cenário A" continua
espelhando produção fielmente (produção/OSRM não recebe peso do veículo, então `costA`
não pode passar a depender dele):

1. **Candidato via `MarginalContributionAnalyzer`** (mesmo padrão de
   `TollCostAdditionExample`/`NewIndicatorAdditionExample`): expressar o tempo extra como
   componente em R$ (`tempoExtraSec/3600 × driverCostPerHourReais`) somado só ao
   `costB`, testado por Wilcoxon/Spearman antes de virar parte "oficial" do modelo.
2. **Alterar `CostMatrixBuilder.duration` só para o Cenário B**, mantendo `costA`
   (espelho de produção) intocado — mais direto, mas quebra a simetria atual onde
   `timeCostReais` deriva do mesmo `durationSec` usado por `costA`.

Nenhuma das duas foi implementada ainda — este documento serve pra decidir a fórmula e o
caminho antes de mexer em código.
