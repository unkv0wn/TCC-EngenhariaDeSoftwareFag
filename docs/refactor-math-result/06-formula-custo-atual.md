# Fórmula Matemática do Modelo de Custo Atual

Escrito em: 2026-08-07
Fonte: `backend/src/main/java/com/routewise/validation/CostMatrixBuilder.java` (transcrito
do código, não parafraseado). Cobre o que já está implementado — não inclui a proposta de
penalidade por peso em `05-penalidade-velocidade-peso-proposta.md`.

## 1. Duração (trecho $i \to j$)

$$
\text{durationSec}_{ij} = \frac{\text{distanceKm}_{ij}}{\text{roadType}_{ij}.\text{avgSpeedKmh}} \times 3600
$$

Só depende de distância e velocidade média do tipo de via — **não depende do veículo**.
É essa independência que faz `costA` (Seção 5) sair idêntico para leve/médio/pesado no
mesmo trecho.

## 2. Fator de carga (`loadFactor`)

$$
\text{weightFraction} = \frac{\text{cargoWeightKg}}{\text{capacityKg}}, \qquad
\text{volumeFraction} = \frac{\text{cargoVolumeM}^3}{\text{capacityM}^3}
$$

$$
\text{loadFactor} = \min\Big(\max(\text{weightFraction},\ \text{volumeFraction}),\ 1\Big)
$$

Usa o que for mais restritivo entre peso e volume, limitado a 100% (carga que excede a
capacidade não gera `loadFactor > 1`). Constante por cenário — a carga não muda ao longo
da rota, então é calculada uma vez, não por trecho.

## 3. Combustível

$$
\text{consumoAjustado}_{ij} = \text{baseFuelConsumptionLPer100Km} \times (1 + 0{,}30 \cdot \text{loadFactor}) \times \text{roadType}_{ij}.\text{fuelMultiplier}
$$

$$
\text{fuelLiters}_{ij} = \text{distanceKm}_{ij} \times \frac{\text{consumoAjustado}_{ij}}{100}
$$

$0{,}30$ (`LOAD_FUEL_FACTOR`) é o quanto a carga cheia aumenta o consumo-base — carga cheia
consome até 30% mais combustível que vazio, no mesmo tipo de via.

## 4. Desgaste de pneu

$$
\text{baseTireWearPerKm} = \sum_{p \,\in\, \{\text{DIANTEIRO},\ \text{TRACAO},\ \text{REBOQUE}\}} \frac{\text{tireCount}(p)}{\text{tireLifeKm} \times \text{lifeFactor}(p)}
$$

Depende só do perfil do veículo (contagem de pneus e fator de vida por posição de eixo),
calculado uma vez por perfil — não por trecho.

$$
\text{tireWearFraction}_{ij} = \text{baseTireWearPerKm} \times (1 + 0{,}50 \cdot \text{loadFactor}) \times \text{roadType}_{ij}.\text{wearMultiplier}
$$

$$
\text{tireWearReais}_{ij} = \text{distanceKm}_{ij} \times \text{tireWearFraction}_{ij} \times \text{tireReplacementCostPerTire}
$$

$0{,}50$ (`LOAD_WEAR_FACTOR`) é o quanto a carga cheia acelera o desgaste — mais agressivo
que o fator de combustível (0,30), porque peso afeta desgaste de pneu mais que consumo.

## 5. Custo por trecho — Cenário A vs. Cenário B

$$
\text{timeCostReais}_{ij} = \frac{\text{durationSec}_{ij}}{3600} \times \text{driverCostPerHourReais}
$$

$$
\text{fuelCostReais}_{ij} = \text{fuelLiters}_{ij} \times \text{fuelPricePerLiter}
$$

$$
\boxed{\text{costA}_{ij} = \text{durationSec}_{ij}}
$$

$$
\boxed{\text{costB}_{ij} = \text{timeCostReais}_{ij} + \text{fuelCostReais}_{ij} + \text{tireWearReais}_{ij}}
$$

**Cenário A** = o que a produção já minimiza hoje (só tempo, em segundos — não convertido
pra R$). **Cenário B** = tudo convertido pra R$ e somado com peso implícito $(1,1,1)$, já
que as três parcelas usam a mesma unidade.

## 6. Exemplo numérico (referência)

Aplicação das fórmulas acima num trecho de 20 km, via ARTERIAL (avgSpeedKmh=50,
fuelMultiplier=1,0, wearMultiplier=1,0), carga 1000 kg / 5 m³:

| Perfil | loadFactor | costA (duração) | timeCostReais | fuelCostReais | tireWearReais | costB |
|---|---|---|---|---|---|---|
| Leve | 0,667 | 1440 s (24,0 min) | R$ 12,00 | R$ 14,64 | R$ 3,05 | R$ 29,69 |
| Médio | 0,250 | 1440 s (24,0 min) | R$ 14,00 | R$ 19,67 | R$ 6,99 | R$ 40,66 |
| Pesado | 0,083 | 1440 s (24,0 min) | R$ 16,80 | R$ 40,02 | R$ 9,60 | R$ 66,41 |

`costA` é idêntico nos três porque a fórmula da Seção 1 não usa `loadFactor` nem
`VehicleProfile` — só `costB` varia, e só porque combustível/pneu (Seções 3–4) usam
`loadFactor`. Ver `05-penalidade-velocidade-peso-proposta.md` para a proposta de corrigir
essa lacuna.
