# Benchmark de frota — cenário estendido (15 pontos, 4 veículos, Toledo-PR)

## 1. O que mudou em relação ao cenário de 10 pontos

Mesma metodologia de [toledo-tsp-fleet-benchmark.md](toledo-tsp-fleet-benchmark.md) — TSP
exato por programação dinâmica bitmask (Held-Karp) sobre a matriz real do OSRM, rota única
aplicada aos 4 perfis de veículo — mas com **5 pontos de entrega extras** (P10–P14),
totalizando 14 paradas + depósito (P0).

**Ponto de atenção de complexidade:** o algoritmo de produção (`AStarWaypointOptimizer`) é
documentado como viável até 10 waypoints (`O(n²·2ⁿ)`). Aqui, com n=15, o espaço de estados
é 15 × 2¹⁵ = 491.520 — **32× maior** que o de n=10 (10 × 2¹⁰ = 10.240) — ainda tratável em
segundos com bitmask DP em Python, mas ilustra por que o limite prático de 10 existe: cada
waypoint a mais dobra o espaço de estados.

## 2. Pontos extras adicionados

| Ponto | Longitude | Latitude |
|---|---|---|
| P10 | -53.7440 | -24.7080 |
| P11 | -53.7260 | -24.7230 |
| P12 | -53.7420 | -24.7300 |
| P13 | -53.7340 | -24.7150 |
| P14 | -53.7460 | -24.7170 |

## 3. Rota ótima (TSP exato, 15 pontos)

**P0 → P13 → P4 → P7 → P8 → P10 → P1 → P14 → P3 → P6 → P5 → P12 → P9 → P11 → P2 → P0**

| Métrica | Cenário 10 pts | Cenário 15 pts |
|---|---|---|
| Distância total (OSRM `/route`) | 12.74 km | **15.36 km** |
| Duração OSRM (referência) | 25.87 min | **30.93 min** |
| Velocidade média implícita | 29.6 km/h | 29.8 km/h |
| Paradas de entrega | 9 | **14** |

## 4. Benchmarking financeiro — mesma rota, 4 veículos

| Veículo | Tempo direção | Tempo serviço (14×) | Combustível+Pneu | Motorista | **Total** |
|---|---|---|---|---|---|
| 🏆 **Van Leve Econômica** | 30.9 min | 70.0 min | R$ 12.29 | R$ 37.01 | **R$ 49.30** |
| Furgão Leve Turbo | 30.9 min | 56.0 min | R$ 13.98 | R$ 36.22 | **R$ 50.20** |
| VUC Médio Padrão | 30.9 min | 112.0 min | R$ 19.97 | R$ 71.46 | **R$ 91.44** |
| VUC Médio Pesado | 30.9 min | 140.0 min | R$ 22.59 | R$ 91.16 | **R$ 113.75** |

**Vencedor: Van Leve Econômica, R$ 49.30** — continua vencendo, e a vantagem sobre o Furgão
Leve Turbo fica ainda mais estreita (R$ 0.90 de diferença, contra R$ 1.20 no cenário de 9
paradas) porque o Furgão tem tempo de serviço/parada menor (4 min vs 5 min), então quanto
mais paradas a rota tem, mais essa vantagem de serviço pesa a favor do Furgão. Já o VUC
Médio Pesado dispara: de R$ 80.53 (9 paradas) para **R$ 113.75** (14 paradas) — o tempo de
serviço sozinho (140 min) já é mais que 4× o tempo de direção (30.9 min), mostrando que,
para veículos com parada demorada e motorista caro, o número de paradas pesa muito mais no
custo total do que a distância percorrida.

## 5. Validação OSRM (URL real, clicável)

http://router.project-osrm.org/route/v1/driving/-53.7380,-24.7170;-53.7340,-24.7150;-53.7280,-24.7150;-53.7300,-24.7080;-53.7390,-24.7050;-53.7440,-24.7080;-53.7450,-24.7120;-53.7460,-24.7170;-53.7490,-24.7190;-53.7470,-24.7250;-53.7410,-24.7280;-53.7420,-24.7300;-53.7350,-24.7290;-53.7260,-24.7230;-53.7320,-24.7220;-53.7380,-24.7170?geometries=geojson&annotations=true

## 6. GeoJSON

Ver arquivo anexo `toledo-tsp-route-15pts.geojson` — `FeatureCollection` com uma única
`LineString`, geometria real obtida via OSRM `/route` (`overview=full`).
