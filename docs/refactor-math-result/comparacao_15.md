# Comparação entre 3 Caminhões — 15 Waypoints

Gerado em: 2026-08-06 22:25:01

## Contexto

`light()`, `medium()` e `heavy()` ([`VehicleProfile`](../../backend/src/main/java/com/routewise/validation/VehicleProfile.java)) comparados na **mesma rota** de 15 waypoints — mesma matriz de distância, mesmos tipos de via por aresta, mesma carga para os três, só o veículo muda. Rota sequencial `0→1→...→14` (não a ótima de cada perfil), justamente para isolar o efeito do veículo do efeito da escolha de rota. Seed fixa 20260807, distância total percorrida 622.5 km, carga do cenário 3336 kg / 4.1 m³.

## Resultado

| Métrica | Leve (2 eixos) | Médio (3 eixos) | Pesado (5 eixos) |
|---|---|---|---|
| Peso máximo (kg) | 1500 | 4000 | 12000 |
| Tempo de viagem (min) | 948.1 | 948.1 | 948.1 |
| Combustível total (L) | 86.56 | 124.87 | 230.85 |
| Média de combustível (L/100km) | 13.90 | 20.06 | 37.08 |
| Desgaste de pneu (R$) | 116.69 | 299.40 | 356.98 |

O tempo de viagem é **idêntico** entre os três (948.1 min) — `durationSec` em `CostMatrixBuilder` depende só de distância e tipo de via (`RoadType.avgSpeedKmh`), nunca do perfil do veículo, então nenhum caminhão "anda mais rápido" que o outro na mesma via. Peso máximo é um dado estático do perfil (`VehicleProfile.capacityKg`), não depende da rota. Combustível e desgaste de pneu crescem de leve para pesado: o consumo-base do pesado (32 L/100km) e o custo de troca por pneu (R$2.200) são tão maiores que os do leve (10 L/100km, R$900) que essa ordem se mantém mesmo quando o leve está no seu próprio limite de carga e o pesado está com folga — ver `EmpiricalCostSimulationTest` para a análise completa dessa robustez a `loadFactor`.
