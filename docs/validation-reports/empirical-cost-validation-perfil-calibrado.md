# Validação Empírica do Custo de Rota — Relatório (Perfil: Médio (3 eixos) (calibrado: combustível + pneu empíricos))

Gerado em: 2026-08-05 00:58:38
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
| Consumo base | 18.4 L/100km |
| Preço do litro | R$ 6.10 |
| Custo de reposição do pneu | R$ 1800.00 |
| Vida útil do pneu | 44000 km |
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
| Rotas que mudaram entre Cenário A e B | 32.2% |
| Cargas inviáveis (peso ou volume > 100% da capacidade) | 20.2% |
| Trials em que o volume (cubagem), não o peso, foi a restrição | 46.6% |
| Gap de custo — média | R$ 6.46 (1.22%) |
| Gap de custo — mediana | R$ 0.00 (0.00%) |
| Gap de custo — desvio padrão | R$ 13.70 (2.42 pp) |
| Gap de custo — mínimo | R$ 0.00 (0.00%) |
| Gap de custo — máximo | R$ 101.07 (13.88%) |

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
| Estatística (W) | 13041.00 |
| p-valor | 0.0000 |

p < 0.05 — a diferença entre as duas funções de custo é estatisticamente significativa, não apenas ruído amostral.

## 4. Correlações (Spearman) com o tamanho do gap (%)

| Variável | Correlação |
|---|---|
| Fração de peso ocupada (peso carga / capacidade kg) | -0.048 |
| Fração de volume ocupada (volume carga / capacidade m³) | -0.087 |
| Ocupação efetiva (a mais restritiva das duas acima) | -0.092 |
| Fração de trechos urbanos na rota | 0.191 |
| Número de waypoints | 0.448 |

### 4.1 Detalhamento por número de waypoints

| Waypoints (n) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 2 | 54 | 0.0% | 0.00% |
| 3 | 50 | 2.0% | 0.02% |
| 4 | 65 | 12.3% | 0.40% |
| 5 | 63 | 20.6% | 0.56% |
| 6 | 48 | 41.7% | 1.68% |
| 7 | 66 | 54.5% | 2.34% |
| 8 | 37 | 56.8% | 1.83% |
| 9 | 50 | 50.0% | 1.69% |
| 10 | 67 | 55.2% | 2.39% |


### 4.2 Detalhamento por nível de ocupação do veículo (peso ou volume, o que for maior)

| Ocupação (% da capacidade) | Trials | Rotas diferentes | Gap médio (%) |
|---|---|---|---|
| 0–25% | 15 | 26.7% | 1.56% |
| 25–50% | 83 | 38.6% | 1.51% |
| 50–75% | 129 | 36.4% | 1.39% |
| 75–100% | 172 | 28.5% | 1.06% |
| > 100% (inviável) | 101 | 28.7% | 0.99% |


## 5. Conclusão

1. **Correlação com o custo:** pelo menos um indicador (ver seção 4) tem correlação não-desprezível com o tamanho do gap.
2. **Muda a rota escolhida?** sim, em 32.2% dos trials a ordem ótima mudou entre os dois cenários.
3. **É estatisticamente relevante?** sim — ver teste de Wilcoxon na seção 3.
4. **Quais indicadores importam mais?** ver as magnitudes de correlação na seção 4 — o de maior valor absoluto é o que mais explica a variação do gap.
5. **Capacidade é respeitada?** não — o algoritmo não valida capacidade hoje; 20.2% das cargas sorteadas excederam o peso e/ou o volume máximo deste veículo e ainda assim foram roteadas normalmente.

