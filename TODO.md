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
   - [x] Unidades de Medida — feito (não estava na fila original; virou pré-requisito de Produtos ao especificar o campo de unidade)
   - [x] Produtos — feito (era "Itens")
   - [x] Clientes/Fornecedores — feito (uma tela só, campo de tipo Cliente/Fornecedor/Ambos)
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
- Cliente e Fornecedor ficam numa tela só (`/dashboard/clientes`), distinguidos por um campo `type` (`cliente`/`fornecedor`/`ambos`) com filtro na listagem — não viram cadastros separados.
- Unidade de Medida virou cadastro próprio (`/dashboard/unidades`), não um enum fixo em código — Produtos referencia a unidade por id e a exclusão de uma unidade em uso por algum produto é bloqueada.
- CEP e CNPJ no cadastro de Clientes são consultados automaticamente (ViaCEP e BrasilAPI, gratuitas e sem chave) e preenchem o formulário; os campos continuam editáveis depois.
- Primitivas de UI compartilhadas moraram em `components/ui/`: `Modal`, `ConfirmDialog`, `PageHeader`, `CreateButton`, `SearchInput`, `EmptyState`, `ViewToggle`, `Textarea` — ver `CLAUDE.md` para o inventário completo. Cada cadastro novo deve reusar essas primitivas em vez de duplicar overlay/header/busca.

## Em aberto

- Campos e regras de validação do cadastro de Motoristas ainda não definidos.
- Detalhe de como o Despacho (motorista + veículo + rota) funciona na prática.
