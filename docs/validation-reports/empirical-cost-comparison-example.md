# Exemplo de Comparação — Cenário A vs Cenário B (10 pontos)

Gerado em: 2026-07-29 16:45:33 · seed: 1

Cenário fixo e reprodutível (não faz parte da amostragem estatística de 500 trials — é um exemplo isolado, escolhido entre várias seeds por ser um caso onde a rota realmente muda, com os mesmos 10 waypoints usados nas duas simulações).

## 1. Waypoints

| # | Latitude | Longitude |
|---|---|---|
| 0 | -23.388885 | -46.696243 |
| 1 | -23.755100 | -46.750398 |
| 2 | -23.223071 | -46.979018 |
| 3 | -23.225907 | -46.325394 |
| 4 | -23.237464 | -46.327342 |
| 5 | -23.622478 | -46.740037 |
| 6 | -23.694660 | -46.628761 |
| 7 | -23.819323 | -46.443925 |
| 8 | -23.438575 | -46.873577 |
| 9 | -23.635759 | -46.885466 |

## 2. Ordem escolhida

- **Cenário A (só tempo, produção atual):** 0 → 8 → 2 → 6 → 7 → 1 → 5 → 9 → 3 → 4 → 0
- **Cenário B (tempo + combustível + desgaste de pneu):** 0 → 5 → 6 → 7 → 1 → 9 → 8 → 2 → 3 → 4 → 0
- Rotas diferentes? **Sim**

## 3. Métricas agregadas de cada rota

| Métrica | Rota do Cenário A | Rota do Cenário B |
|---|---|---|
| Distância total | 309.33 km | 272.29 km |
| Duração total | 239.3 min | 249.6 min |
| Combustível total | 41.86 L | 38.56 L |
| Custo de desgaste de pneu | R$ 24.51 | R$ 22.98 |
| Custo total (fórmula combinada, R$) | R$ 419.47 | R$ 403.81 |

**Gap:** seguir a rota do Cenário A custaria R$ 15.66 a mais (3.73%) do que a rota do Cenário B, avaliadas as duas sob a mesma fórmula de custo combinada.

## 4. Detalhamento por trecho — Cenário A

| De | Para | Tipo de via | Distância (km) | Duração (min) | Combustível (L) | Desgaste (R$) | Custo do trecho (R$) |
|---|---|---|---|---|---|---|---|
| 0 | 8 | RODOVIA | 18.92 | 14.2 | 2.54 | 1.48 | 25.23 |
| 8 | 2 | RODOVIA | 26.27 | 19.7 | 3.52 | 2.05 | 35.04 |
| 2 | 6 | RODOVIA | 63.45 | 47.6 | 8.51 | 4.96 | 84.63 |
| 6 | 7 | RODOVIA | 23.37 | 17.5 | 3.13 | 1.83 | 31.17 |
| 7 | 1 | RODOVIA | 31.99 | 24.0 | 4.29 | 2.50 | 42.67 |
| 1 | 5 | RODOVIA | 14.78 | 11.1 | 1.98 | 1.16 | 19.72 |
| 5 | 9 | ARTERIAL | 14.89 | 17.9 | 2.35 | 1.46 | 26.21 |
| 9 | 3 | RODOVIA | 73.09 | 54.8 | 9.80 | 5.72 | 97.48 |
| 3 | 4 | ARTERIAL | 1.30 | 1.6 | 0.21 | 0.13 | 2.29 |
| 4 | 0 | RODOVIA | 41.26 | 30.9 | 5.53 | 3.23 | 55.03 |

## 5. Detalhamento por trecho — Cenário B

| De | Para | Tipo de via | Distância (km) | Duração (min) | Combustível (L) | Desgaste (R$) | Custo do trecho (R$) |
|---|---|---|---|---|---|---|---|
| 0 | 5 | RODOVIA | 26.36 | 19.8 | 3.53 | 2.06 | 35.15 |
| 5 | 6 | RODOVIA | 13.89 | 10.4 | 1.86 | 1.09 | 18.52 |
| 6 | 7 | RODOVIA | 23.37 | 17.5 | 3.13 | 1.83 | 31.17 |
| 7 | 1 | RODOVIA | 31.99 | 24.0 | 4.29 | 2.50 | 42.67 |
| 1 | 9 | ARTERIAL | 19.11 | 22.9 | 3.02 | 1.87 | 33.64 |
| 9 | 8 | URBANA | 21.96 | 52.7 | 4.50 | 3.01 | 61.22 |
| 8 | 2 | RODOVIA | 26.27 | 19.7 | 3.52 | 2.05 | 35.04 |
| 2 | 3 | RODOVIA | 66.79 | 50.1 | 8.96 | 5.22 | 89.08 |
| 3 | 4 | ARTERIAL | 1.30 | 1.6 | 0.21 | 0.13 | 2.29 |
| 4 | 0 | RODOVIA | 41.26 | 30.9 | 5.53 | 3.23 | 55.03 |

## 6. Perfil de veículo usado

Eixos: 3 · Capacidade: 4000 kg · Carga neste cenário: 17% · Consumo base: 15.0 L/100km · Preço do litro: R$ 6.10 · Custo do pneu: R$ 1800.00 · Vida útil do pneu: 60000 km · Custo-hora: R$ 35.00
