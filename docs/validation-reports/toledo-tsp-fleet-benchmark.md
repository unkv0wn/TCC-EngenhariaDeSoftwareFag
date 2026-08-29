# Benchmark de frota — mesma rota, 4 veículos (Toledo, PR)

## 1. Metodologia

Diferente do exercício anterior (divisão de pontos entre veículos), aqui o objetivo é uma
**rota única** — o TSP clássico, não um VRP. A matriz de distância entre os 10 pontos foi
obtida da API pública do OSRM (`/table`, malha viária real de Toledo-PR). A sequência ótima
foi resolvida por **programação dinâmica com bitmask (Held-Karp)** sobre o espaço de estados
`(nó atual, conjunto de visitados)` — exatamente a mesma formulação de estado que
`AStarWaypointOptimizer` usa em produção; para n=10 (10 × 2¹⁰ = 10.240 estados) a busca é
exaustiva e a solução é **garantidamente ótima**, não uma heurística.

Essa mesma sequência de pontos foi então "rodada" pelos 4 perfis de veículo, cada um com
seu próprio `Vl_Max`, custo de combustível/pneu e custo-hora de motorista — a distância é
idêntica para todos, o que muda é velocidade efetiva, tempo de serviço acumulado (9 paradas
× tempo/parada do veículo) e as tarifas.

```
Custo = KM × (R$Combustível + R$Pneu/Manutenção) + (Tempo_Direção + Tempo_Serviço) × R$/Hora
Tempo_Direção = Distância ÷ min(velocidade média real da via, Vl_Max do veículo)
```

**Nota sobre as duas distâncias do OSRM:** a matriz `/table` (usada para achar a rota ótima)
deu 12.39 km; o `/route` real sobre essa mesma sequência (usado para a velocidade média
efetiva, o tempo de direção e a geometria do GeoJSON) deu 12.74 km — uma diferença de ~3%,
esperada porque `/table` usa uma simplificação do grafo viário para ser rápido em matrizes
grandes, enquanto `/route` calcula o caminho exato passo a passo. Os custos abaixo usam a
distância e duração do `/route` (mais precisa) para os 4 veículos.

## 2. Rota ótima (TSP exato)

**P0 → P4 → P7 → P8 → P1 → P3 → P6 → P5 → P9 → P2 → P0**

| Métrica | Valor |
|---|---|
| Distância total (OSRM `/route`) | 12.74 km |
| Duração OSRM (referência, sem custo aplicado) | 25.87 min |
| Velocidade média implícita | 29.6 km/h |

## 3. Benchmarking financeiro — mesma rota, 4 veículos

| Veículo | Vl_Max | Vel. efetiva | Tempo direção | Tempo serviço (9×) | Combustível+Pneu | Motorista | **Total** |
|---|---|---|---|---|---|---|---|
| 🏆 **Van Leve Econômica** | 100 km/h | 29.6 km/h | 25.9 min | 45.0 min | R$ 10.20 | R$ 25.99 | **R$ 36.18** |
| Furgão Leve Turbo | 110 km/h | 29.6 km/h | 25.9 min | 36.0 min | R$ 11.60 | R$ 25.78 | **R$ 37.38** |
| VUC Médio Padrão | 80 km/h | 29.6 km/h | 25.9 min | 72.0 min | R$ 16.57 | R$ 48.93 | **R$ 65.50** |
| VUC Médio Pesado | 70 km/h | 29.6 km/h | 25.9 min | 90.0 min | R$ 18.73 | R$ 61.80 | **R$ 80.53** |

**Vencedor: Van Leve Econômica, R$ 36.18** — a mais barata em todos os quesitos (combustível,
pneu e hora de motorista), e como nenhum veículo bate no teto de `Vl_Max` nesta rota
(velocidade real da via, ~30 km/h, é bem menor que o menor `Vl_Max` da frota, 70 km/h), a
velocidade efetiva é igual para os 4 — o resultado é decidido inteiramente pelas tarifas,
não pela capacidade de velocidade. O maior fator de disparidade é o **tempo de serviço**: o
VUC Médio Pesado gasta 90 min só parado entregando (10 min × 9 paradas) contra 36 min do
Furgão — isso, multiplicado por um custo-hora de motorista também mais alto (R$32 vs R$25),
é o que faz o VUC Pesado custar mais que o dobro da Van.

## 4. Validação OSRM (URL real, clicável)

http://router.project-osrm.org/route/v1/driving/-53.7380,-24.7170;-53.7280,-24.7150;-53.7300,-24.7080;-53.7390,-24.7050;-53.7450,-24.7120;-53.7490,-24.7190;-53.7470,-24.7250;-53.7410,-24.7280;-53.7350,-24.7290;-53.7320,-24.7220;-53.7380,-24.7170?geometries=geojson&annotations=true

## 5. GeoJSON

Ver arquivo anexo `toledo-tsp-route.geojson` — `FeatureCollection` com uma única
`LineString`, geometria real obtida via OSRM `/route` (`overview=full`).
