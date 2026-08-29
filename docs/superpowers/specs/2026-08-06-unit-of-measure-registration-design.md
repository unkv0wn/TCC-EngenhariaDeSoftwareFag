# Cadastro de Unidade de Medida — Design Spec

Data: 2026-08-06
Escopo: `FrontEnd-Next/` (Next.js)

## Contexto

Surgiu durante a implementação do cadastro de Produtos
(`docs/superpowers/specs/2026-08-06-products-and-customers-registration-design.md`): o campo
`unit` do Produto era um enum fixo em código (`un`/`kg`/`cx`/`l`). Cadastrar uma unidade nova
exigia editar três arquivos. Este spec substitui o enum por um cadastro próprio — Produtos passa
a listar dinamicamente as unidades registradas.

Não estava no `TODO.md` original; é um pré-requisito de Produtos identificado depois do fato,
tratado como sua própria fase (Fase 3), entre a entrega de Produtos (Fase 2, commit `1ab3c26`) e
Clientes (que passa a ser Fase 4).

## Decisões de escopo

- **Campos:** `code` (obrigatório, até 10 caracteres, uppercase automático) e `name`
  (obrigatório). Label de exibição: `${name} (${code})` — mesmo formato que o enum antigo já
  produzia, então nada muda visualmente nos lugares que já mostravam unidade.
- **Unicidade** de `code` e de `name`, verificada no `handleSubmit` da página, mesmo padrão de
  SKU (Produto) e placa (Veículo).
- **Exclusão bloqueada quando em uso.** Se algum produto referencia a unidade, o clique no ícone
  de excluir dispara um toast de erro e **não abre** o `ConfirmDialog` — evita um clique extra
  para descobrir que não pode excluir.
- **Tela em lista simples.** Sem alternância cards/tabela: a entidade tem dois campos, uma grade
  de cards não agregaria nada.
- **Continuidade de dados:** as quatro unidades seed nascem com `id` fixo —
  `"un"`, `"kg"`, `"cx"`, `"l"` — os mesmos valores que os produtos mock em `useProducts.ts` já
  usam no campo `unit`. Não há migração de dados de Produtos a fazer.
- **Sem guarda de rota,** como as demais telas do dashboard.

## Modelo de dados

### `lib/validations/unit.ts`

```
unitSchema = z.object({
  code: z.string().min(1, "Informe o código.").max(10, "Máximo de 10 caracteres.")
          .transform(v => v.toUpperCase()),
  name: z.string().min(1, "Informe o nome."),
})
type UnitFormData = z.infer<typeof unitSchema>
```

### `hooks/useUnits.ts`

`interface Unit extends UnitFormData { id: string }`, seed:

| id | code | name |
|---|---|---|
| `un` | UN | Unidade |
| `kg` | KG | Quilo |
| `cx` | CX | Caixa |
| `l` | L | Litro |

`createUnit` / `updateUnit` / `deleteUnit`, mesmo formato de `useProducts`. O hook não faz a
checagem de "em uso" — isso depende da lista de produtos, que o hook de unidades não conhece;
fica na `UnitsPageContent`.

## Tela `/dashboard/unidades`

`PageHeader` (título "Unidades de Medida", subtítulo "Gerencie as unidades de medida disponíveis
para produtos") + `CreateButton` ("Nova unidade") — sem `ViewToggle`. `SearchInput` filtrando por
`code` e `name`. `UnitTable` (única listagem: colunas Código · Nome · ações) ou `EmptyState`.

`UnitFormModal`, `size="md"`, dois campos (`Input` Código, `Input` Nome).

Exclusão: `UnitsPageContent` lê tanto `useUnits()` quanto `useProducts()`. Ao clicar em excluir,
conta quantos produtos têm `product.unit === unit.id`; se `> 0`, dispara
`error("Unidade em uso", \`N produto(s) usam esta unidade e ela não pode ser excluída.\`)` e para
aí — sem abrir modal. Caso contrário, abre o `ConfirmDialog` de exclusão normal.

Sem `UnitCard`/`UnitGrid` — não existem nesta tela.

## Impacto em Produtos (retrofit da Fase 2)

- `lib/validations/product.ts`: remove `UNIT_VALUES`, `ProductUnit`, `PRODUCT_UNITS`. Campo
  `unit` passa de `z.enum(...)` para `z.string().min(1, "Selecione a unidade.")` — a existência do
  valor é garantida pelas `<option>` do próprio `<select>`, não precisa ser revalidada no zod.
- `ProductCard`: o badge de unidade perde a cor por tipo (`UNIT_BADGE_STYLES` é removido) e passa
  a usar uma única cor — amarelo (`bg-warning-50 text-warning-700`) — para qualquer unidade. Como
  a exclusão de unidade em uso é bloqueada, todo `product.unit` sempre resolve para uma unidade
  existente; não há necessidade de tratar o caso de referência órfã.
- `ProductCard`, `ProductGrid`, `ProductTable`, `ProductFormModal`, `ProductsPageContent`: passam
  a receber (ou repassar) `units: Unit[]`, vindo de `useUnits()` em `ProductsPageContent`, no
  lugar de importar `PRODUCT_UNITS` estático.

## Navegação

Grupo "Cadastros" da `Sidebar`: novo item "Unidades de Medida" (ícone `Ruler`) entre Veículos e
Produtos — reflete que Produtos depende dele.

## Verificação

`pnpm lint`, `pnpm build`, teste manual em navegador: CRUD de unidades, código/nome duplicado
bloqueado, exclusão de unidade em uso bloqueada com toast (sem abrir modal), exclusão de unidade
livre funcionando, e a tela de Produtos continuando a criar/editar produtos normalmente com a
nova lista dinâmica de unidades.
