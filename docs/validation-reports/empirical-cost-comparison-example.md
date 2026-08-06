# Exemplo de Comparação — Cenário A vs Cenário B (10 pontos)

Gerado em: 2026-08-05 00:58:51 · seed: 1

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
| Combustível total | 47.21 L | 43.48 L |
| Custo de desgaste de pneu | R$ 102.01 | R$ 95.66 |
| Custo total (fórmula combinada, R$) | R$ 529.57 | R$ 506.51 |

**Gap:** seguir a rota do Cenário A custaria R$ 23.05 a mais (4.35%) do que a rota do Cenário B, avaliadas as duas sob a mesma fórmula de custo combinada.

## 4. Detalhamento por trecho — Cenário A

| De | Para | Tipo de via | Distância (km) | Duração (min) | Combustível (L) | Desgaste (R$) | Custo do trecho (R$) |
|---|---|---|---|---|---|---|---|
| 0 | 8 | RODOVIA | 18.92 | 14.2 | 2.86 | 6.16 | 31.89 |
| 8 | 2 | RODOVIA | 26.27 | 19.7 | 3.97 | 8.55 | 44.28 |
| 2 | 6 | RODOVIA | 63.45 | 47.6 | 9.60 | 20.66 | 106.95 |
| 6 | 7 | RODOVIA | 23.37 | 17.5 | 3.53 | 7.61 | 39.38 |
| 7 | 1 | RODOVIA | 31.99 | 24.0 | 4.84 | 10.41 | 53.92 |
| 1 | 5 | RODOVIA | 14.78 | 11.1 | 2.24 | 4.81 | 24.92 |
| 5 | 9 | ARTERIAL | 14.89 | 17.9 | 2.65 | 6.06 | 32.64 |
| 9 | 3 | RODOVIA | 73.09 | 54.8 | 11.05 | 23.79 | 123.19 |
| 3 | 4 | ARTERIAL | 1.30 | 1.6 | 0.23 | 0.53 | 2.85 |
| 4 | 0 | RODOVIA | 41.26 | 30.9 | 6.24 | 13.43 | 69.55 |

## 5. Detalhamento por trecho — Cenário B

| De | Para | Tipo de via | Distância (km) | Duração (min) | Combustível (L) | Desgaste (R$) | Custo do trecho (R$) |
|---|---|---|---|---|---|---|---|
| 0 | 5 | RODOVIA | 26.36 | 19.8 | 3.99 | 8.58 | 44.42 |
| 5 | 6 | RODOVIA | 13.89 | 10.4 | 2.10 | 4.52 | 23.41 |
| 6 | 7 | RODOVIA | 23.37 | 17.5 | 3.53 | 7.61 | 39.38 |
| 7 | 1 | RODOVIA | 31.99 | 24.0 | 4.84 | 10.41 | 53.92 |
| 1 | 9 | ARTERIAL | 19.11 | 22.9 | 3.40 | 7.78 | 41.89 |
| 9 | 8 | URBANA | 21.96 | 52.7 | 5.08 | 12.51 | 74.23 |
| 8 | 2 | RODOVIA | 26.27 | 19.7 | 3.97 | 8.55 | 44.28 |
| 2 | 3 | RODOVIA | 66.79 | 50.1 | 10.10 | 21.74 | 112.58 |
| 3 | 4 | ARTERIAL | 1.30 | 1.6 | 0.23 | 0.53 | 2.85 |
| 4 | 0 | RODOVIA | 41.26 | 30.9 | 6.24 | 13.43 | 69.55 |

## 6. Perfil de veículo usado

Eixos: 3 · Capacidade: 4000 kg / 25.0 m³ · Carga neste cenário: 818 kg (20% peso), 15.5 m³ (62% volume) · Consumo base: 15.0 L/100km · Preço do litro: R$ 6.10 · Custo do pneu: R$ 1800.00 · Vida útil do pneu: 60000 km · Custo-hora: R$ 35.00
