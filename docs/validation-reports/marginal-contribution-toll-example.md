# Validação de Contribuição Marginal — Nova Variável (pedágio)

Candidato real (não é placeholder sintético como o exemplo de risco): **custo de
pedágio**. Só se aplica em trechos RODOVIA, proporcional à distância e ao número
de eixos (R$ 0.05/km/eixo — aproximação da lógica de cobrança por eixo dos
pedágios brasileiros, ANTT). Perfil de veículo: Médio (3 eixos) (3 eixos).

Motivação: o otimizador atual (só tempo) tende a preferir rodovia por ser mais
rápida; se o pedágio for caro o suficiente, pode compensar financeiramente uma
rota um pouco mais lenta por vias sem pedágio.

| Métrica | Valor |
|---|---|
| Cenários testados | 500 |
| Rotas que mudaram ao adicionar o pedágio | 14.0% |
| Gap médio de custo | 0.15% |
| Wilcoxon p-valor | 0.0000 |
| Correlação (Spearman) com o custo já existente | 0.560 |

## Veredito

AGREGA VALOR — muda rotas de forma estatisticamente significativa e não é redundante com o modelo atual. Vale considerar incluir essa variável na função de custo.
