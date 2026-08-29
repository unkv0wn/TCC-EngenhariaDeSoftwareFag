# Desgaste de Pneu Empírico por Classe e Posição do Eixo — Design Spec

Data: 2026-08-01
Escopo: `backend/` (pacote `com.routewise.validation`)

## Contexto

`VehicleProfile.tireLifeKm` é hoje uma constante única por classe de veículo, documentada
como suposição plausível, não telemetria real (mesmo caveat de todo `VehicleProfile`).
`CostMatrixBuilder` aplica essa vida útil uniformemente a todos os eixos do veículo
(`axleCount` só multiplica, não diferencia posição).

Diferente do combustível (`docs/superpowers/specs/2026-08-01-empirical-fuel-consumption-rolling-window-design.md`),
calibrar `tireLifeKm` empiricamente tem duas dificuldades estruturais próprias:

1. **Evento raro e terminal.** Um pneu só gera um dado (`kmUsado`) quando é trocado — não
   existe medição parcial contínua como o abastecimento. Uma janela por veículo individual
   levaria anos pra encher.
2. **Nem toda troca é desgaste.** Um pneu furado num buraco ou estourado no meio-fio não
   mede desgaste — mede um evento aleatório. Incluir isso numa média de "vida útil por
   desgaste" contamina o número com um fenômeno diferente do que se quer medir. E, ao
   contrário de um erro de digitação no log de combustível (visível pela janela pequena
   expor o outlier), esse é um dado real, só que sobre outra coisa — não dá pra confiar
   em "vai aparecer destoando" pra filtrar sozinho.

## Decisão

### Log de troca de pneu

Cada evento de troca registra:

| Campo | Descrição |
|---|---|
| `kmInstalacao` | odômetro no momento da instalação |
| `kmTroca` | odômetro no momento da troca (`kmUsado = kmTroca - kmInstalacao`) |
| `motivoTroca` | `DESGASTE_NORMAL` ou `DANO_ACIDENTE` |
| `posicaoEixo` | `DIANTEIRO` (direção), `TRACAO`, ou `REBOQUE` |
| `classeVeiculo` | `light` / `medium` / `heavy`, para agrupar |

Só eventos com `motivoTroca = DESGASTE_NORMAL` entram na média — `DANO_ACIDENTE` é
excluído estruturalmente pelo campo, não por inspeção visual do resultado.

`REBOQUE` só existe no perfil pesado (carreta articulada); leve e médio só têm
`DIANTEIRO` e `TRACAO`.

### Agregação por classe × posição do eixo, não por veículo individual

A mesma lógica de amostra pequena que levou a agregar por classe (em vez de por veículo)
se aplica com mais força aqui, porque troca de pneu é rara. Agrupar por
`(classeVeiculo, posicaoEixo)` junta os eventos de toda a frota daquela classe, enchendo
a amostra bem mais rápido do que esperar um único caminhão acumular trocas.

Isso tem um custo: adicionar `posicaoEixo` como dimensão extra **divide** a amostra de
novo (classe × posição em vez de só classe). Vale o custo porque a diferença de desgaste
entre dianteiro/tração/reboço é física, não ruído — dianteiro sofre mais com
esterço/alinhamento, tração com torque, reboço só carrega peso.

Cada grupo `(classe, posição)` calcula, sobre uma **janela móvel dos últimos 2 eventos**
`DESGASTE_NORMAL` do grupo:

```
tireLifeKm[classe][posicao] = média( kmUsado )
                               — só os últimos 2 eventos DESGASTE_NORMAL do grupo
```

Diferente do combustível, essa é uma **média simples**, não ponderada por distância:
cada evento de troca já é uma medição completa de "quanto km um pneu durou" (não uma
razão parcial tipo litros/km que precisa de ponderação). A janela em si é bem menor que
a do combustível (2 eventos, contra 5) — a raridade do evento, mesmo agregando a frota
inteira da classe, não sustenta uma janela grande; 2 já é suficiente pra não depender de
um único evento isolado (mesma razão de ter janela nenhuma) sem esfomear ainda mais uma
amostra que já é escassa.

### Partida a frio (cold start)

| Eventos `DESGASTE_NORMAL` no grupo `(classe, posição)` | Fonte do valor |
|---|---|
| 0 | `VehicleProfile.tireLifeKm` assumido da classe (mesmo valor pra todas as posições, já que não há dado ainda pra diferenciar) |
| 1 | Esse único evento |
| 2 ou mais | Janela móvel dos últimos 2 eventos do grupo |

### Fórmula no `CostMatrixBuilder`

Hoje:

```
tireWearFraction = (1 / tireLifeKm) × axleCount × (1 + 0.5 × loadFactor) × roadType.wearMultiplier
```

Passa a somar por posição, cada uma com sua vida útil e sua contagem de pneus nessa
posição do veículo:

```
tireWearFraction = Σ_posicao [ (1 / tireLifeKm[classe][posicao]) × nºPneusNaPosicao ]
                    × (1 + 0.5 × loadFactor) × roadType.wearMultiplier
```

`nºPneusNaPosição` é fixo por classe de veículo (ex: médio = 2 dianteiros + 4 de tração,
zero de reboque); não muda com calibração empírica, só o `tireLifeKm` de cada posição
muda.

## Fora de escopo

- Marca do pneu, qualidade de recapagem, alinhamento/calibragem — ruído não-modelado,
  mesma categoria de "ar-condicionado ligado" no combustível.
- Persistência real do log de trocas — decisão de uma eventual spec de integração em
  produção.
- Qualquer alteração em `RouteController`, `RouteOptimizerServiceImpl` ou no frontend.

> **Atualização (2026-08-05):** o refactor do `CostMatrixBuilder` para somar
> `tireWearFraction` por posição de eixo — listado aqui como fora de escopo — foi
> implementado na branch `review-math-a-star`. Ver
> `docs/refactor-math-result/04-desgaste-pneu-por-eixo.md`. Um achado da implementação:
> a fórmula antiga usava `axleCount` como se fosse a quantidade de pneus, subestimando o
> custo de pneu em ~3x; a contagem real por posição vive agora em `AxleLayout`.

## Estrutura de código

| Classe | Responsabilidade |
|---|---|
| `AxlePosition` | enum — `DIANTEIRO`, `TRACAO`, `REBOQUE`, cada um com seu `lifeFactor` (promovido para top-level em 2026-08-05, quando passou a ser usado fora do calculador) |
| `AxleLayout` | record — quantos pneus em cada posição, fixo por classe de veículo |
| `EmpiricalTireLifeCalculator.TireReplacementReason` | enum — `DESGASTE_NORMAL`, `DANO_ACIDENTE` |
| `EmpiricalTireLifeCalculator.TireReplacementRecord` | record — `kmInstalacao`, `kmTroca`, `motivoTroca`, `posicaoEixo` (implementado — `classeVeiculo` não entra no record; agrupar por classe é responsabilidade de quem monta o log passado ao calculador, mesma convenção do `EmpiricalFuelConsumptionCalculator`) |
| `EmpiricalTireLifeCalculator` | filtra `DESGASTE_NORMAL`, aplica a janela móvel de 2 eventos com fallback de partida a fria — implementado, com testes em `EmpiricalTireLifeCalculatorTest` e `EmpiricalCostCalculatorsCombinedTest` |
| `CostMatrixBuilder` | `baseTireWearPerKm` soma a fração de desgaste por posição de eixo (implementado em 2026-08-05, com testes em `AxleTireWearTest`) |
