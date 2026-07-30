# Análise de Sensibilidade dos Pesos da Função de Custo

Mesmos 500 cenários sintéticos e mesmo veículo (Médio, 3 eixos) em todas as linhas — só os pesos `(tempo, combustível, desgaste de pneu)` mudam na combinação `custoB = α·tempo + β·combustível + γ·desgaste`. Mostra o trade-off real: priorizar um fator tende a piorar os outros.

| Configuração | Pesos (α,β,γ) | Distância média | Duração média | Combustível médio | Desgaste médio (R$) | Muda rota vs. equilibrado |
|---|---|---|---|---|---|---|
| Só tempo (produção atual) | (1.0, 0.0, 0.0) | 185.27 km | 176.6 min | 29.11 L | R$ 18.36 | 29.0% |
| Equilibrado (1,1,1) — modelo atual | (1.0, 1.0, 1.0) | 175.27 km | 179.9 min | 28.18 L | R$ 17.93 | 0.0% |
| Prioriza combustível | (0.3, 2.0, 0.3) | 171.18 km | 184.7 min | 27.96 L | R$ 17.89 | 20.0% |
| Prioriza desgaste de pneu | (0.3, 0.3, 2.0) | 174.17 km | 180.8 min | 28.10 L | R$ 17.90 | 4.0% |
| Só combustível | (0.0, 1.0, 0.0) | 170.10 km | 187.0 min | 27.94 L | R$ 17.91 | 25.8% |
| Só desgaste de pneu | (0.0, 0.0, 1.0) | 172.15 km | 183.2 min | 27.99 L | R$ 17.88 | 15.0% |

"Muda rota vs. equilibrado" compara a rota escolhida por essa configuração com a rota do modelo atual (pesos 1,1,1) — quantifica o quanto mudar os pesos realmente altera a decisão.

## Como interpretar

Compare "Duração média" com "Combustível médio"/"Desgaste médio" entre as linhas: se priorizar combustível reduz o combustível médio mas aumenta a duração média, isso confirma um trade-off real (não é ruído) — a escolha de pesos deve refletir o objetivo real do negócio (cumprir prazo, economizar combustível ou preservar a vida útil da frota).
