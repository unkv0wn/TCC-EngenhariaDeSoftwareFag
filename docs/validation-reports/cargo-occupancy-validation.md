# Validação de Ocupação de Carga (Peso x Cubagem) — Relatório Detalhado

Gerado em: 2026-07-30 22:24:55

Este relatório documenta, caso a caso, a lógica de `CargoOccupancy` — quem decide, entre peso (kg) e volume (m³), qual dimensão limita a carga de um veículo, e se a carga é fisicamente viável. Os mesmos casos são verificados como testes automatizados em `CargoOccupancyTest` (JUnit, `mvn test`); este documento existe para apresentar a mesma evidência de forma legível, e amplia a cobertura para os três perfis de veículo (`VehicleProfile.light()/medium()/heavy()`), não só o perfil médio usado no teste.

**Fórmula:** dada uma carga com peso `cargoWeightKg` e volume `cargoVolumeM3`,

```
fraçãoPeso   = cargoWeightKg / capacidadeKg
fraçãoVolume = cargoVolumeM3 / capacidadeM3
restrição    = peso, se fraçãoPeso >= fraçãoVolume; volume, caso contrário
ocupação     = max(fraçãoPeso, fraçãoVolume)          // não limitada — usada para viabilidade
loadFactor   = min(ocupação, 1.0)                      // limitada — alimenta as fórmulas de custo
viável       = ocupação <= 1.0
```

## 1. Perfis de veículo avaliados

| Perfil | Eixos | Capacidade (peso) | Capacidade (volume) |
|---|---|---|---|
| Leve (2 eixos) | 2 | 1500 kg | 8.0 m³ |
| Médio (3 eixos) | 3 | 4000 kg | 25.0 m³ |
| Pesado (5 eixos) | 5 | 12000 kg | 90.0 m³ |

## 2. Cargas avaliadas

| Caso | Descrição | Peso | Volume |
|---|---|---|---|
| Carga densa (peso-limitante) | Ex.: barras de aço — pesada, ocupa pouco espaço. | 3600 kg | 5.0 m³ |
| Carga volumosa (volume-limitante) | Ex.: espuma de embalagem — leve, ocupa muito espaço. | 800 kg | 22.5 m³ |
| Excesso de peso | Peso além da capacidade; volume folgado. | 5000 kg | 10.0 m³ |
| Excesso de volume | Volume além da capacidade; peso folgado. | 500 kg | 30.0 m³ |
| Carga vazia | Veículo sem carga — caso de referência (ocupação zero). | 0 kg | 0.0 m³ |

Cada carga acima é avaliada contra os três perfis de veículo — o peso/volume da carga não muda, só a capacidade que a recebe, exatamente como aconteceria se a operação precisasse decidir qual veículo escalar para um pedido.

## 3. Ocupação por caso × perfil de veículo

| Caso | Perfil | Fração peso | Fração volume | Restrição | Ocupação | loadFactor (custo) | Viável? |
|---|---|---|---|---|---|---|---|
| Carga densa (peso-limitante) | Leve (2 eixos) | 240.0% | 62.5% | Peso | 240.0% | 1.000 | **Não** |
| Carga densa (peso-limitante) | Médio (3 eixos) | 90.0% | 20.0% | Peso | 90.0% | 0.900 | Sim |
| Carga densa (peso-limitante) | Pesado (5 eixos) | 30.0% | 5.6% | Peso | 30.0% | 0.300 | Sim |
| Carga volumosa (volume-limitante) | Leve (2 eixos) | 53.3% | 281.3% | Volume | 281.3% | 1.000 | **Não** |
| Carga volumosa (volume-limitante) | Médio (3 eixos) | 20.0% | 90.0% | Volume | 90.0% | 0.900 | Sim |
| Carga volumosa (volume-limitante) | Pesado (5 eixos) | 6.7% | 25.0% | Volume | 25.0% | 0.250 | Sim |
| Excesso de peso | Leve (2 eixos) | 333.3% | 125.0% | Peso | 333.3% | 1.000 | **Não** |
| Excesso de peso | Médio (3 eixos) | 125.0% | 40.0% | Peso | 125.0% | 1.000 | **Não** |
| Excesso de peso | Pesado (5 eixos) | 41.7% | 11.1% | Peso | 41.7% | 0.417 | Sim |
| Excesso de volume | Leve (2 eixos) | 33.3% | 375.0% | Volume | 375.0% | 1.000 | **Não** |
| Excesso de volume | Médio (3 eixos) | 12.5% | 120.0% | Volume | 120.0% | 1.000 | **Não** |
| Excesso de volume | Pesado (5 eixos) | 4.2% | 33.3% | Volume | 33.3% | 0.333 | Sim |
| Carga vazia | Leve (2 eixos) | 0.0% | 0.0% | Peso | 0.0% | 0.000 | Sim |
| Carga vazia | Médio (3 eixos) | 0.0% | 0.0% | Peso | 0.0% | 0.000 | Sim |
| Carga vazia | Pesado (5 eixos) | 0.0% | 0.0% | Peso | 0.0% | 0.000 | Sim |

Repare como a mesma carga muda de restrição (peso ↔ volume) e de viabilidade dependendo só do veículo escolhido — ex.: "Carga volumosa" costuma ser volume-limitante em veículos pequenos e caber tranquilamente em um veículo pesado. Nenhum destes casos é rejeitado pelo `AStarWaypointOptimizer` hoje — ele roteia cargas inviáveis normalmente, porque não recebe nenhuma informação de capacidade.

## 4. Impacto no custo do trecho (100 km, via ARTERIAL)

Mesmo trecho fixo, variando só a carga — isola o efeito de `loadFactor` nas fórmulas de combustível e desgaste de pneu (`CostMatrixBuilder`). A duração (Cenário A, comportamento de produção hoje) é idêntica em todas as linhas porque não depende de carga.

| Caso | Perfil | Duração | Combustível | Desgaste de pneu |
|---|---|---|---|---|
| Carga densa (peso-limitante) | Leve (2 eixos) | 120.0 min | 13.00 L | R$ 5.40 |
| Carga densa (peso-limitante) | Médio (3 eixos) | 120.0 min | 19.05 L | R$ 13.05 |
| Carga densa (peso-limitante) | Pesado (5 eixos) | 120.0 min | 34.88 L | R$ 15.81 |
| Carga volumosa (volume-limitante) | Leve (2 eixos) | 120.0 min | 13.00 L | R$ 5.40 |
| Carga volumosa (volume-limitante) | Médio (3 eixos) | 120.0 min | 19.05 L | R$ 13.05 |
| Carga volumosa (volume-limitante) | Pesado (5 eixos) | 120.0 min | 34.40 L | R$ 15.47 |
| Excesso de peso | Leve (2 eixos) | 120.0 min | 13.00 L | R$ 5.40 |
| Excesso de peso | Médio (3 eixos) | 120.0 min | 19.50 L | R$ 13.50 |
| Excesso de peso | Pesado (5 eixos) | 120.0 min | 36.00 L | R$ 16.61 |
| Excesso de volume | Leve (2 eixos) | 120.0 min | 13.00 L | R$ 5.40 |
| Excesso de volume | Médio (3 eixos) | 120.0 min | 19.50 L | R$ 13.50 |
| Excesso de volume | Pesado (5 eixos) | 120.0 min | 35.20 L | R$ 16.04 |
| Carga vazia | Leve (2 eixos) | 120.0 min | 10.00 L | R$ 3.60 |
| Carga vazia | Médio (3 eixos) | 120.0 min | 15.00 L | R$ 9.00 |
| Carga vazia | Pesado (5 eixos) | 120.0 min | 32.00 L | R$ 13.75 |

## 5. Conclusão

1. **A restrição relevante depende do veículo, não só da carga.** A mesma carga pode ser peso-limitante num veículo e volume-limitante noutro — validar só peso (como o sistema faria hoje, se validasse algo) esconde metade dos casos de estouro de capacidade.
2. **O algoritmo de roteamento é cego a capacidade.** Todos os casos acima, viáveis ou não, são roteados normalmente pelo `AStarWaypointOptimizer` — a coluna "Viável?" não influencia em nada a rota escolhida hoje.
3. **Carga mais pesada/volumosa realmente encarece o trecho.** As colunas de combustível e desgaste na seção 4 crescem com `loadFactor`, confirmando que a fórmula reage à ocupação como esperado; a duração não muda, confirmando que o Cenário A (produção atual) de fato ignora esse efeito.
4. **Cobertura de teste:** os cinco casos da seção 2, avaliados contra o perfil médio, são também verificados como asserts em `CargoOccupancyTest` (`mvn test -Dtest=CargoOccupancyTest`) — este relatório amplia a mesma verificação para os perfis leve e pesado, sem introduzir lógica nova.
