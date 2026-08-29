# Capacidade do Veículo — Peso (kg) e Volume (m³)

Gerado em: 2026-08-05 00:58:06

## Resumo do que foi feito

**Nada mudou nesta dimensão neste refactor** — ela já estava implementada. Este relatório revalida o comportamento sob a matemática de pneu corrigida (ver `04-desgaste-pneu-por-eixo.md`), que altera os valores absolutos da última seção.

O modelo segue a prática de frete de "peso x cubagem": uma carga é limitada pela dimensão mais restritiva, não pelo peso sozinho. Carga leve e volumosa enche o baú muito antes de atingir o limite de peso; carga densa faz o inverso.

```
fraçãoPeso   = cargoWeightKg / capacityKg
fraçãoVolume = cargoVolumeM3 / capacityM3
ocupação     = max(fraçãoPeso, fraçãoVolume)   // sem limite — decide viabilidade
loadFactor   = min(ocupação, 1,0)              // limitado — alimenta as fórmulas de custo
```

`loadFactor` é limitado a 1,0 porque as fórmulas de combustível e desgaste só foram calibradas nessa faixa — extrapolá-las para uma carga inviável produziria número sem lastro. Implementado em `CargoOccupancy`, coberto por `CargoOccupancyTest`.

## 1. Capacidade por classe

| Perfil | Capacidade (peso) | Capacidade (volume) | Densidade de equilíbrio |
|---|---|---|---|
| Leve (2 eixos) | 1,500 kg | 8.0 m³ | 188 kg/m³ |
| Médio (3 eixos) | 4,000 kg | 25.0 m³ | 160 kg/m³ |
| Pesado (5 eixos) | 12,000 kg | 90.0 m³ | 133 kg/m³ |

A **densidade de equilíbrio** é a densidade de carga em que peso e volume esgotam juntos. Carga mais densa que isso é limitada por peso; menos densa, por volume. Repare que ela cai conforme o veículo cresce — o baú cresce mais rápido que a capacidade de carga, então caminhão grande satura por peso com mais facilidade.

## 2. Qual dimensão limita, por carga × veículo

| Carga | Perfil | Fração peso | Fração volume | Restrição | loadFactor | Viável? |
|---|---|---|---|---|---|---|
| Densa (3,600 kg / 5.0 m³) | Leve (2 eixos) | 240% | 63% | Peso | 1.00 | **Não** |
| Densa (3,600 kg / 5.0 m³) | Médio (3 eixos) | 90% | 20% | Peso | 0.90 | Sim |
| Densa (3,600 kg / 5.0 m³) | Pesado (5 eixos) | 30% | 6% | Peso | 0.30 | Sim |
| Volumosa (800 kg / 22.5 m³) | Leve (2 eixos) | 53% | 281% | Volume | 1.00 | **Não** |
| Volumosa (800 kg / 22.5 m³) | Médio (3 eixos) | 20% | 90% | Volume | 0.90 | Sim |
| Volumosa (800 kg / 22.5 m³) | Pesado (5 eixos) | 7% | 25% | Volume | 0.25 | Sim |
| Excesso de peso (5,000 kg / 10.0 m³) | Leve (2 eixos) | 333% | 125% | Peso | 1.00 | **Não** |
| Excesso de peso (5,000 kg / 10.0 m³) | Médio (3 eixos) | 125% | 40% | Peso | 1.00 | **Não** |
| Excesso de peso (5,000 kg / 10.0 m³) | Pesado (5 eixos) | 42% | 11% | Peso | 0.42 | Sim |
| Excesso de volume (500 kg / 30.0 m³) | Leve (2 eixos) | 33% | 375% | Volume | 1.00 | **Não** |
| Excesso de volume (500 kg / 30.0 m³) | Médio (3 eixos) | 13% | 120% | Volume | 1.00 | **Não** |
| Excesso de volume (500 kg / 30.0 m³) | Pesado (5 eixos) | 4% | 33% | Volume | 0.33 | Sim |

A mesma carga troca de restrição conforme o veículo — validar só peso deixaria passar metade dos casos de estouro. O `AStarWaypointOptimizer` continua cego a isso: roteia cargas inviáveis normalmente, porque não recebe informação de capacidade.

## 3. Como `loadFactor` propaga para o custo (100 km, via ARTERIAL)

| Perfil | loadFactor | Combustível | Custo pneu | Custo total do trecho |
|---|---|---|---|---|
| Leve (2 eixos) | 0.00 | 10.00 L | R$ 11.44 | R$ 132.44 |
| Leve (2 eixos) | 0.50 | 11.50 L | R$ 14.29 | R$ 144.44 |
| Leve (2 eixos) | 1.00 | 13.00 L | R$ 17.15 | R$ 156.45 |
| Médio (3 eixos) | 0.00 | 15.00 L | R$ 31.06 | R$ 192.56 |
| Médio (3 eixos) | 0.50 | 17.25 L | R$ 38.82 | R$ 214.05 |
| Médio (3 eixos) | 1.00 | 19.50 L | R$ 46.59 | R$ 235.54 |
| Pesado (5 eixos) | 0.00 | 32.00 L | R$ 46.07 | R$ 325.27 |
| Pesado (5 eixos) | 0.50 | 36.80 L | R$ 57.59 | R$ 366.07 |
| Pesado (5 eixos) | 1.00 | 41.60 L | R$ 69.11 | R$ 406.87 |

Carga cheia acrescenta 30% ao combustível e 50% ao desgaste de pneu. O custo de motorista não se move — só depende da duração, que independe da carga. É por isso que o Cenário A (duração pura, comportamento de produção hoje) é completamente insensível a quanto o caminhão está carregado.
