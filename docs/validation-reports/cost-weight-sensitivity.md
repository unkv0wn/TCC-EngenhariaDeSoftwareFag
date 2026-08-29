# Análise de Sensibilidade dos Pesos da Função de Custo

Mesmos 500 cenários sintéticos e mesmo veículo (Médio, 3 eixos) em todas as linhas — só os pesos `(tempo, combustível, desgaste de pneu)` mudam na combinação `custoB = α·tempo + β·combustível + γ·desgaste`. Mostra o trade-off real: priorizar um fator tende a piorar os outros.

| Configuração | Pesos (α,β,γ) | Distância média | Duração média | Combustível médio | Desgaste médio (R$) | Muda rota vs. equilibrado |
|---|---|---|---|---|---|---|
| Só tempo (produção atual) | (1.0, 0.0, 0.0) | 181.29 km | 175.8 min | 30.44 L | R$ 68.50 | 31.2% |
| Equilibrado (1,1,1) — modelo atual | (1.0, 1.0, 1.0) | 171.91 km | 179.5 min | 29.55 L | R$ 67.07 | 0.0% |
| Prioriza combustível | (0.3, 2.0, 0.3) | 168.00 km | 184.5 min | 29.34 L | R$ 66.99 | 18.6% |
| Prioriza desgaste de pneu | (0.3, 0.3, 2.0) | 170.63 km | 180.9 min | 29.46 L | R$ 66.99 | 6.0% |
| Só combustível | (0.0, 1.0, 0.0) | 166.83 km | 186.9 min | 29.32 L | R$ 67.09 | 24.6% |
| Só desgaste de pneu | (0.0, 0.0, 1.0) | 169.04 km | 182.9 min | 29.38 L | R$ 66.96 | 13.2% |

"Muda rota vs. equilibrado" compara a rota escolhida por essa configuração com a rota do modelo atual (pesos 1,1,1) — quantifica o quanto mudar os pesos realmente altera a decisão.

## Como interpretar

Compare "Duração média" com "Combustível médio"/"Desgaste médio" entre as linhas: se priorizar combustível reduz o combustível médio mas aumenta a duração média, isso confirma um trade-off real (não é ruído) — a escolha de pesos deve refletir o objetivo real do negócio (cumprir prazo, economizar combustível ou preservar a vida útil da frota).
