# Validação Empírica do Custo de Rota — Comparação entre Perfis de Veículo

Os mesmos 500 cenários sintéticos de rota foram avaliados para cada perfil de veículo abaixo — só o veículo muda entre as colunas.

## Comparação entre perfis

| Perfil | Rotas diferentes | Gap médio (R$) | Gap médio (%) | Wilcoxon p-valor |
|---|---|---|---|---|
| Leve (2 eixos) | 28.0% | R$ 2.63 | 0.86% | 0.0000 |
| Médio (3 eixos) | 31.2% | R$ 4.69 | 1.06% | 0.0000 |
| Pesado (5 eixos) | 34.8% | R$ 9.48 | 1.36% | 0.0000 |

---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Leve (2 eixos))

Gerado em: 2026-08-05 00:58:33
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
ver `ScenarioGenerator`). A ocupação efetiva usada nas fórmulas de consumo e
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
| Rotas que mudaram entre Cenário A e B | 28.0% |
| Cargas inviáveis (peso ou volume > 100% da capacidade) | 92.6% |
| Trials em que o volume (cubagem), não o peso, foi a restrição | 53.8% |
| Gap de custo — média | R$ 2.63 (0.86%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 6.27 (1.91 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 45.80 (11.85%) |

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
| Estatística (W) | 9870.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fração de peso ocupada (peso carga / capacidade kg) | -0.068 |
| Fração de volume ocupada (volume carga / capacidade m³) | -0.120 |
| Ocupação efetiva (a mais restritiva das duas acima) | -0.121 |
| Fração de trechos urbanos na rota | 0.170 |
| Número de waypoints | 0.430 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 54 | 0.0% | 0.00% |
| 3 | 50 | 4.0% | 0.02% |
| 4 | 65 | 7.7% | 0.27% |
| 5 | 63 | 12.7% | 0.35% |
| 6 | 48 | 39.6% | 1.19% |
| 7 | 66 | 45.5% | 1.66% |
| 8 | 37 | 48.6% | 1.18% |
| 9 | 50 | 44.0% | 1.21% |
| 10 | 67 | 53.7% | 1.73% |


### 4.2 Detalhamento por nível de ocupação do veículo (peso ou volume, o que for maior)

| Ocupação (% da capacidade) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 0–25% | 2 | 0.0% | 0.00% |
| 25–50% | 3 | 33.3% | 1.32% |
| 50–75% | 19 | 31.6% | 1.34% |
| 75–100% | 13 | 46.2% | 1.65% |
| > 100% (inviável) | 463 | 27.4% | 0.81% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 28.0% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.
5. **Capacidade é respeitada?** não — o algoritmo não valida capacidade hoje; 92.6% das cargas sorteadas excederam o peso e/ou o volume máximo deste veículo e ainda assim foram roteadas normalmente.


---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Médio (3 eixos))

Gerado em: 2026-08-05 00:58:33
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
ver `ScenarioGenerator`). A ocupação efetiva usada nas fórmulas de consumo e
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
| Rotas que mudaram entre Cenário A e B | 31.2% |
| Cargas inviáveis (peso ou volume > 100% da capacidade) | 20.2% |
| Trials em que o volume (cubagem), não o peso, foi a restrição | 46.6% |
| Gap de custo — média | R$ 4.69 (1.06%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 10.42 (2.19 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 79.20 (12.86%) |

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
| Estatística (W) | 12246.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fração de peso ocupada (peso carga / capacidade kg) | -0.059 |
| Fração de volume ocupada (volume carga / capacidade m³) | -0.102 |
| Ocupação efetiva (a mais restritiva das duas acima) | -0.102 |
| Fração de trechos urbanos na rota | 0.189 |
| Número de waypoints | 0.439 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 54 | 0.0% | 0.00% |
| 3 | 50 | 4.0% | 0.02% |
| 4 | 65 | 9.2% | 0.34% |
| 5 | 63 | 20.6% | 0.46% |
| 6 | 48 | 41.7% | 1.46% |
| 7 | 66 | 53.0% | 2.03% |
| 8 | 37 | 56.8% | 1.54% |
| 9 | 50 | 44.0% | 1.47% |
| 10 | 67 | 55.2% | 2.10% |


### 4.2 Detalhamento por nível de ocupação do veículo (peso ou volume, o que for maior)

| Ocupação (% da capacidade) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 0–25% | 15 | 26.7% | 1.43% |
| 25–50% | 83 | 39.8% | 1.35% |
| 50–75% | 129 | 34.9% | 1.18% |
| 75–100% | 172 | 27.3% | 0.91% |
| > 100% (inviável) | 101 | 26.7% | 0.85% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 31.2% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.
5. **Capacidade é respeitada?** não — o algoritmo não valida capacidade hoje; 20.2% das cargas sorteadas excederam o peso e/ou o volume máximo deste veículo e ainda assim foram roteadas normalmente.


---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Pesado (5 eixos))

Gerado em: 2026-08-05 00:58:33
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
ver `ScenarioGenerator`). A ocupação efetiva usada nas fórmulas de consumo e
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
| Rotas que mudaram entre Cenário A e B | 34.8% |
| Cargas inviáveis (peso ou volume > 100% da capacidade) | 0.0% |
| Trials em que o volume (cubagem), não o peso, foi a restrição | 35.2% |
| Gap de custo — média | R$ 9.48 (1.36%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 19.34 (2.61 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 131.49 (14.86%) |

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
| Estatística (W) | 15225.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fração de peso ocupada (peso carga / capacidade kg) | -0.048 |
| Fração de volume ocupada (volume carga / capacidade m³) | -0.094 |
| Ocupação efetiva (a mais restritiva das duas acima) | -0.094 |
| Fração de trechos urbanos na rota | 0.195 |
| Número de waypoints | 0.475 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 54 | 0.0% | 0.00% |
| 3 | 50 | 2.0% | 0.02% |
| 4 | 65 | 13.8% | 0.46% |
| 5 | 63 | 20.6% | 0.64% |
| 6 | 48 | 41.7% | 1.88% |
| 7 | 66 | 62.1% | 2.60% |
| 8 | 37 | 59.5% | 2.05% |
| 9 | 50 | 56.0% | 1.92% |
| 10 | 67 | 59.7% | 2.63% |


### 4.2 Detalhamento por nível de ocupação do veículo (peso ou volume, o que for maior)

| Ocupação (% da capacidade) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 0–25% | 270 | 38.9% | 1.59% |
| 25–50% | 230 | 30.0% | 1.09% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 34.8% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.
5. **Capacidade é respeitada?** não — o algoritmo não valida capacidade hoje; 0.0% das cargas sorteadas excederam o peso e/ou o volume máximo deste veículo e ainda assim foram roteadas normalmente.


---

