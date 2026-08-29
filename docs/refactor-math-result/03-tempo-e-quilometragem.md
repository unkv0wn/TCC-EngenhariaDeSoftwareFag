# Tempo e Quilometragem

Gerado em: 2026-08-05 00:58:06

## Resumo do que foi feito

**Nada mudou nestas dimensões neste refactor** — ambas já estavam implementadas. Este relatório revalida o comportamento e mostra a composição do custo sob a matemática de pneu corrigida (ver `04-desgaste-pneu-por-eixo.md`).

As duas estão no mesmo documento porque **não são entradas independentes**: a duração é derivada da distância e do tipo de via, não medida separadamente.

```
durationSec   = distanceKm / roadType.avgSpeedKmh × 3600
timeCostReais = (durationSec / 3600) × driverCostPerHourReais
```

A quilometragem é a entrada primária — entra em todas as três parcelas de custo (motorista via duração, combustível, pneu). O tempo só entra no custo de motorista. **A duração é o único componente que o Cenário A (produção hoje) enxerga.**

## 1. Velocidade por tipo de via

| Via | Velocidade média | Tempo por 100 km |
|---|---|---|
| RODOVIA | 80 km/h | 75 min |
| ARTERIAL | 50 km/h | 120 min |
| URBANA | 25 km/h | 240 min |

Mesma distância, tempos muito diferentes: 100 km urbanos levam **3,2x** o tempo de 100 km em rodovia. É essa não-proporcionalidade entre distância e duração que torna o experimento informativo — se tempo e distância fossem proporcionais, otimizar por um seria idêntico a otimizar pelo outro.

## 2. Escala com a distância (Médio (3 eixos), vazio, via arterial)

| Distância | Duração | Custo motorista | Combustível | Pneu | Total |
|---|---|---|---|---|---|
| 10 km | 12 min | R$ 7.00 | R$ 9.15 | R$ 3.11 | R$ 19.26 |
| 50 km | 60 min | R$ 35.00 | R$ 45.75 | R$ 15.53 | R$ 96.28 |
| 100 km | 120 min | R$ 70.00 | R$ 91.50 | R$ 31.06 | R$ 192.56 |
| 250 km | 300 min | R$ 175.00 | R$ 228.75 | R$ 77.65 | R$ 481.40 |
| 500 km | 600 min | R$ 350.00 | R$ 457.50 | R$ 155.29 | R$ 962.79 |

Todas as parcelas escalam linearmente com a distância, então a **proporção** entre elas é constante para um dado tipo de via. O que muda a proporção é o tipo de via e a carga — não o comprimento do trecho.

## 3. Composição do custo por tipo de via (100 km, vazio)

| Perfil | Via | Motorista | Combustível | Pneu | Total |
|---|---|---|---|---|---|
| Leve (2 eixos) | RODOVIA | 38% | 53% | 9% | R$ 98.50 |
| Leve (2 eixos) | ARTERIAL | 45% | 46% | 9% | R$ 132.44 |
| Leve (2 eixos) | URBANA | 56% | 37% | 7% | R$ 215.31 |
| Médio (3 eixos) | RODOVIA | 30% | 53% | 17% | R$ 146.37 |
| Médio (3 eixos) | ARTERIAL | 36% | 48% | 16% | R$ 192.56 |
| Médio (3 eixos) | URBANA | 46% | 39% | 14% | R$ 302.43 |
| Pesado (5 eixos) | RODOVIA | 21% | 65% | 14% | R$ 255.28 |
| Pesado (5 eixos) | ARTERIAL | 26% | 60% | 14% | R$ 325.27 |
| Pesado (5 eixos) | URBANA | 35% | 52% | 13% | R$ 486.26 |

Conforme a via piora, a parcela de motorista cresce e a de combustível encolhe — a duração dispara enquanto a distância é a mesma. Mas o ponto de virada depende da classe: no perfil leve o motorista passa a dominar em via urbana (56%), enquanto no pesado o combustível continua sendo a maior parcela mesmo no urbano (52%), porque o consumo de uma carreta é alto demais para o tempo compensar. Em rodovia o combustível é a maior parcela em todas as classes.

**É exatamente por isso que otimizar por duração pura pode escolher uma rota diferente de otimizar por custo real** — as duas ordenam os trechos por critérios que não são proporcionais entre si, e o desalinhamento muda de tamanho conforme o veículo.
