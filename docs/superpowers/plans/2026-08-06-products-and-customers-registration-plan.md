# Cadastro de Produtos e Clientes — Plano de Implementação

Data: 2026-08-06
Spec: `docs/superpowers/specs/2026-08-06-products-and-customers-registration-design.md`
Escopo: `FrontEnd-Next/`

Três fases, cada uma com verificação própria e commit próprio. Todos os caminhos são relativos a
`FrontEnd-Next/`.

---

## Fase 1 — Primitivas de UI e migração de Veículos

Refactor sem mudança visual, mais duas correções de comportamento (acessibilidade dos modais e
unicidade de placa). Nada de Produtos ou Clientes entra aqui.

### 1.1 `components/ui/Modal.tsx`

```
{ title: string; onClose: () => void; size?: "md" | "lg"; children: ReactNode }
```

- Overlay: `fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-gray-900/45 px-4 py-9`.
- Painel: `w-full rounded-2xl border border-gray-100 bg-white shadow-xl shadow-gray-900/10`,
  com `max-w-md` para `size="md"` (padrão) e `max-w-2xl` para `size="lg"`.
- Header: `flex items-center justify-between border-b border-gray-100 px-6 py-4`, com
  `<h2 className="text-base font-extrabold text-gray-900">` e botão X
  (`rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600`, `aria-label="Fechar"`).
- `role="dialog"`, `aria-modal="true"`, `aria-labelledby` apontando para o id do `h2` (via `useId`).
- `useEffect` registrando `keydown` em `document`: `Escape` chama `onClose`. Remover no cleanup.
- `ref` no painel + `tabIndex={-1}` e `.focus()` no mount, para o foco não ficar no `body`.
- Clique no overlay **não** fecha — comportamento atual dos dois modais de Veículos; manter.
- `"use client"`.

Extraído de `VehicleFormModal.tsx` linhas 45–59. Um detalhe: `DeleteVehicleDialog` usa hoje
`max-w-sm` e um overlay sem `overflow-y-auto`/`py-9`. Ele passa a usar `size="md"` (`max-w-md`) —
diferença visual mínima e aceita, em troca de uma primitiva só.

### 1.2 `components/ui/ConfirmDialog.tsx`

```
{ title: string; description: ReactNode; confirmLabel: string; onCancel: () => void; onConfirm: () => void }
```

Constrói sobre `Modal` (`onClose={onCancel}`). Corpo: `px-6 py-5` com a descrição em
`text-sm font-medium text-gray-500`, e o rodapé `mt-5 flex justify-end gap-2.5` com os dois
`Button` — o de confirmar mantendo o override ad hoc de danger já usado hoje
(`w-auto bg-danger-600 hover:bg-danger-700 active:bg-danger-800`).

`description` é `ReactNode`, não `string`, porque Veículos destaca a placa em negrito dentro da
frase.

### 1.3 `components/ui/PageHeader.tsx`

```
{ title: string; subtitle: string; children?: ReactNode }
```

`mb-5 flex items-start justify-between`; título `text-[19px] font-extrabold text-gray-900`,
subtítulo `mt-1 text-[13px] font-medium text-gray-500`; `children` renderizado em
`flex items-center gap-2.5`. Extraído de `VehiclesPageContent.tsx` linhas 66–82.

### 1.4 `components/ui/CreateButton.tsx`

```
{ label: string; onClick: () => void }
```

O botão "Novo veículo" atual (`VehiclesPageContent.tsx` linhas 73–81) com o ícone `Plus`,
verbatim, incluindo o estilo ad hoc `text-[13.5px] font-bold` que o `CLAUDE.md` registra como
divergente do componente `Button`.

**Adição ao que a spec previa** (ela listava sete primitivas). Sem isso, o mesmo botão fora do
padrão seria copiado três vezes. Extrair preserva os pixels e concentra a divergência num arquivo
só — alinhá-lo ao `Button` vira uma decisão de design isolada e barata depois.

### 1.5 `components/ui/SearchInput.tsx`

```
{ value: string; onChange: (value: string) => void; placeholder: string; label: string }
```

O bloco `relative` com ícone `Search` e `<input type="search">` de `VehiclesPageContent.tsx`
linhas 84–97. `label` vira `aria-label`. O wrapper mantém `max-w-xs`. Recebe a string já
desembrulhada do evento (`onChange(event.target.value)` internamente) — as `PageContent` só
lidam com o valor.

Não reusa o `Input` primitivo: `Input` sempre renderiza `<label>` visível, e a busca não tem.

### 1.6 `components/ui/EmptyState.tsx`

```
{ message: ReactNode }
```

`rounded-xl border border-dashed border-gray-300 px-6 py-8 text-center text-[12.5px] font-semibold text-gray-400`.

### 1.7 `components/ui/ViewToggle.tsx`

Mover `components/vehicles/ViewToggle.tsx` para cá, renomeando o tipo exportado
`VehicleView` → `ListView`. Conteúdo inalterado. Deletar o arquivo antigo.

### 1.8 `components/ui/Textarea.tsx`

Espelha `Input.tsx`: `forwardRef<HTMLTextAreaElement>`, `useId`, `label`, `error`,
`aria-invalid`/`aria-describedby`, mesmas classes de campo mais `min-h-[80px] resize-y`, mesmo
bloco de erro com `AlertCircle`. Sem `startIcon`/`endAdornment`.

### 1.9 Migrar Veículos

- `VehicleFormModal.tsx` — trocar overlay/painel/header por `<Modal title={...} onClose={onClose}>`.
  O formulário e os campos ficam iguais.
- `DeleteVehicleDialog.tsx` — reescrito como uso de `ConfirmDialog`. Manter o arquivo (ele
  monta a descrição com a placa em negrito e o texto "Essa ação não pode ser desfeita").
- `VehiclesPageContent.tsx` — usar `PageHeader`, `CreateButton`, `SearchInput`, `EmptyState`,
  e importar `ViewToggle`/`ListView` de `@/components/ui/ViewToggle`.
- `EmptyState`: passar a distinguir os dois casos —
  `search ? \`Nenhum veículo encontrado para "${search}".\` : "Nenhum veículo cadastrado."`
- **Placa única:** em `handleSubmit`, antes de gravar, checar
  `vehicles.some((v) => v.plate === data.plate && v.id !== formVehicle?.id)`. Se colidir, disparar
  `error("Placa já cadastrada", \`${data.plate} já pertence a outro veículo.\`)` e retornar sem
  fechar o modal. `useToast` já expõe `error`.

`VehicleCard`, `VehicleGrid`, `VehicleTable` e `lib/validations/vehicle.ts` não são tocados.

### Verificação da Fase 1

`pnpm lint` e `pnpm build`. Manualmente em `/dashboard/veiculos`: criar, editar e excluir seguem
funcionando; `Esc` fecha os dois modais; cadastrar placa repetida mostra toast de erro e mantém o
modal aberto; limpar a busca com lista cheia não mostra empty state.

---

## Fase 2 — Produtos

### 2.1 `lib/format.ts`

- `formatCurrency(value: number): string` — `Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" })`.
- `formatWeight(kg: number): string` — número com até 3 casas + `" kg"`.
- `formatDocument(value: string): string` — aplica máscara de CPF ou CNPJ conforme a quantidade
  de dígitos; idempotente sobre valor já mascarado.
- `formatPhone(value: string): string` — `(00) 0000-0000` / `(00) 00000-0000`.

`formatDocument` e `formatPhone` só serão consumidos na Fase 3, mas nascem aqui para o arquivo
não ser criado duas vezes.

### 2.2 `lib/validations/product.ts`

Espelha `vehicle.ts`, inclusive o comentário sobre `valueAsNumber`/`NaN`:

```
UNIT_VALUES = ["un", "kg", "cx", "l"] as const
type ProductUnit
PRODUCT_UNITS: { value, label }[]   // "Unidade (UN)", "Quilo (KG)", "Caixa (CX)", "Litro (L)"

productSchema = z.object({
  sku:         string min 1 "Informe o código." + transform(toUpperCase)
  name:        string min 1 "Informe o nome."
  description: string max 200 "Máximo de 200 caracteres." optional
  unit:        enum(UNIT_VALUES) "Selecione a unidade."
  unitPrice:   number, !NaN "Informe o preço." , > 0 "Informe um preço válido."
  weightKg:    number, !NaN "Informe o peso." , > 0 "Informe um peso válido."
})

type ProductFormData = z.infer<typeof productSchema>
```

### 2.3 `hooks/useProducts.ts`

Cópia estrutural de `useVehicles`: `interface Product extends ProductFormData { id: string }`,
`INITIAL_PRODUCTS` com 5–6 itens de exemplo coerentes com uma distribuidora, e
`createProduct` / `updateProduct` / `deleteProduct` com `crypto.randomUUID()`.

### 2.4 `components/products/`

- **`ProductCard.tsx`** — molde do `VehicleCard`: mesmas classes de card, mesmo bloco de ações
  que aparece no hover, mesmo botão interno que abre a edição. SKU no badge
  `rounded-md bg-primary-50 … text-primary-700` (onde a placa está hoje), unidade no badge
  `rounded-full` à direita, nome em `text-sm font-extrabold`, descrição truncada em
  `text-[11.5px] font-semibold text-gray-500 line-clamp-1`, e o rodapé com borda superior
  mostrando preço (`formatCurrency`) e peso (`formatWeight`).
- **`ProductGrid.tsx`** — idêntico ao `VehicleGrid`, trocando o tipo.
- **`ProductTable.tsx`** — molde do `VehicleTable`. Cabeçalhos:
  `["Código", "Nome", "Unidade", "Preço unit.", "Peso", ""]`.
- **`ProductFormModal.tsx`** — `<Modal title={isEditing ? "Editar produto" : "Novo produto"} size="md">`,
  `useForm` + `zodResolver(productSchema)`, `reset` no `useEffect` igual ao de Veículos.
  Layout: grid 2 colunas para Código/Unidade, `Input` de largura total para Nome, `Textarea` para
  Descrição, grid 2 colunas para Preço/Peso (ambos `type="number"` com `valueAsNumber`;
  preço com `step="0.01"`). Rodapé com Cancelar + "Salvar produto".
- **`ProductsPageContent.tsx`** — molde do `VehiclesPageContent` sobre as primitivas da Fase 1.
  Título "Produtos", subtítulo "Gerencie os produtos do seu catálogo", busca por `sku` e `name`,
  unicidade de `sku`, exclusão via `ConfirmDialog` inline (sem arquivo dedicado), toasts de
  criação/edição/exclusão nomeando o produto.

### 2.5 Rota e navegação

- `app/dashboard/produtos/page.tsx` — molde de `veiculos/page.tsx`, com `metadata`
  (`title: "Produtos"`).
- `components/dashboard/Sidebar.tsx` — no grupo "Cadastros", acrescentar
  `{ href: "/dashboard/produtos", label: "Produtos", icon: Package }` depois de Veículos, e
  importar `Package` de `lucide-react`.

### Verificação da Fase 2

`pnpm lint` e `pnpm build`. Manualmente: CRUD completo em `/dashboard/produtos`, alternância
cards/tabela, busca sem resultado, SKU duplicado bloqueado, preço formatado em BRL, item da
Sidebar ativo na rota certa.

---

## Fase 3 — Clientes

### 3.1 `lib/validations/document.ts`

`isValidCpf(value: string): boolean` e `isValidCnpj(value: string): boolean`. Ambas removem tudo
que não é dígito antes de calcular, rejeitam comprimento errado e sequências de dígito repetido
(`00000000000`, `11111111111`, …), e conferem os dois dígitos verificadores pelo algoritmo módulo
11. Funções puras, sem import de React.

### 3.2 `lib/masks.ts`

`maskCpf`, `maskCnpj`, `maskPhone`, `maskZipCode` — cada uma remove não-dígitos, corta no
comprimento máximo e insere os separadores progressivamente (funcionam com valor parcial, para
poder rodar a cada tecla). Mais `maskDocument(value, personType)`, que delega para `maskCpf` ou
`maskCnpj`.

Convenção: **o formulário guarda o valor mascarado**; os validadores tiram a máscara internamente.
Sem backend, isso evita um par converte/desconverte que não serviria a ninguém hoje. Quando os
endpoints existirem, a normalização entra no service.

### 3.3 `lib/validations/customer.ts`

```
PERSON_TYPES  = [{ fisica, "Pessoa física" }, { juridica, "Pessoa jurídica" }]
CUSTOMER_TYPES = [{ cliente, "Cliente" }, { fornecedor, "Fornecedor" }, { ambos, "Ambos" }]
BRAZIL_STATES  = 27 UFs (AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO)

addressSchema = z.object({
  zipCode:    string, 8 dígitos após remover máscara, "Informe um CEP válido."
  street:     string min 1 "Informe o logradouro."
  number:     string min 1 "Informe o número."     // texto: aceita "S/N"
  complement: string optional
  district:   string min 1 "Informe o bairro."
  city:       string min 1 "Informe a cidade."
  state:      enum(UFs) "Selecione a UF."
})

customerSchema = z.object({
  personType: enum(["fisica","juridica"]) "Selecione o tipo de pessoa."
  document:   string min 1 "Informe o documento."
  name:       string min 1 "Informe o nome."
  tradeName:  string optional
  type:       enum(["cliente","fornecedor","ambos"]) "Selecione o tipo."
  email:      string email "Informe um e-mail válido."
  phone:      string, 10 ou 11 dígitos, "Informe um telefone válido."
  address:    addressSchema
}).superRefine((data, ctx) => {
  // valida document contra isValidCpf ou isValidCnpj conforme personType,
  // adicionando o issue em path: ["document"]
})
```

O `superRefine` é o que amarra `document` a `personType` — validação cruzada não cabe num campo
isolado.

### 3.4 `hooks/useCustomers.ts`

Mesma forma de `useProducts`. `INITIAL_CUSTOMERS` com 5–6 registros cobrindo os casos que a tela
precisa exercitar: pessoa física, pessoa jurídica com nome fantasia, e ao menos um `fornecedor` e
um `ambos` — senão o filtro de tipo não tem o que mostrar.

### 3.5 `components/customers/`

- **`CustomerTypeFilter.tsx`** — `{ value: "todos" | CustomerType; onChange }`. Grupo segmentado
  com o mesmo invólucro do `ViewToggle` (`inline-flex … rounded-lg border border-gray-200 bg-white p-0.5`),
  três botões com rótulo de texto (`px-2.5 h-7 text-[12px] font-bold`), ativo em
  `bg-primary-50 text-primary-600`, `aria-pressed` em cada um.
- **`CustomerCard.tsx`** — nome em destaque, badge `rounded-full` do tipo
  (cliente → `bg-primary-50 text-primary-700`, fornecedor → `bg-secondary-50 text-secondary-700`,
  ambos → `bg-success-50 text-success-700`), documento via `formatDocument`, `cidade/UF`, e-mail
  e telefone via `formatPhone`. Mesma mecânica de hover/ações do `VehicleCard`.
- **`CustomerGrid.tsx`** — grade um pouco mais larga que a de Veículos
  (`grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`), porque o card tem mais linhas de texto.
- **`CustomerTable.tsx`** — cabeçalhos `["Nome", "Tipo", "Documento", "Cidade/UF", "Telefone", ""]`.
- **`CustomerFormModal.tsx`** — `<Modal size="lg">`. Duas seções separadas por `Divider`, cada uma
  com um título `text-[11px] font-extrabold uppercase tracking-wider text-gray-400`:
  - *Dados cadastrais*: Tipo de pessoa / Tipo (2 col.), Documento / Nome-Razão social (2 col.),
    Nome fantasia (só quando `personType === "juridica"`), E-mail / Telefone (2 col.).
  - *Endereço*: CEP / Logradouro (2 col.), Número / Complemento (2 col.),
    Bairro / Cidade / UF (3 col.).
  - `const personType = watch("personType")` controla o rótulo do documento
    (`CPF` / `CNPJ`), o rótulo do nome (`Nome` / `Razão social`) e a exibição do nome fantasia.
  - `useEffect` sobre `personType`: `resetField("document")` e `resetField("tradeName")`, para não
    deixar valor inválido preso ao trocar de tipo.
  - Máscaras nos campos `document`, `phone` e `address.zipCode`, via
    `register(campo, { onChange: (e) => setValue(campo, mask(e.target.value)) })`.
- **`CustomersPageContent.tsx`** — título "Clientes", subtítulo
  "Gerencie seus clientes e fornecedores". No slot de ações: `CustomerTypeFilter`, `ViewToggle`,
  `CreateButton`. Filtro combinado — primeiro por tipo
  (`todos`, ou `type === filtro || type === "ambos"`), depois pela busca em `name`, `tradeName`,
  `document` e `address.city`. Unicidade de `document`. Exclusão via `ConfirmDialog`.

### 3.6 Rota e navegação

- `app/dashboard/clientes/page.tsx`, molde das anteriores.
- `Sidebar.tsx`: `{ href: "/dashboard/clientes", label: "Clientes", icon: Users }` depois de
  Produtos.

### Verificação da Fase 3

`pnpm lint` e `pnpm build`. Manualmente: CRUD completo; CPF e CNPJ inválidos recusados e válidos
aceitos; trocar de pessoa física para jurídica limpa documento e nome fantasia; máscaras se
formando ao digitar; filtro de tipo com "Ambos" aparecendo nas três posições; busca por cidade;
documento duplicado bloqueado.

---

## Fase 4 — Documentação

Commit separado, depois das três fases.

- **`TODO.md`** — marcar `[x] Itens` e `[x] Clientes/Fornecedores`; em "Decisões já tomadas",
  registrar que Cliente e Fornecedor ficam numa tela só com campo de tipo; remover de "Em aberto"
  a linha sobre campos e validação desses dois cadastros.
- **`CLAUDE.md`**, seção "Component style patterns (`FrontEnd-Next`)":
  - "No shared `<Modal>` primitive yet (`VehicleFormModal` / `DeleteVehicleDialog` duplicate the
    overlay/panel structure)" deixa de ser verdade — reescrever descrevendo `Modal` e
    `ConfirmDialog`.
  - A observação sobre botões de header que contornam o `Button` continua verdadeira, mas passa a
    estar concentrada em `CreateButton` — ajustar a redação.
  - Acrescentar `Textarea`, `PageHeader`, `SearchInput`, `EmptyState`, `ViewToggle` e
    `CreateButton` à lista de primitivas de `components/ui/`.
  - Ajustar a regra de nomenclatura `I`/`T` para registrar que não se aplica ao `FrontEnd-Next`
    (decisão da spec).

---

## Ordem de commits

1. `refactor(frontend-next): extract shared UI primitives and migrate vehicles`
2. `feat(frontend-next): add product registration screen`
3. `feat(frontend-next): add customer registration screen`
4. `docs: update TODO and component patterns after products/customers`

## Riscos

- **Regressão em Veículos na Fase 1.** É a única fase que mexe em código funcionando. Mitigação:
  não mudar `VehicleCard`/`VehicleGrid`/`VehicleTable`/schema, e conferir a tela manualmente antes
  de seguir para a Fase 2.
- **`max-w-sm` → `max-w-md` no diálogo de exclusão.** Mudança visual pequena e consciente
  (item 1.1). Se incomodar, `ConfirmDialog` pode fixar `size="sm"` com um terceiro valor no
  `Modal`.
- **Volume da Fase 3.** Clientes tem 15 campos e é bem maior que as outras. Se ficar pesado,
  o corte natural é entregar `document.ts` + `masks.ts` + schema num commit e a UI noutro.
