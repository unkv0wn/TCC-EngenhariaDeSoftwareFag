# Consumo Médio de Combustível

Gerado em: 2026-08-05 00:58:06

## Resumo do que foi feito

**Nada mudou nesta dimensão neste refactor** — ela já estava implementada, incluindo a calibração empírica por janela móvel. Este relatório revalida o comportamento e documenta os números atuais.

O consumo parte de uma constante por classe (`baseFuelConsumptionLPer100Km`, medida a vazio em via arterial) e é ajustado por carga e tipo de via:

```
consumoAjustado = baseFuelConsumptionLPer100Km × (1 + 0,30 × loadFactor) × fuelMultiplier
fuelLiters      = distanceKm × consumoAjustado / 100
```

`EmpiricalFuelConsumptionCalculator` pode substituir a constante base por um valor derivado do log de abastecimento (janela móvel dos últimos 5 abastecimentos, razão litros/km ponderada por distância). Coberto por `EmpiricalFuelConsumptionCalculatorTest`.

## 1. Consumo base por classe

| Perfil | Consumo base | Equivalente | Preço diesel |
|---|---|---|---|
| Leve (2 eixos) | 10.0 L/100km | 10.00 km/L | R$ 6.10/L |
| Médio (3 eixos) | 15.0 L/100km | 6.67 km/L | R$ 6.10/L |
| Pesado (5 eixos) | 32.0 L/100km | 3.13 km/L | R$ 6.10/L |

Valores a vazio, em via arterial — a referência a partir da qual os multiplicadores abaixo operam.

## 2. Consumo ajustado por carga e via (trecho de 100 km)

| Perfil | Ocupação | Rodovia (×0,85) | Arterial (×1,00) | Urbana (×1,30) |
|---|---|---|---|---|
| Leve (2 eixos) | 0% | 8.5 L (R$ 51.85) | 10.0 L (R$ 61.00) | 13.0 L (R$ 79.30) |
| Leve (2 eixos) | 100% | 11.1 L (R$ 67.41) | 13.0 L (R$ 79.30) | 16.9 L (R$ 103.09) |
| Médio (3 eixos) | 0% | 12.8 L (R$ 77.77) | 15.0 L (R$ 91.50) | 19.5 L (R$ 118.95) |
| Médio (3 eixos) | 100% | 16.6 L (R$ 101.11) | 19.5 L (R$ 118.95) | 25.4 L (R$ 154.64) |
| Pesado (5 eixos) | 0% | 27.2 L (R$ 165.92) | 32.0 L (R$ 195.20) | 41.6 L (R$ 253.76) |
| Pesado (5 eixos) | 100% | 35.4 L (R$ 215.70) | 41.6 L (R$ 253.76) | 54.1 L (R$ 329.89) |

O pior caso (carregado, urbano) consome **1,99x** o melhor (vazio, rodovia) no mesmo trecho de mesma distância — praticamente o dobro. Essa diferença é invisível para o Cenário A, que só enxerga duração.

## 3. Calibração empírica (janela móvel)

| Fonte | Consumo | Custo por 100 km |
|---|---|---|
| Constante assumida | 15.00 L/100km | R$ 91.50 |
| Log de abastecimento (últimos 5) | 18.39 L/100km | R$ 112.18 |
| **Diferença** | **+3.39 L/100km** | **+22.6%** |

A janela móvel existe para o número acompanhar o estado atual do veículo — motor desgastando, pneu descalibrado, rota mudando de perfil — em vez de diluir isso numa média histórica que nunca esquece. Cinco abastecimentos é curto o bastante para reagir e longo o bastante para um tanque atípico não dominar o resultado.
