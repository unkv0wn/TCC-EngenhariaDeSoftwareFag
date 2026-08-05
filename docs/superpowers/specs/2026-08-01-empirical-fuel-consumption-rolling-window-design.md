# Consumo de Combustível Empírico via Janela Móvel — Design Spec

Data: 2026-08-01
Escopo: `backend/` (pacote `com.routewise.validation`)

## Contexto

`VehicleProfile.baseFuelConsumptionLPer100Km` é hoje uma constante documentada como
"suposição plausível, não telemetria real" (ver Javadoc de `VehicleProfile`).
`FuelConsumptionFromRefuelingExample` já prova que dá pra substituir esse valor fixo por
uma média empírica derivada de um log de abastecimento (`Σ litros / Σ km × 100`), sem
mexer em `CostMatrixBuilder` — só troca qual número entra na fórmula.

Esta spec resolve duas perguntas que ficaram em aberto naquele exemplo:

1. A média deve considerar **todo** o histórico de abastecimento, ou só os mais recentes?
2. O que o sistema faz quando o veículo é novo e ainda não tem abastecimento nenhum
   registrado?

## Decisão

### Janela móvel de 5 abastecimentos

Nem o último abastecimento isolado (ruidoso — um único tanque de rodovia vs. cidade
varia ~20% no consumo observado) nem o histórico completo (estável, mas lento pra
refletir uma mudança real recente — pneu careca, motor precisando de revisão) são
ideais isoladamente. A janela móvel dos últimos 5 abastecimentos é o meio-termo:
suficiente pra cancelar o ruído de um tanque atípico, recente o bastante pra acompanhar
o estado atual do veículo.

Dentro da janela, mantém a mesma ponderação por distância que o exemplo original já
usava — soma de litros dividida pela soma de km, não a média simples das razões de cada
tanque (um tanque que rodou mais km deve pesar mais no resultado).

```
baseFuelConsumptionLPer100Km = ( Σ litrosAbastecidos / Σ kmRodados ) × 100
                                — somado só sobre os últimos 5 registros do log
```

### Partida a frio (cold start)

Um veículo recém-cadastrado não tem abastecimento nenhum registrado ainda — a janela
não pode simplesmente falhar ou usar zero. A política é degradar graciosamente:

| Nº de abastecimentos no log | Fonte do valor |
|---|---|
| 0 | Constante assumida da classe do veículo (`VehicleProfile.light/medium/heavy()`) |
| 1 a 4 | Média ponderada do que existir (janela parcial) |
| 5 ou mais | Janela completa dos últimos 5, deslizando a cada novo abastecimento |

Não há "modo de espera": o usuário nunca precisa acumular um número mínimo de registros
antes de ver algum benefício empírico — a janela cresce naturalmente de 0 até 5 e depois
desliza.

## Fora de escopo

- **Desgaste de pneu (`tireLifeKm`)** — tem mais variáveis (marca, se foi recapado,
  tipo de piso, alinhamento) que não se resolvem com o mesmo padrão de janela simples;
  fica como constante assumida por enquanto, revisitado numa spec própria quando houver
  um log de trocas (sintético ou real) pra discutir.
- Persistência do log de abastecimento (hoje é uma lista em memória no exemplo,
  `EmpiricalFuelConsumptionCalculator` só recebe a lista já montada — de onde ela vem é
  decisão de uma eventual spec de integração em produção).
- Qualquer alteração em `RouteController`, `RouteOptimizerServiceImpl` ou no frontend.

## Estrutura de código

| Classe | Responsabilidade |
|---|---|
| `EmpiricalFuelConsumptionCalculator` | novo — `RefuelingRecord` (data, litros, km desde o anterior) e `consumptionLPer100Km(log, fallback)`, aplicando janela móvel + cold start |
| `FuelConsumptionFromRefuelingExample` | atualizado — usa a classe acima em vez de somar o log inteiro inline; relatório em `docs/validation-reports/fuel-consumption-from-refueling.md` passa a mostrar qual parte do log está "na janela" |

## Testes

`EmpiricalFuelConsumptionCalculatorTest` cobre:

- Log vazio → retorna o valor de fallback (cold start puro).
- Log com 1 a 4 registros → usa a janela parcial (todos os registros existentes).
- Log com exatamente 5 registros → usa todos.
- Log com mais de 5 registros → ignora os mais antigos, considera só os últimos 5
  (verifica que um registro antigo "de fora" da janela não influencia o resultado, mas um
  registro recente sim).
- Ponderação por distância dentro da janela — replica o mesmo caso do exemplo original
  (tanques com km bem diferentes) pra confirmar que não é média simples das razões.
