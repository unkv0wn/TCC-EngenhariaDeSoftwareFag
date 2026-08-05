# Consumo Empírico (via Histórico de Abastecimento) vs Consumo Assumido — Relatório

Gerado em: 2026-08-01 12:30:05

## 1. Contexto

`VehicleProfile.medium()` usa `baseFuelConsumptionLPer100Km = 15.0` — uma constante documentada como suposição plausível, não telemetria real (ver Javadoc de `VehicleProfile`). Este relatório substitui esse valor fixo por uma média empírica calculada por `EmpiricalFuelConsumptionCalculator` a partir de uma janela móvel dos últimos 5 abastecimentos de um histórico sintético (8 registros no total) e reaproveita o mesmo cenário fixo de Toledo-PR de `ToledoRouteComparisonExample` (mesma seed, mesmos sete pontos reais, mesma carga) para comparar os dois valores lado a lado — o que muda na fórmula, e o que muda no resultado do teste.

## 2. Fórmula — antes e depois

**Antes** (`VehicleProfile.medium()`, valor fixo assumido):

```
baseFuelConsumptionLPer100Km = 15.0   // constante documentada, não medida
```

**Depois** (janela móvel dos últimos 5 abastecimentos):

```
baseFuelConsumptionLPer100Km = ( Σ litrosAbastecidos / Σ kmRodados ) × 100   [últimos 5 abastecimentos]
                              = ( 1039.0 / 5650.0 ) × 100
                              = 18.39 L/100km
```

O resto da fórmula de `CostMatrixBuilder` não muda — `baseFuelConsumptionLPer100Km` é só um dos fatores de entrada:

```
consumoAjustado = baseFuelConsumptionLPer100Km × (1 + 0.30 × loadFactor) × roadType.fuelMultiplier
fuelLiters      = distanceKm × consumoAjustado / 100
```

Ou seja: a mudança está inteiramente em **qual número entra** na fórmula, não na fórmula em si — `loadFactor` (peso/volume da carga) e `roadType.fuelMultiplier` (tipo de via) continuam ajustando esse valor base do mesmo jeito.

**Por que janela móvel, e não o histórico inteiro:** um único tanque isolado tem ruído alto (trecho de rodovia vs. cidade, carga variando de viagem pra viagem); o histórico inteiro suaviza esse ruído, mas também demora a refletir uma mudança real recente (pneu careca, motor precisando de revisão). A janela dos últimos 5 abastecimentos é o meio-termo: estável o bastante pra não ser dominada por um tanque atípico, recente o bastante pra acompanhar o estado atual do veículo. Ver `EmpiricalFuelConsumptionCalculator` e a spec de design para a política completa, incluindo o comportamento de partida a frio (veículo sem histórico ainda).

**Por que soma de litros / soma de km dentro da janela, e não a média simples do consumo de cada abastecimento individual:** cada abastecimento tem um número diferente de km rodados; uma média simples das razões `litros/km` de cada linha pesa igualmente um tanque que rodou 1030 km e um que rodou 1250 km, distorcendo o resultado para cima ou para baixo dependendo de quais tanques tiveram trechos mais curtos. Dividir a soma total de litros pela soma total de km pondera cada abastecimento pela distância real que ele cobriu — é o consumo médio correto do período, não a média das médias.

## 3. Histórico de abastecimento (sintético)

8 registros no total; a coluna **Na janela?** marca os últimos 5, que são os únicos usados no cálculo (os mais antigos aparecem só pra contexto — mostram o que a janela deixou de fora).

| Data | Litros abastecidos | Km rodados desde o último | Consumo do tanque | Na janela? |
|---|---|---|---|---|
| 2026-01-08 | 215.0 L | 1200 km | 17.92 L/100km | não |
| 2026-01-22 | 198.0 L | 1080 km | 18.33 L/100km | não |
| 2026-02-05 | 230.0 L | 1250 km | 18.40 L/100km | não |
| 2026-02-19 | 205.0 L | 1150 km | 17.83 L/100km | sim |
| 2026-03-05 | 190.0 L | 1030 km | 18.45 L/100km | sim |
| 2026-03-19 | 222.0 L | 1190 km | 18.66 L/100km | sim |
| 2026-04-02 | 208.0 L | 1120 km | 18.57 L/100km | sim |
| 2026-04-16 | 214.0 L | 1160 km | 18.45 L/100km | sim |
| **Total / Média ponderada (janela)** | **1039.0 L** | **5650 km** | **18.39 L/100km** | |

Para comparação: a média simples (não ponderada) das linhas na janela seria 18.3896 L/100km — próxima, mas não idêntica, à média ponderada de 18.3894 L/100km usada neste relatório; a diferença entre as duas cresce quanto mais desigual for a quilometragem entre tanques.

## 4. Perfis comparados

| Parâmetro | Assumido (produção/relatórios anteriores) | Empírico (este relatório) |
|---|---|---|
| Consumo base | 15.0 L/100km | 18.39 L/100km |
| Diferença | — | +3.39 L/100km (+22.6%) |

Demais parâmetros (eixos, capacidade, preço do litro, pneu, custo-hora) são idênticos — só o consumo base muda.

## 5. Cenário reaproveitado

Mesmo cenário fixo de `ToledoRouteComparisonExample`: sete pontos reais em Toledo-PR (Prefeitura como depósito, ROUND_TRIP), carga de 2800 kg / 14.0 m³, tipo de via por sentido sorteado com seed **20260732** (a mesma seed daquele relatório, já que a busca é determinística e não depende do consumo de combustível). Detalhes de cada ponto e trecho estão em `docs/validation-reports/toledo-pr-route-comparison.md` — este relatório foca só no que muda com o consumo.

## 6. Exemplo numérico — um trecho

Primeiro trecho da rota do Cenário A: **Prefeitura Municipal de Toledo → Terminal Rodoviário de Toledo** (0.50 km, via URBANA, loadFactor = 0.700):

**Com consumo assumido (15.0 L/100km):**

```
consumoAjustado = 15.0 × (1 + 0.30 × 0.700) × 1.30 = 23.595 L/100km
fuelLiters      = 0.50 × 23.595 / 100 = 0.118 L
```

**Com consumo empírico (18.39 L/100km):**

```
consumoAjustado = 18.39 × (1 + 0.30 × 0.700) × 1.30 = 28.926 L/100km
fuelLiters      = 0.50 × 28.926 / 100 = 0.144 L
```

Nesse único trecho, o consumo empírico consome +0.027 L (+22.6%) a mais do que o assumido — a mesma proporção de aumento (22.6%) do consumo base se propaga para todos os 42 trechos do grafo, porque `consumoAjustado` é linear em `baseFuelConsumptionLPer100Km`.

## 7. A rota escolhida muda?

- **Cenário A (só duração — não depende de combustível):** Prefeitura Municipal de Toledo → Terminal Rodoviário de Toledo → C.Vale — Sede → Parque Ecológico de Toledo → UNIOESTE — Campus Toledo → Shopping Toledo → Catedral Sagrada Família → Prefeitura Municipal de Toledo
- **Cenário B, consumo assumido (15.0 L/100km):** Prefeitura Municipal de Toledo → Catedral Sagrada Família → Terminal Rodoviário de Toledo → Shopping Toledo → UNIOESTE — Campus Toledo → Parque Ecológico de Toledo → C.Vale — Sede → Prefeitura Municipal de Toledo
- **Cenário B, consumo empírico:** Prefeitura Municipal de Toledo → Catedral Sagrada Família → Terminal Rodoviário de Toledo → Shopping Toledo → UNIOESTE — Campus Toledo → Parque Ecológico de Toledo → C.Vale — Sede → Prefeitura Municipal de Toledo
- Cenário B (assumido) difere de A? **Sim** · Cenário B (empírico) difere de A? **Sim** · Cenário B (empírico) difere de Cenário B (assumido)? **Não**

## 8. Métricas agregadas — assumido vs empírico

| Métrica | Consumo assumido (15.0 L/100km) | Consumo empírico |
|---|---|---|
| Combustível — rota do Cenário A | 3.20 L | 3.92 L |
| Combustível — rota do Cenário B | 3.17 L | 3.88 L |
| Custo total (fórmula B) — rota do Cenário A | R$ 36.20 | R$ 40.60 |
| Custo total (fórmula B) — rota do Cenário B | R$ 36.10 | R$ 40.47 |
| Gap (A vs B, mesma fórmula) | R$ 0.10 (0.26%) | R$ 0.14 (0.34%) |

## 9. Conclusão

1. **A fórmula em si não muda** — só o valor que entra em `baseFuelConsumptionLPer100Km`. Isso confirma que `CostMatrixBuilder` já está desenhado para aceitar um valor calibrado por telemetria real no lugar da constante assumida, sem precisar mexer em código.
2. **O aumento é proporcional em todo trecho:** 15.00 → 18.39 L/100km é um aumento de 22.6%, e como `consumoAjustado` é linear em `baseFuelConsumptionLPer100Km`, todo litro calculado no grafo sobe na mesma proporção — não só no trecho do exemplo da seção 6.
3. **A rota escolhida pelo Cenário B não muda ao trocar o consumo assumido pelo empírico neste cenário** — isso significa que, ao menos para esta geometria e carga específicas, o valor exato do consumo base não foi decisivo — mudou o custo total, mas não a ordem ótima.
4. **Generalização:** este é um único cenário fixo; o efeito de recalibrar o consumo base tende a ser mais visível quanto mais díspares forem os tipos de via/carga entre as rotas candidatas (ver `docs/validation-reports/empirical-cost-validation.md` para o comportamento agregado em 500 cenários sintéticos). O objetivo aqui era mostrar *como* plugar um consumo derivado de abastecimento real na fórmula existente, não afirmar que 15.0 L/100km está errado.
