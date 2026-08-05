# TODO — Próximos módulos do FrontEnd-Next

Rascunho de roadmap combinado em conversa, pra não perder a sequência decidida antes de começar a especificar cada módulo.

## Fluxo de negócio confirmado

```
Pedido (cliente + itens)
   → fica pendente, se acumula com outros pedidos
   → Faturamento agrupa um lote de pedidos faturados
   → o lote faturado vira os waypoints de uma Rota
   → Rota é despachada num Veículo (+ Motorista)
```

Ou seja: **Faturamento é quem monta a rota**, não o pedido individual. Cada módulo depende do anterior — não são fluxos paralelos.

## Ordem de construção (de baixo pra cima)

1. **Cadastros**
   - [x] Veículos — feito
   - [ ] Itens
   - [ ] Clientes/Fornecedores
   - [ ] Motoristas
2. **Pedidos**
   - [ ] Novo pedido (cliente + itens)
   - [ ] Dashboard de pedidos (mini-dashboard)
3. **Faturamento**
   - [ ] Selecionar/agrupar pedidos pendentes em um lote faturado
4. **Rotas**
   - [ ] Nova rota — nasce de um Faturamento (endereços dos pedidos do lote viram waypoints); migra o mapa/A*/SSE do `frontend/` (protótipo) pro `FrontEnd-Next`
   - [ ] Minhas rotas — histórico
   - [ ] Despacho — associar veículo + motorista à rota

## Decisões já tomadas

- Motoristas entra em Cadastros.
- Sequência de construção: Cadastros → Pedidos → Faturamento → Rotas (de baixo pra cima, cada etapa é pré-requisito da próxima).
- Cada módulo passa pelo processo de brainstorming (mockups + Q&A) antes de virar spec e implementação — igual foi feito com Veículos e o Toast.

## Em aberto

- Campos e regras de validação de cada cadastro/módulo ainda não definidos.
- Detalhe de como o Despacho (motorista + veículo + rota) funciona na prática.
