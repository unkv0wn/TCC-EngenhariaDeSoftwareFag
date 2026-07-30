# Validação Empírica do Custo de Rota — Comparação entre Perfis de Veículo

Os mesmos 500 cenários sintéticos de rota foram avaliados para cada perfil de veículo abaixo — só o veículo muda entre as colunas.

## Comparação entre perfis

| Perfil | Rotas diferentes | Gap médio (R$) | Gap médio (%) | Wilcoxon p-valor |
|---|---|---|---|---|
| Leve (2 eixos) | 25.0% | R$ 2.35 | 0.85% | 0.0000 |
| Médio (3 eixos) | 29.0% | R$ 4.17 | 1.08% | 0.0000 |
| Pesado (5 eixos) | 35.4% | R$ 10.87 | 1.58% | 0.0000 |

---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Leve (2 eixos))

Gerado em: 2026-07-29 16:51:40
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
| Capacidade | 1500 kg |
| Consumo base | 10.0 L/100km |
| Preço do litro | R$ 6.10 |
| Custo de reposição do pneu | R$ 900.00 |
| Vida útil do pneu | 50000 km |
| Custo-hora do motorista | R$ 30.00 |

Cada trecho sintético recebe um tipo de via (RODOVIA/ARTERIAL/URBANA) sorteado
independentemente por par ordenado, com velocidade e multiplicadores de
consumo/desgaste próprios — isso evita que o experimento seja circular (se tudo
fosse proporcional só à distância, as duas rotas seriam sempre idênticas).

## 2. Resultados agregados

| Métrica | Valor |
|---|---|
| Rotas que mudaram entre Cenário A e B | 25.0% |
| Gap de custo — média | R$ 2.35 (0.85%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 5.87 (2.02 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 43.82 (13.84%) |

"Gap" = quanto a operação deixa de economizar, em R$, seguindo a rota do Cenário A
em vez da rota do Cenário B — sempre ≥ 0, pois a rota do Cenário B é ótima por
construção sob o próprio custo do Cenário B.

## 3. Teste estatístico

Wilcoxon signed-rank (pareado, custo do Cenário B avaliado na rota A vs na rota B,
excluindo pares com diferença zero, aproximação normal para amostra grande):

| | |
|---|---|
| Estatística (W) | 7875.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fator de carga do veículo | 0.049 |
| Fração de trechos urbanos na rota | 0.225 |
| Número de waypoints | 0.426 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 56 | 0.0% | 0.00% |
| 3 | 39 | 0.0% | 0.00% |
| 4 | 51 | 5.9% | 0.28% |
| 5 | 60 | 18.3% | 0.54% |
| 6 | 53 | 15.1% | 0.70% |
| 7 | 49 | 40.8% | 1.13% |
| 8 | 61 | 27.9% | 0.77% |
| 9 | 66 | 43.9% | 1.49% |
| 10 | 65 | 56.9% | 2.18% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 25.0% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.


---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Médio (3 eixos))

Gerado em: 2026-07-29 16:51:40
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
| Capacidade | 4000 kg |
| Consumo base | 15.0 L/100km |
| Preço do litro | R$ 6.10 |
| Custo de reposição do pneu | R$ 1800.00 |
| Vida útil do pneu | 60000 km |
| Custo-hora do motorista | R$ 35.00 |

Cada trecho sintético recebe um tipo de via (RODOVIA/ARTERIAL/URBANA) sorteado
independentemente por par ordenado, com velocidade e multiplicadores de
consumo/desgaste próprios — isso evita que o experimento seja circular (se tudo
fosse proporcional só à distância, as duas rotas seriam sempre idênticas).

## 2. Resultados agregados

| Métrica | Valor |
|---|---|
| Rotas que mudaram entre Cenário A e B | 29.0% |
| Gap de custo — média | R$ 4.17 (1.08%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 9.71 (2.37 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 69.22 (15.52%) |

"Gap" = quanto a operação deixa de economizar, em R$, seguindo a rota do Cenário A
em vez da rota do Cenário B — sempre ≥ 0, pois a rota do Cenário B é ótima por
construção sob o próprio custo do Cenário B.

## 3. Teste estatístico

Wilcoxon signed-rank (pareado, custo do Cenário B avaliado na rota A vs na rota B,
excluindo pares com diferença zero, aproximação normal para amostra grande):

| | |
|---|---|
| Estatística (W) | 10585.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fator de carga do veículo | 0.053 |
| Fração de trechos urbanos na rota | 0.234 |
| Número de waypoints | 0.480 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 56 | 0.0% | 0.00% |
| 3 | 39 | 0.0% | 0.00% |
| 4 | 51 | 5.9% | 0.32% |
| 5 | 60 | 20.0% | 0.71% |
| 6 | 53 | 18.9% | 0.85% |
| 7 | 49 | 40.8% | 1.40% |
| 8 | 61 | 34.4% | 1.06% |
| 9 | 66 | 54.5% | 1.88% |
| 10 | 65 | 66.2% | 2.73% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 29.0% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.


---

# Validação Empírica do Custo de Rota — Relatório (Perfil: Pesado (5 eixos))

Gerado em: 2026-07-29 16:51:40
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
| Capacidade | 12000 kg |
| Consumo base | 32.0 L/100km |
| Preço do litro | R$ 6.10 |
| Custo de reposição do pneu | R$ 2200.00 |
| Vida útil do pneu | 80000 km |
| Custo-hora do motorista | R$ 42.00 |

Cada trecho sintético recebe um tipo de via (RODOVIA/ARTERIAL/URBANA) sorteado
independentemente por par ordenado, com velocidade e multiplicadores de
consumo/desgaste próprios — isso evita que o experimento seja circular (se tudo
fosse proporcional só à distância, as duas rotas seriam sempre idênticas).

## 2. Resultados agregados

| Métrica | Valor |
|---|---|
| Rotas que mudaram entre Cenário A e B | 35.4% |
| Gap de custo — média | R$ 10.87 (1.58%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 22.31 (3.05 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 152.82 (18.41%) |

"Gap" = quanto a operação deixa de economizar, em R$, seguindo a rota do Cenário A
em vez da rota do Cenário B — sempre ≥ 0, pois a rota do Cenário B é ótima por
construção sob o próprio custo do Cenário B.

## 3. Teste estatístico

Wilcoxon signed-rank (pareado, custo do Cenário B avaliado na rota A vs na rota B,
excluindo pares com diferença zero, aproximação normal para amostra grande):

| | |
|---|---|
| Estatística (W) | 15753.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fator de carga do veículo | 0.038 |
| Fração de trechos urbanos na rota | 0.240 |
| Número de waypoints | 0.523 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 56 | 0.0% | 0.00% |
| 3 | 39 | 0.0% | 0.00% |
| 4 | 51 | 7.8% | 0.42% |
| 5 | 60 | 26.7% | 1.08% |
| 6 | 53 | 28.3% | 1.30% |
| 7 | 49 | 49.0% | 2.01% |
| 8 | 61 | 45.9% | 1.75% |
| 9 | 66 | 63.6% | 2.71% |
| 10 | 65 | 73.8% | 3.84% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 35.4% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.


---

