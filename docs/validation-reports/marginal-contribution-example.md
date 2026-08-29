# Validação de Contribuição Marginal — Nova Variável (exemplo: custo de risco)

Testa se adicionar uma nova variável ao modelo de custo atual (Cenário B: tempo +
combustível + desgaste de pneu) muda a rota escolhida de forma real, ou se é
negligenciável/redundante com o que já existe.

Exemplo usado aqui: um "custo de risco" fixo por trecho (não escala com distância,
ao contrário de combustível/pneu), maior em vias urbanas — simula algo como risco
de furto/sinistro. Para validar uma variável nova de verdade, troque a fórmula em
`NewIndicatorAdditionExample.riskCostReais(...)` por ela e rode de novo.

| Métrica | Valor |
|---|---|
| Cenários testados | 500 |
| Rotas que mudaram ao adicionar a variável | 18.4% |
| Gap médio de custo | 0.30% |
| Wilcoxon p-valor | 0.0000 |
| Correlação (Spearman) com o custo já existente | 0.647 |

Correlação alta (>= 0.9) sugere que a variável nova está medindo, na prática,
algo já capturado pelas variáveis atuais (redundância). Correlação baixa com
mudança de rota significativa sugere um sinal genuinamente novo.

## Veredito

AGREGA VALOR — muda rotas de forma estatisticamente significativa e não é redundante com o modelo atual. Vale considerar incluir essa variável na função de custo.
