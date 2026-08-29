# Cadastro de Unidade de Medida — Plano de Implementação

Data: 2026-08-06
Spec: `docs/superpowers/specs/2026-08-06-unit-of-measure-registration-design.md`
Escopo: `FrontEnd-Next/`

Fase 3 da fila de cadastros (entre Produtos, já entregue, e Clientes, que passa a ser Fase 4).
Um commit ao final. Caminhos relativos a `FrontEnd-Next/`.

## 1. `lib/validations/unit.ts`

```ts
export const unitSchema = z.object({
  code: z.string().min(1, "Informe o código.").max(10, "Máximo de 10 caracteres.")
          .transform((value) => value.toUpperCase()),
  name: z.string().min(1, "Informe o nome."),
});
export type UnitFormData = z.infer<typeof unitSchema>;
```

## 2. `hooks/useUnits.ts`

Molde de `useProducts.ts`. `interface Unit extends UnitFormData { id: string }`,
`INITIAL_UNITS` com os 4 ids fixos da tabela da spec (`un`/`kg`/`cx`/`l`, preservando
continuidade com `useProducts.ts`). `createUnit` / `updateUnit` / `deleteUnit`.

## 3. `components/units/UnitTable.tsx`

Molde de `ProductTable.tsx` reduzido: cabeçalhos `["Código", "Nome", ""]`, código em
`font-bold`, nome em `font-semibold`, ações editar/excluir.

## 4. `components/units/UnitFormModal.tsx`

Molde de `ProductFormModal.tsx` reduzido: `Modal size="md"`, dois `Input` (Código, Nome), sem
grid — cada campo em largura total, já que só há dois. `useForm` + `zodResolver(unitSchema)`,
`reset` no mesmo padrão dos outros formulários.

## 5. `components/units/UnitsPageContent.tsx`

Molde de `ProductsPageContent.tsx`, sem `ViewToggle`/`view` state — sempre renderiza `UnitTable`
(ou `EmptyState`). Usa `useUnits()` e também `useProducts()` (só para a checagem de uso).

```ts
function handleDeleteClick(unit: Unit) {
  const usageCount = products.filter((product) => product.unit === unit.id).length;
  if (usageCount > 0) {
    error("Unidade em uso", `${usageCount} produto(s) usam esta unidade e ela não pode ser excluída.`);
    return;
  }
  setUnitToDelete(unit);
}
```

`unitToDelete` só é setado quando a exclusão é permitida — o `ConfirmDialog` não precisa saber
nada sobre a regra de uso.

Unicidade de `code` e `name` no `handleSubmit`, mesmo padrão de SKU/placa (ignorando o próprio
registro na edição).

## 6. `app/dashboard/unidades/page.tsx`

Molde de `produtos/page.tsx`, `metadata.title = "Unidades de Medida"`.

## 7. `components/dashboard/Sidebar.tsx`

Import `Ruler` de `lucide-react`. No grupo "Cadastros", inserir entre Veículos e Produtos:
`{ href: "/dashboard/unidades", label: "Unidades de Medida", icon: Ruler }`.

## 8. Retrofit de Produtos

- **`lib/validations/product.ts`** — remover `UNIT_VALUES`, `ProductUnit`, `PRODUCT_UNITS`.
  Trocar `unit: z.enum(UNIT_VALUES, {...})` por `unit: z.string().min(1, "Selecione a unidade.")`.
- **`components/products/ProductCard.tsx`** — remover `UNIT_BADGE_STYLES` e o import de
  `ProductUnit`. Badge de unidade vira classe fixa `rounded-full bg-warning-50 px-2 py-0.5
  text-[10px] font-bold text-warning-700`. Recebe `units: Unit[]` como prop; label resolvido com
  `units.find((u) => u.id === product.unit)` — sem fallback (exclusão em uso é bloqueada, então
  sempre resolve).
- **`components/products/ProductGrid.tsx`** — recebe `units: Unit[]` e repassa para `ProductCard`.
- **`components/products/ProductTable.tsx`** — recebe `units: Unit[]`; troca
  `PRODUCT_UNITS.find(...)` por `units.find((u) => u.id === product.unit)`, exibindo
  `${u.name} (${u.code})`.
- **`components/products/ProductFormModal.tsx`** — recebe `units: Unit[]`; `Select` passa a usar
  `options={units.map((u) => ({ value: u.id, label: \`${u.name} (${u.code})\` }))}` no lugar de
  `PRODUCT_UNITS`.
- **`components/products/ProductsPageContent.tsx`** — chama `useUnits()`, repassa `units` para
  `ProductGrid`, `ProductTable` e `ProductFormModal`.

Nenhum dado mock muda: os `id` fixos de `INITIAL_UNITS` já batem com os valores de `unit` em
`INITIAL_PRODUCTS`.

## Verificação

`pnpm lint`, `pnpm build`, teste manual em navegador:

- CRUD completo em `/dashboard/unidades`; código/nome duplicado bloqueado com toast, modal
  permanece aberto.
- Excluir uma unidade em uso (ex.: "Caixa") dispara toast de erro e **não** abre o
  `ConfirmDialog`.
- Criar uma unidade nova, confirmar que ela aparece no `Select` do formulário de Produto, e que
  dá pra excluir essa unidade nova normalmente enquanto não estiver em uso por nenhum produto.
- `/dashboard/produtos` continua funcionando: criar/editar produto com o `Select` de unidade
  agora dinâmico, badge da unidade sempre amarelo no card.

## Commit

`feat(frontend-next): add unit of measure registration and make product units dynamic`
