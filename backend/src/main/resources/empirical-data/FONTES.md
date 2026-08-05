# Origem dos dados — `fuel-consumption-by-vehicle-type.csv` e `tire-replacements-by-vehicle-type.csv`

Os dois CSVs nesta pasta têm **registros individuais sintéticos** (linha por
abastecimento / troca de pneu — nenhuma nota fiscal real foi usada), mas a **média
agregada de cada tipo de veículo foi calibrada pra bater com dados reais publicados**
sobre esses veículos — ao contrário dos logs de exemplo usados nos relatórios
anteriores (`FuelConsumptionFromRefuelingExample`, `TireLifeFromReplacementLogExample`),
que eram números ilustrativos sem relação com nenhum veículo real específico.

Isso ainda **não é telemetria de frota real** (nenhum abastecimento ou troca aqui
aconteceu de fato) — é uma ponte entre "número totalmente inventado" e "dado real
coletado da própria operação": a média bate com fonte pública, mas a distribuição
abastecimento-a-abastecimento é construída, não medida.

## Combustível (`fuel-consumption-by-vehicle-type.csv`)

| Tipo | Veículo de referência | Consumo-fonte | L/100km alvo | L/100km no CSV |
|---|---|---|---|---|
| `micro_caminhao` | Iveco Daily (classe 3/4, chassi-cabine) | 7,9–8,5 km/l (urbano/rodoviário) | ~12,5 | 12,49 |
| `van_entrega` | Fiat Ducato Cargo/MaxiCargo | 9,7–10,0 km/l (rodoviário/urbano, carregada) | ~10,15 | 10,10 |
| `saveiro` | Volkswagen Saveiro Robust (gasolina) | 11,3–12,4 km/l (urbano/rodoviário) | ~8,44 | 8,44 |

Fontes: [Iveco Daily — consumo](https://www.cobli.co/blog/iveco-daily-consumo/),
[Iveco Daily 35S14 — ficha técnica](https://blog.caminhoesecarretas.com.br/2026/05/iveco-daily-35s14-ficha-tecnica/),
[Fiat Ducato — consumo (2005 a 2026)](https://carrosbr.com/fiat-ducato-consumo-combustivel/),
[Volkswagen Saveiro Robust CD 2026 — ficha técnica](https://nonamarcha.com.br/volkswagen-saveiro-robust-cd-2026-ficha-tecnica-consumo-tecnologias-e-preco/).

## Pneu (`tire-replacements-by-vehicle-type.csv`)

| Tipo | Vida útil-fonte | km alvo | Média no CSV (só `DESGASTE_NORMAL`) |
|---|---|---|---|
| `micro_caminhao` | Pneu de caminhão: ~60.000 km (até 80.000 km com uso cuidadoso) | ~60.000 | 59.500 |
| `van_entrega` | **Sem fonte direta pra van especificamente** — estimado por proximidade ao caminhão leve, um pouco abaixo | ~55.000 (estimativa) | 54.833 |
| `saveiro` | **Sem fonte direta pra picape especificamente** — estimado pela regra geral de pneu de veículo de passeio (~40–50 mil km) | ~45.000 (estimativa) | 45.000 |

Fonte (só para o caminhão — a única com número direto): [Qual a média de km que um pneu pode rodar — Saga Pneus](https://sagapneus.com.br/blog/post/qual-a-media-de-km-que-um-pneu-pode-rodar.html).

Van e Saveiro **não têm fonte publicada específica encontrada** — os alvos usados são
estimativas por analogia (van perto do caminhão leve, picape perto de veículo de
passeio comum). Diferente do combustível, onde as três fontes são diretas e
específicas do modelo. Se aparecer uma fonte melhor, só recalibrar os `km_troca` do
CSV pra bater com o novo alvo — a estrutura não muda.

## Cada linha do CSV mapeia direto pros records dos calculadores

- `fuel-consumption-by-vehicle-type.csv` → `EmpiricalFuelConsumptionCalculator.RefuelingRecord(date, litersRefueled, kmSincePrevious)`, uma lista por `tipo_veiculo`.
- `tire-replacements-by-vehicle-type.csv` → `EmpiricalTireLifeCalculator.TireReplacementRecord(kmInstalacao, kmTroca, motivoTroca, posicaoEixo)`, uma lista por `tipo_veiculo`.

Nenhum código lê esses CSVs ainda — é dado preparado, não pipeline. Ainda falta um
leitor (CSV → `List<RefuelingRecord>` / `List<TireReplacementRecord>`) pra plugar isso
nos calculadores existentes.
