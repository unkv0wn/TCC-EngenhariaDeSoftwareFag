# Vida Útil de Pneu Empírica (via Log de Trocas) vs Assumida — Relatório

Gerado em: 2026-08-05 00:59:17

## 1. Contexto

`VehicleProfile.medium()` usa `tireLifeKm = 60000.0` — uma constante documentada como suposição plausível, não telemetria real (ver Javadoc de `VehicleProfile`). Este relatório substitui esse valor fixo por uma média empírica calculada por `EmpiricalTireLifeCalculator` a partir de uma janela móvel dos últimos 2 eventos de troca por desgaste normal (grupo eixo de TRAÇÃO, 5 registros no log, incluindo um evento de dano/acidente propositalmente excluído) e reaproveita o mesmo cenário fixo de Toledo-PR de `ToledoRouteComparisonExample` (mesma seed, mesmos sete pontos reais, mesma carga) para comparar os dois valores lado a lado.

## 2. Fórmula — antes e depois

**Antes** (`VehicleProfile.medium()`, valor fixo assumido):

```
tireLifeKm = 60000.0   // constante documentada, não medida
```

**Depois** (janela móvel dos últimos 2 eventos de desgaste normal):

```
tireLifeKm = média( kmUsado )   [últimos 2 eventos DESGASTE_NORMAL, eventos DANO_ACIDENTE excluídos]
           = 44000.0 km
```

O resto da fórmula de `CostMatrixBuilder` não muda — `tireLifeKm` é só um dos fatores de entrada:

```
tireWearFraction = (1 / tireLifeKm) × axleCount × (1 + 0.5 × loadFactor) × roadType.wearMultiplier
tireWearReais    = distanceKm × tireWearFraction × tireReplacementCostPerTire
```

A fórmula atual ainda usa um `tireLifeKm` único por veículo, não por posição de eixo — a versão por posição (dianteiro/tração/reboque) está descrita na spec de design, mas a refatoração de `CostMatrixBuilder` pra somar por posição é trabalho futuro. Este exemplo aproxima isso plugando só o valor calibrado do grupo de TRAÇÃO, que é a fonte dominante de desgaste num caminhão de 3 eixos (2 posições de tração contra 1 de direção).

**Por que média simples, e não ponderada por distância como no combustível:** cada evento de troca já é uma medição completa de quanto km um pneu durou — não é uma razão parcial (tipo litros/km) que precise de ponderação por distância percorrida. Ver `EmpiricalTireLifeCalculator` e a spec de design para a justificativa completa da janela pequena (2, contra 5 do combustível) e do filtro de motivo da troca.

## 3. Log de trocas (sintético) — eixo de TRAÇÃO

5 registros no total. A coluna **Motivo** marca eventos de dano/acidente (excluídos antes de qualquer janela); a coluna **Na janela?** marca os últimos 2 eventos de desgaste normal, os únicos usados no cálculo.

| Km instalação | Km troca | Km rodado | Motivo | Na janela? |
|---|---|---|---|---|
| 0 | 52000 | 52000 | DESGASTE_NORMAL | não |
| 52000 | 55500 | 3500 | DANO_ACIDENTE | — |
| 55500 | 113000 | 57500 | DESGASTE_NORMAL | não |
| 113000 | 156000 | 43000 | DESGASTE_NORMAL | sim |
| 156000 | 201000 | 45000 | DESGASTE_NORMAL | sim |
| | | **Média (janela)** | | **44000.0 km** |

## 4. Perfis comparados

| Parâmetro | Assumido (produção/relatórios anteriores) | Empírico (este relatório) |
|---|---|---|
| Vida útil do pneu | 60000 km | 44000.0 km |
| Diferença | — | -16000.0 km (-26.7%) |

Demais parâmetros (eixos, capacidade, consumo, preço do litro, preço do pneu, custo-hora) são idênticos — só a vida útil do pneu muda.

## 5. Cenário reaproveitado

Mesmo cenário fixo de `ToledoRouteComparisonExample`: sete pontos reais em Toledo-PR (Prefeitura como depósito, ROUND_TRIP), carga de 2800 kg / 14.0 m³, tipo de via por sentido sorteado com seed **20260732** (a mesma seed daquele relatório, já que a busca é determinística e não depende da vida útil do pneu). Detalhes de cada ponto e trecho estão em `docs/validation-reports/toledo-pr-route-comparison.md` — este relatório foca só no que muda com o pneu.

## 6. Exemplo numérico — um trecho

Primeiro trecho da rota do Cenário A: **Prefeitura Municipal de Toledo → Terminal Rodoviário de Toledo** (0.50 km, via URBANA, loadFactor = 0.700):

**Com vida útil assumida (60000 km):**

```
tireWearFraction = (1 / 60000) × 3 × (1 + 0.5 × 0.700) × 1.40 = 0.00009450
tireWearReais    = 0.50 × 0.00009450 × 1800.00 = R$ 0.0848
```

**Com vida útil empírica (44000.0 km):**

```
tireWearFraction = (1 / 44000.0) × 3 × (1 + 0.5 × 0.700) × 1.40 = 0.00012886
tireWearReais    = 0.50 × 0.00012886 × 1800.00 = R$ 0.1156
```

Nesse único trecho, a vida útil empírica (mais curta) resulta em +0.0308 R$ (+36.4%) a mais de custo de desgaste do que a assumida — a mesma proporção se propaga para todos os 42 trechos do grafo, porque `tireWearFraction` é inversamente proporcional a `tireLifeKm`.

## 7. A rota escolhida muda?

- **Cenário A (só duração — não depende de pneu):** Prefeitura Municipal de Toledo → Terminal Rodoviário de Toledo → C.Vale — Sede → Parque Ecológico de Toledo → UNIOESTE — Campus Toledo → Shopping Toledo → Catedral Sagrada Família → Prefeitura Municipal de Toledo
- **Cenário B, vida útil assumida (60.000 km):** Prefeitura Municipal de Toledo → Catedral Sagrada Família → Terminal Rodoviário de Toledo → Shopping Toledo → UNIOESTE — Campus Toledo → Parque Ecológico de Toledo → C.Vale — Sede → Prefeitura Municipal de Toledo
- **Cenário B, vida útil empírica:** Prefeitura Municipal de Toledo → Catedral Sagrada Família → Terminal Rodoviário de Toledo → Shopping Toledo → UNIOESTE — Campus Toledo → Parque Ecológico de Toledo → C.Vale — Sede → Prefeitura Municipal de Toledo
- Cenário B (assumido) difere de A? **Sim** · Cenário B (empírico) difere de A? **Sim** · Cenário B (empírico) difere de Cenário B (assumido)? **Não**

## 8. Métricas agregadas — assumido vs empírico

| Métrica | Vida útil assumida (60.000 km) | Vida útil empírica |
|---|---|---|
| Desgaste de pneu — rota do Cenário A | R$ 7.57 | R$ 10.33 |
| Desgaste de pneu — rota do Cenário B | R$ 7.52 | R$ 10.26 |
| Custo total (fórmula B) — rota do Cenário A | R$ 41.57 | R$ 44.33 |
| Custo total (fórmula B) — rota do Cenário B | R$ 41.44 | R$ 44.18 |
| Gap (A vs B, mesma fórmula) | R$ 0.13 (0.32%) | R$ 0.15 (0.34%) |

## 9. Conclusão

1. **A fórmula em si não muda** — só o valor que entra em `tireLifeKm`. Isso confirma que `CostMatrixBuilder` já aceita um valor calibrado por telemetria real no lugar da constante assumida, sem precisar mexer em código (dentro do modelo de um único `tireLifeKm` por veículo — a versão por posição de eixo ainda é trabalho futuro).
2. **A vida útil empírica é 26.7% menor que a assumida** (60000 km → 44000.0 km) — o impacto no custo de desgaste se propaga proporcionalmente a todos os trechos do grafo, já que `tireWearFraction` é inversamente proporcional a `tireLifeKm`.
3. **A rota escolhida pelo Cenário B não muda ao trocar a vida útil assumida pela empírica neste cenário** — isso significa que, ao menos para esta geometria e carga específicas, o valor exato da vida útil do pneu não foi decisivo — mudou o custo total, mas não a ordem ótima.
4. **Sobre o log sintético:** o evento de dano/acidente incluído no log (seção 3) foi corretamente excluído do cálculo — confirma que o filtro de `motivoTroca` funciona como descrito na spec, não só em teste unitário isolado.
