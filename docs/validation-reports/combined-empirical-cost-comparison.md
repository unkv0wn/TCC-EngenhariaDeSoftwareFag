# Combustível + Pneu Empíricos, Combinados — vs. Estado Atual do Projeto

Gerado em: 2026-08-01 13:08:31

## 1. Contexto

Os dois relatórios anteriores (`fuel-consumption-from-refueling.md` e `tire-life-from-replacement-log.md`) validam cada correção empírica isoladamente. Este relatório empilha as duas em cima do mesmo perfil e compara o resultado direto contra **o estado atual do projeto** — `VehicleProfile.medium()` com as constantes assumidas (`baseFuelConsumptionLPer100Km = 15.0`, `tireLifeKm = 60000.0`), que é o perfil que `ToledoRouteComparisonExample` e os demais relatórios deste pacote usam hoje como linha de base — nenhum deles usa dado calibrado ainda.

## 2. Perfis comparados

| Parâmetro | Estado atual do projeto (assumido) | Combinado (fuel + pneu empíricos) | Diferença |
|---|---|---|---|
| Consumo base | 15.0 L/100km | 18.39 L/100km | +22.6% |
| Vida útil do pneu | 60000 km | 44000.0 km | -26.7% |

Demais parâmetros (eixos, capacidade, preços, custo-hora) são idênticos nos dois perfis.

## 3. Origem de cada valor

- **Consumo (18.39 L/100km):** janela móvel dos últimos 5 abastecimentos de `FuelConsumptionFromRefuelingExample.REFUELING_LOG` (8 registros no log). Detalhe completo, tanque por tanque, em `docs/validation-reports/fuel-consumption-from-refueling.md`.
- **Vida útil do pneu (44000.0 km):** janela móvel dos últimos 2 eventos de troca por desgaste normal (eixo de tração) de `TireLifeFromReplacementLogExample.REPLACEMENT_LOG` (5 registros no log, incluindo um evento de dano/acidente excluído do cálculo). Detalhe completo em `docs/validation-reports/tire-life-from-replacement-log.md`.

## 4. Cenário reaproveitado

Mesmo cenário fixo de `ToledoRouteComparisonExample`: sete pontos reais em Toledo-PR (Prefeitura como depósito, ROUND_TRIP), carga de 2800 kg / 14.0 m³, tipo de via por sentido sorteado com seed **20260732** (a mesma seed dos outros dois relatórios, já que a busca é determinística e não depende de combustível nem pneu).

## 5. A rota escolhida muda?

- **Cenário A (só duração):** Prefeitura Municipal de Toledo → Terminal Rodoviário de Toledo → C.Vale — Sede → Parque Ecológico de Toledo → UNIOESTE — Campus Toledo → Shopping Toledo → Catedral Sagrada Família → Prefeitura Municipal de Toledo
- **Cenário B, estado atual do projeto (assumido):** Prefeitura Municipal de Toledo → Catedral Sagrada Família → Terminal Rodoviário de Toledo → Shopping Toledo → UNIOESTE — Campus Toledo → Parque Ecológico de Toledo → C.Vale — Sede → Prefeitura Municipal de Toledo
- **Cenário B, combinado (fuel + pneu empíricos):** Prefeitura Municipal de Toledo → Catedral Sagrada Família → Terminal Rodoviário de Toledo → Shopping Toledo → UNIOESTE — Campus Toledo → Parque Ecológico de Toledo → C.Vale — Sede → Prefeitura Municipal de Toledo
- Cenário B (atual) difere de A? **Sim** · Cenário B (combinado) difere de A? **Sim** · Cenário B (combinado) difere de Cenário B (atual)? **Não**

## 6. Métricas agregadas — estado atual vs combinado

Todas avaliadas sobre a rota do Cenário A (mesma rota, isolando o efeito da troca de perfil):

| Métrica | Estado atual do projeto | Combinado (fuel + pneu empíricos) |
|---|---|---|
| Combustível | 3.20 L | 3.92 L |
| Desgaste de pneu | R$ 2.19 | R$ 2.99 |
| Custo total (fórmula B) | R$ 36.20 | R$ 41.40 |

Gap interno de cada perfil (rota do Cenário A vs. sua própria rota ótima do Cenário B):

| Perfil | Gap (R$) | Gap (%) |
|---|---|---|
| Estado atual do projeto | R$ 0.10 | 0.26% |
| Combinado (fuel + pneu empíricos) | R$ 0.14 | 0.35% |

## 7. Conclusão

1. **Os dois efeitos vão na mesma direção neste log sintético:** consumo empírico 22.6% maior e vida útil de pneu empírica 26.7% menor do que o estado atual do projeto — ambos aumentam o custo por trecho, então o efeito combinado é maior do que qualquer um isolado (14.38% de aumento no custo total da rota do Cenário A, vs. os dois relatórios individuais). Isso não é garantido em geral — com outro log, consumo e vida útil de pneu poderiam divergir em direções opostas e se cancelar parcialmente.
2. **A rota escolhida pelo Cenário B não muda ao trocar o estado atual do projeto pelo perfil combinado neste cenário** — isso significa que, ao menos para esta geometria e carga específicas, os valores exatos de consumo e vida útil do pneu não foram decisivos juntos — mudou o custo total, mas não a ordem ótima.
3. **O que isso significa pra produção:** nenhuma dessas correções está plugada em `RouteController`/`RouteOptimizerServiceImpl` — os três relatórios deste pacote (combustível, pneu, combinado) validam a lógica com logs sintéticos. O próximo passo real seria plugar logs de abastecimento e troca de pneu de fato (ou pelo menos um piloto com poucos veículos reais) antes de considerar levar isso pra produção.
