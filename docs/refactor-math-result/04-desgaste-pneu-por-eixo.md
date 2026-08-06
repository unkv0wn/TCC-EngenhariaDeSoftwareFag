# Desgaste de Pneu por Posição de Eixo

Gerado em: 2026-08-05 00:58:06

## Resumo do que foi feito

Esta era a **única lacuna real** entre as dimensões revisadas — as outras já estavam implementadas. O `CostMatrixBuilder` calculava desgaste de pneu com uma vida útil única multiplicada pelo número de **eixos**:

```
tireWearFraction = (1 / tireLifeKm) × axleCount × (1 + 0,5 × loadFactor) × wearMultiplier
```

Duas coisas estavam erradas aí:

1. **Todo eixo desgastava igual.** Dianteiro, tração e reboque têm desgaste fisicamente diferente (esterçamento, torque, e só peso, respectivamente) — tratá-los como um número médio apaga essa diferença.
2. **`axleCount` fazia o papel de quantidade de pneus.** Um caminhão de 3 eixos roda com 10 pneus, não 3. Como o resultado é multiplicado pelo custo de **um** pneu, o modelo antigo subestimava o custo de pneu em cerca de 3x.

A fórmula passou a somar por posição, cada uma com sua vida útil e sua contagem real de pneus:

```
tireWearFraction = Σ_posição [ (1 / (tireLifeKm × fatorVida_posição)) × nºPneus_posição ]
                    × (1 + 0,5 × loadFactor) × wearMultiplier
```

Implementado em `AxlePosition` (fatores de vida), `AxleLayout` (contagem de pneus) e `CostMatrixBuilder.baseTireWearPerKm`. Coberto por `AxleTireWearTest` (8 testes, `mvn test`).

## 1. Constantes do modelo

### Fator de vida útil por posição

Multiplicador sobre o `tireLifeKm` da classe. Abaixo de 1,0 = pneu dura menos.

| Posição | Fator | Razão |
|---|---|---|
| `DIANTEIRO` | 0.85 | Esterçamento e sensibilidade a alinhamento |
| `TRACAO` | 1.00 | Referência — absorve o torque do motor |
| `REBOQUE` | 1.25 | Só carrega peso, sem torque nem esterçamento |

`TRACAO` é a referência (1,00) porque o log empírico em `empirical-data/tire-replacements-by-vehicle-type.csv` é quase todo de eixo de tração — ancorar ali mantém a calibração empírica existente válida sem reinterpretá-la contra outra base. Os outros dois fatores são suposições plausíveis documentadas, mesmo caveat de todas as constantes de `VehicleProfile`: nenhuma telemetria de frota os separa ainda.

### Configuração de pneus por classe

| Perfil | Eixos | Dianteiro | Tração | Reboque | Total de pneus |
|---|---|---|---|---|---|
| Leve (2 eixos) | 2 | 2 | 4 | 0 | **6** |
| Médio (3 eixos) | 3 | 2 | 8 | 0 | **10** |
| Pesado (5 eixos) | 5 | 2 | 8 | 8 | **18** |

Configuração brasileira padrão: eixo direcional simples (2 pneus) e eixos traseiros em rodado duplo (4 pneus cada). Fixa por classe — calibração empírica muda quanto um pneu dura, nunca quantos pneus o caminhão tem.

## 2. Modelo anterior vs. atual (trecho de 100 km, via ARTERIAL, vazio)

A coluna "anterior" reconstrói a fórmula antiga apenas para comparação — ela não existe mais no código.

| Perfil | Anterior (R$) | Atual (R$) | Diferença | Fator |
|---|---|---|---|---|
| Leve (2 eixos) | R$ 3.60 | R$ 11.44 | +R$ 7.84 | 3.18x |
| Médio (3 eixos) | R$ 9.00 | R$ 31.06 | +R$ 22.06 | 3.45x |
| Pesado (5 eixos) | R$ 13.75 | R$ 46.07 | +R$ 32.32 | 3.35x |

O salto vem quase todo da troca de `axleCount` por contagem real de pneus — o fator de cada classe fica perto da sua razão pneus/eixos (leve 6/2 = 3,0; médio 10/3 = 3,3; pesado 18/5 = 3,6). Os fatores de vida por posição modulam isso nos dois sentidos: o eixo dianteiro empurra o fator para cima (dura 15% menos), enquanto os 8 pneus de reboque do perfil pesado o puxam para baixo (duram 25% mais), e é por isso que o pesado sobe menos que o médio apesar de ter mais pneus por eixo.

## 3. De onde vem o desgaste (trecho de 100 km, via ARTERIAL, vazio)

| Perfil | Posição | Pneus | Vida útil efetiva | Custo (R$) | % do total |
|---|---|---|---|---|---|
| Leve (2 eixos) | `DIANTEIRO` | 2 | 42,500 km | R$ 4.24 | 37.0% |
| Leve (2 eixos) | `TRACAO` | 4 | 50,000 km | R$ 7.20 | 63.0% |
| Médio (3 eixos) | `DIANTEIRO` | 2 | 51,000 km | R$ 7.06 | 22.7% |
| Médio (3 eixos) | `TRACAO` | 8 | 60,000 km | R$ 24.00 | 77.3% |
| Pesado (5 eixos) | `DIANTEIRO` | 2 | 68,000 km | R$ 6.47 | 14.0% |
| Pesado (5 eixos) | `TRACAO` | 8 | 80,000 km | R$ 22.00 | 47.8% |
| Pesado (5 eixos) | `REBOQUE` | 8 | 100,000 km | R$ 17.60 | 38.2% |

O eixo de tração é a maior parcela em todas as classes — é onde estão a maioria dos pneus — mas no perfil pesado ele fica abaixo de 50%, porque os 8 pneus de reboque respondem por outros ~38%. O dianteiro pesa pouco no total apesar de ser o que desgasta mais rápido por pneu, porque são só 2.

## 4. Sensibilidade à carga e ao tipo de via

Custo de pneu num trecho de 100 km, variando ocupação e tipo de via.

| Perfil | Ocupação | Rodovia | Arterial | Urbana |
|---|---|---|---|---|
| Leve (2 eixos) | 0% | R$ 9.15 | R$ 11.44 | R$ 16.01 |
| Leve (2 eixos) | 50% | R$ 11.44 | R$ 14.29 | R$ 20.01 |
| Leve (2 eixos) | 100% | R$ 13.72 | R$ 17.15 | R$ 24.01 |
| Médio (3 eixos) | 0% | R$ 24.85 | R$ 31.06 | R$ 43.48 |
| Médio (3 eixos) | 50% | R$ 31.06 | R$ 38.82 | R$ 54.35 |
| Médio (3 eixos) | 100% | R$ 37.27 | R$ 46.59 | R$ 65.22 |
| Pesado (5 eixos) | 0% | R$ 36.86 | R$ 46.07 | R$ 64.50 |
| Pesado (5 eixos) | 50% | R$ 46.07 | R$ 57.59 | R$ 80.62 |
| Pesado (5 eixos) | 100% | R$ 55.28 | R$ 69.11 | R$ 96.75 |

Carga cheia acrescenta 50% ao desgaste; via urbana acrescenta 40% sobre a arterial. Combinados, um trecho urbano carregado desgasta **2,6x** o que o mesmo trecho em rodovia vazio desgasta (2,1x se a comparação for contra a arterial vazia) — a razão de o custo de pneu conseguir mudar a rota escolhida.
