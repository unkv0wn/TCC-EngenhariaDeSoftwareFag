# Validação Empírica do Custo de Rota — Comparação entre Perfis de Veículo

Os mesmos 500 cenários sintéticos de rota foram avaliados para cada perfil de veículo abaixo — só o veículo muda entre as colunas.

## Comparação entre perfis

| Perfil | Rotas diferentes | Gap médio (R$) | Gap médio (%) | Wilcoxon p-valor |
|---|---|---|---|---|
| Leve (2 eixos) | 27.2% | R$ 2.27 | 0.80% | 0.0000 |
| Médio (3 eixos) | 29.4% | R$ 3.69 | 0.96% | 0.0000 |
| Pesado (5 eixos) | 34.4% | R$ 8.20 | 1.32% | 0.0000 |

---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Leve (2 eixos))

Gerado em: 2026-07-30 22:20:39
Trials: 500

## 1. Metodologia e suposições

Experimento sintético — nenhum dado é telemetria real de frota. Cenário A replica
o comportamento atual de produção (minimizar apenas duração). Cenário B minimiza
um custo combinado em R$ (tempo + combustível + desgaste de pneu), usando o
`AStarWaypointOptimizer` já existente, sem alterações, em ambos os casos.

Perfil de veículo assumido (constante, fixo para todos os trials):

| Parâmetro | Valor |
|---|---|
| Eixos | 2 |
| Capacidade (peso) | 1500 kg |
| Capacidade (volume) | 8.0 m³ |
| Consumo base | 10.0 L/100km |
| Preço do litro | R$ 6.10 |
| Custo de reposição do pneu | R$ 900.00 |
| Vida útil do pneu | 50000 km |
| Custo-hora do motorista | R$ 30.00 |

A carga de cada trial é sorteada em kg e m³ de forma independente da capacidade
do veículo (mesma faixa bruta reaproveitada entre perfis, para comparação justa —
ver {@code ScenarioGenerator}). A ocupação efetiva usada nas fórmulas de consumo e
desgaste é `min(max(peso/capacidadeKg, volume/capacidadeM3), 1)` — ou seja, a
dimensão mais restritiva (peso OU cubagem) é que manda, como na prática real de
frete. Cargas cuja ocupação bruta excede 100% em qualquer dimensão são marcadas
como inviáveis (ver seção 2) — hoje o algoritmo de roteamento não rejeita nem
sinaliza esse caso, ele só é detectado por este experimento.

Cada trecho sintético recebe um tipo de via (RODOVIA/ARTERIAL/URBANA) sorteado
independentemente por par ordenado, com velocidade e multiplicadores de
consumo/desgaste próprios — isso evita que o experimento seja circular (se tudo
fosse proporcional só à distância, as duas rotas seriam sempre idênticas).

## 2. Resultados agregados

| Métrica | Valor |
|---|---|
| Rotas que mudaram entre Cenário A e B | 27.2% |
| Cargas inviáveis (peso ou volume > 100% da capacidade) | 92.6% |
| Trials em que o volume (cubagem), não o peso, foi a restrição | 53.8% |
| Gap de custo — média | R$ 2.27 (0.80%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 5.53 (1.83 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 40.77 (11.45%) |

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
| Estatística (W) | 9316.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fração de peso ocupada (peso carga / capacidade kg) | -0.073 |
| Fração de volume ocupada (volume carga / capacidade m³) | -0.131 |
| Ocupação efetiva (a mais restritiva das duas acima) | -0.127 |
| Fração de trechos urbanos na rota | 0.161 |
| Número de waypoints | 0.431 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 54 | 0.0% | 0.00% |
| 3 | 50 | 2.0% | 0.02% |
| 4 | 65 | 7.7% | 0.25% |
| 5 | 63 | 12.7% | 0.33% |
| 6 | 48 | 37.5% | 1.11% |
| 7 | 66 | 43.9% | 1.56% |
| 8 | 37 | 45.9% | 1.10% |
| 9 | 50 | 44.0% | 1.12% |
| 10 | 67 | 53.7% | 1.62% |


### 4.2 Detalhamento por nível de ocupação do veículo (peso ou volume, o que for maior)

| Ocupação (% da capacidade) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 0–25% | 2 | 0.0% | 0.00% |
| 25–50% | 3 | 33.3% | 1.24% |
| 50–75% | 19 | 31.6% | 1.28% |
| 75–100% | 13 | 46.2% | 1.57% |
| > 100% (inviável) | 463 | 26.6% | 0.76% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 27.2% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.
5. **Capacidade é respeitada?** não — o algoritmo não valida capacidade hoje; 92.6% das cargas sorteadas excederam o peso e/ou o volume máximo deste veículo e ainda assim foram roteadas normalmente.


---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Médio (3 eixos))

Gerado em: 2026-07-30 22:20:39
Trials: 500

## 1. Metodologia e suposições

Experimento sintético — nenhum dado é telemetria real de frota. Cenário A replica
o comportamento atual de produção (minimizar apenas duração). Cenário B minimiza
um custo combinado em R$ (tempo + combustível + desgaste de pneu), usando o
`AStarWaypointOptimizer` já existente, sem alterações, em ambos os casos.

Perfil de veículo assumido (constante, fixo para todos os trials):

| Parâmetro | Valor |
|---|---|
| Eixos | 3 |
| Capacidade (peso) | 4000 kg |
| Capacidade (volume) | 25.0 m³ |
| Consumo base | 15.0 L/100km |
| Preço do litro | R$ 6.10 |
| Custo de reposição do pneu | R$ 1800.00 |
| Vida útil do pneu | 60000 km |
| Custo-hora do motorista | R$ 35.00 |

A carga de cada trial é sorteada em kg e m³ de forma independente da capacidade
do veículo (mesma faixa bruta reaproveitada entre perfis, para comparação justa —
ver {@code ScenarioGenerator}). A ocupação efetiva usada nas fórmulas de consumo e
desgaste é `min(max(peso/capacidadeKg, volume/capacidadeM3), 1)` — ou seja, a
dimensão mais restritiva (peso OU cubagem) é que manda, como na prática real de
frete. Cargas cuja ocupação bruta excede 100% em qualquer dimensão são marcadas
como inviáveis (ver seção 2) — hoje o algoritmo de roteamento não rejeita nem
sinaliza esse caso, ele só é detectado por este experimento.

Cada trecho sintético recebe um tipo de via (RODOVIA/ARTERIAL/URBANA) sorteado
independentemente por par ordenado, com velocidade e multiplicadores de
consumo/desgaste próprios — isso evita que o experimento seja circular (se tudo
fosse proporcional só à distância, as duas rotas seriam sempre idênticas).

## 2. Resultados agregados

| Métrica | Valor |
|---|---|
| Rotas que mudaram entre Cenário A e B | 29.4% |
| Cargas inviáveis (peso ou volume > 100% da capacidade) | 20.2% |
| Trials em que o volume (cubagem), não o peso, foi a restrição | 46.6% |
| Gap de custo — média | R$ 3.69 (0.96%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 8.46 (2.06 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 65.03 (12.29%) |

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
| Estatística (W) | 10731.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fração de peso ocupada (peso carga / capacidade kg) | -0.057 |
| Fração de volume ocupada (volume carga / capacidade m³) | -0.099 |
| Ocupação efetiva (a mais restritiva das duas acima) | -0.099 |
| Fração de trechos urbanos na rota | 0.182 |
| Número de waypoints | 0.439 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 54 | 0.0% | 0.00% |
| 3 | 50 | 4.0% | 0.02% |
| 4 | 65 | 9.2% | 0.30% |
| 5 | 63 | 15.9% | 0.41% |
| 6 | 48 | 39.6% | 1.33% |
| 7 | 66 | 48.5% | 1.86% |
| 8 | 37 | 51.4% | 1.38% |
| 9 | 50 | 44.0% | 1.35% |
| 10 | 67 | 55.2% | 1.94% |


### 4.2 Detalhamento por nível de ocupação do veículo (peso ou volume, o que for maior)

| Ocupação (% da capacidade) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 0–25% | 15 | 26.7% | 1.37% |
| 25–50% | 83 | 37.3% | 1.26% |
| 50–75% | 129 | 33.3% | 1.06% |
| 75–100% | 172 | 25.6% | 0.83% |
| > 100% (inviável) | 101 | 24.8% | 0.76% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 29.4% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.
5. **Capacidade é respeitada?** não — o algoritmo não valida capacidade hoje; 20.2% das cargas sorteadas excederam o peso e/ou o volume máximo deste veículo e ainda assim foram roteadas normalmente.


---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Pesado (5 eixos))

Gerado em: 2026-07-30 22:20:39
Trials: 500

## 1. Metodologia e suposições

Experimento sintético — nenhum dado é telemetria real de frota. Cenário A replica
o comportamento atual de produção (minimizar apenas duração). Cenário B minimiza
um custo combinado em R$ (tempo + combustível + desgaste de pneu), usando o
`AStarWaypointOptimizer` já existente, sem alterações, em ambos os casos.

Perfil de veículo assumido (constante, fixo para todos os trials):

| Parâmetro | Valor |
|---|---|
| Eixos | 5 |
| Capacidade (peso) | 12000 kg |
| Capacidade (volume) | 90.0 m³ |
| Consumo base | 32.0 L/100km |
| Preço do litro | R$ 6.10 |
| Custo de reposição do pneu | R$ 2200.00 |
| Vida útil do pneu | 80000 km |
| Custo-hora do motorista | R$ 42.00 |

A carga de cada trial é sorteada em kg e m³ de forma independente da capacidade
do veículo (mesma faixa bruta reaproveitada entre perfis, para comparação justa —
ver {@code ScenarioGenerator}). A ocupação efetiva usada nas fórmulas de consumo e
desgaste é `min(max(peso/capacidadeKg, volume/capacidadeM3), 1)` — ou seja, a
dimensão mais restritiva (peso OU cubagem) é que manda, como na prática real de
frete. Cargas cuja ocupação bruta excede 100% em qualquer dimensão são marcadas
como inviáveis (ver seção 2) — hoje o algoritmo de roteamento não rejeita nem
sinaliza esse caso, ele só é detectado por este experimento.

Cada trecho sintético recebe um tipo de via (RODOVIA/ARTERIAL/URBANA) sorteado
independentemente por par ordenado, com velocidade e multiplicadores de
consumo/desgaste próprios — isso evita que o experimento seja circular (se tudo
fosse proporcional só à distância, as duas rotas seriam sempre idênticas).

## 2. Resultados agregados

| Métrica | Valor |
|---|---|
| Rotas que mudaram entre Cenário A e B | 34.4% |
| Cargas inviáveis (peso ou volume > 100% da capacidade) | 0.0% |
| Trials em que o volume (cubagem), não o peso, foi a restrição | 35.2% |
| Gap de custo — média | R$ 8.20 (1.32%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 16.94 (2.56 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 115.64 (14.66%) |

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
| Estatística (W) | 14878.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fração de peso ocupada (peso carga / capacidade kg) | -0.053 |
| Fração de volume ocupada (volume carga / capacidade m³) | -0.092 |
| Ocupação efetiva (a mais restritiva das duas acima) | -0.096 |
| Fração de trechos urbanos na rota | 0.192 |
| Número de waypoints | 0.469 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 54 | 0.0% | 0.00% |
| 3 | 50 | 2.0% | 0.02% |
| 4 | 65 | 13.8% | 0.44% |
| 5 | 63 | 20.6% | 0.62% |
| 6 | 48 | 41.7% | 1.82% |
| 7 | 66 | 62.1% | 2.52% |
| 8 | 37 | 56.8% | 1.98% |
| 9 | 50 | 56.0% | 1.85% |
| 10 | 67 | 58.2% | 2.56% |


### 4.2 Detalhamento por nível de ocupação do veículo (peso ou volume, o que for maior)

| Ocupação (% da capacidade) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 0–25% | 270 | 38.5% | 1.55% |
| 25–50% | 230 | 29.6% | 1.05% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 34.4% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.
5. **Capacidade é respeitada?** não — o algoritmo não valida capacidade hoje; 0.0% das cargas sorteadas excederam o peso e/ou o volume máximo deste veículo e ainda assim foram roteadas normalmente.


---

