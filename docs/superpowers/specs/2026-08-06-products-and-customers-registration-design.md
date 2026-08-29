# Cadastro de Produtos e Clientes — Design Spec

Data: 2026-08-06
Escopo: `FrontEnd-Next/` (Next.js)

## Contexto

Segundo e terceiro cadastros do sistema, seguindo Veículos
(`docs/superpowers/specs/2026-07-30-vehicle-registration-design.md`). São os itens 2 e 3
da fila de Cadastros do `TODO.md` — "Itens" e "Clientes/Fornecedores" — pré-requisitos do
módulo de Pedidos, que por sua vez alimenta Faturamento e Rotas.

Continua 100% front-end: sem integração com o backend Spring, dados mockados em memória.
O backend hoje tem apenas a infraestrutura de conexão (Flyway + JPA), sem nenhuma entidade
ou repositório.

A entrega inclui uma extração de primitivas de UI. Veículos concentra hoje sete componentes,
e vários não têm nada de específico de veículo: `ViewToggle` (tipado como `VehicleView`), a
estrutura de overlay/painel duplicada entre `VehicleFormModal` e `DeleteVehicleDialog`, o
header da página, a busca e o empty state. Clonar isso duas vezes produziria três cópias —
cinco quando Motoristas e Pedidos chegarem. O `CLAUDE.md` já registra "no shared `<Modal>`
primitive yet" como lacuna conhecida.

## Decisões de escopo

- **Persistência:** nenhuma. Estado em memória via `useProducts` / `useCustomers`, inicializado
  com listas mock, igual a `useVehicles`. Recarregar a página volta ao estado inicial.
- **Sem services:** não há chamada de rede a simular. Mesma justificativa registrada em
  Veículos.
- **CRUD completo** em ambas as telas: criar, editar, excluir.
- **Clientes e Fornecedores numa tela só,** distinguidos por um campo `type`
  (`cliente` / `fornecedor` / `ambos`) com filtro na listagem. Só o cliente participa do fluxo
  Pedido → Faturamento → Rota; fornecedor fica cadastrado para uso futuro.
- **Endereço estruturado, sem coordenadas.** CEP, logradouro, número, complemento, bairro,
  cidade e UF. A conversão para lat/lng é responsabilidade do módulo de Rotas, que é quem
  precisa do waypoint — não polui o cadastro com campo técnico.
- **Sem guarda de rota,** como nas telas existentes.
- **Sem animação de entrada/saída nos modais.** A ausência é o comportamento atual documentado
  no `CLAUDE.md`; introduzi-la é decisão de design separada.
- **Convenção de nomes:** os arquivos novos seguem o padrão local do `FrontEnd-Next`
  (`interface Vehicle`, `type VehicleView` — sem prefixo), e não a regra `I`/`T` do `CLAUDE.md`,
  que na prática só vale em `frontend/`. Misturar os dois estilos dentro do mesmo módulo seria
  pior que a inconsistência já existente. A linha do `CLAUDE.md` deve ser ajustada para
  refletir isso.

## Parte 1 — Primitivas compartilhadas

Sete componentes em `components/ui/` — seis extraídos do que Veículos já faz, com comportamento
e aparência idênticos ao atual, mais um (`Textarea`) que ainda não existe no projeto.

| Primitiva | Absorve | Props |
|---|---|---|
| `Modal` | overlay `bg-gray-900/45` + painel `rounded-2xl … shadow-xl` + header com botão X | `{ title, onClose, size?: "md" \| "lg", children }` |
| `ConfirmDialog` | corpo do `DeleteVehicleDialog` (ícone, texto, botão danger ad hoc) | `{ title, description, confirmLabel, onCancel, onConfirm }` |
| `PageHeader` | bloco título + subtítulo + ações do `VehiclesPageContent` | `{ title, subtitle, children }` — `children` é o slot de ações |
| `SearchInput` | input com ícone `Search` | `{ value, onChange, placeholder, label }` |
| `EmptyState` | caixa tracejada de "nenhum resultado" | `{ message }` |
| `ViewToggle` | movido de `components/vehicles/`; tipo `VehicleView` renomeado para `ListView` | `{ view, onChange }` |
| `Textarea` | novo — não existe hoje; espelha `Input` (rótulo, erro, `forwardRef`, `useId`) para o campo Descrição do Produto | `{ label, error, ...textareaProps }` |

`Modal` acrescenta o que hoje falta nos dois modais de Veículos: fechamento com `Esc`, foco
inicial no painel e `role="dialog"` / `aria-modal="true"`. Como os dois passam a consumir a
mesma primitiva, Veículos herda a correção.

`ConfirmDialog` é construído sobre `Modal`.

### Migração de Veículos

`VehiclesPageContent`, `VehicleFormModal` e `DeleteVehicleDialog` passam a usar as primitivas.
`VehicleCard`, `VehicleGrid`, `VehicleTable` e `lib/validations/vehicle.ts` não mudam.
`components/vehicles/ViewToggle.tsx` é removido.

Uma mudança de comportamento entra junto: **placa passa a ser validada como única**, pela
mesma regra descrita em "Unicidade" abaixo.

## Parte 2 — Modelos de dados

### `lib/validations/product.ts`

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `sku` | texto | sim | convertido para maiúsculas automaticamente |
| `name` | texto | sim | |
| `description` | texto | não | máximo 200 caracteres |
| `unit` | select | sim | `un` / `kg` / `cx` / `l` |
| `unitPrice` | número | sim | maior que zero |
| `weightKg` | número | sim | maior que zero |

O peso é o campo que fecha o ciclo com o `capacityKg` do Veículo: permite, no Despacho, validar
se o lote faturado cabe no veículo escolhido.

Exporta `PRODUCT_UNITS` (pares `value`/`label`) no mesmo formato de `FUEL_TYPES`.

### `lib/validations/customer.ts`

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `personType` | select | sim | `fisica` / `juridica` — controla o rótulo e a validação do documento |
| `document` | texto | sim | CPF (11 dígitos) ou CNPJ (14) conforme `personType`, com dígito verificador |
| `name` | texto | sim | rótulo "Nome" (PF) ou "Razão social" (PJ) |
| `tradeName` | texto | não | nome fantasia; exibido apenas quando PJ |
| `type` | select | sim | `cliente` / `fornecedor` / `ambos` |
| `email` | texto | sim | formato de e-mail |
| `phone` | texto | sim | 10 ou 11 dígitos |
| `address.zipCode` | texto | sim | 8 dígitos |
| `address.street` | texto | sim | |
| `address.number` | texto | sim | texto, não número — aceita "S/N" |
| `address.complement` | texto | não | |
| `address.district` | texto | sim | |
| `address.city` | texto | sim | |
| `address.state` | select | sim | as 27 UFs |

`personType` é observado com `watch` do react-hook-form. Trocar de PF para PJ (ou vice-versa)
limpa `document` e `tradeName`, para não deixar valor inválido preso no formulário.

### Arquivos de apoio

- `lib/validations/document.ts` — `isValidCpf` e `isValidCnpj`, algoritmo de dígito verificador.
  Funções puras, sem dependência de React.
- `lib/masks.ts` — `maskCpf`, `maskCnpj`, `maskPhone`, `maskZipCode`, aplicadas no `onChange`
  do `register`.
- `lib/format.ts` — `formatCurrency` (BRL via `Intl.NumberFormat`), `formatWeight`,
  `formatDocument`, `formatPhone`. Usadas na leitura (cards e tabelas); as máscaras cuidam da
  escrita.

### Unicidade

`sku` (Produto), `document` (Cliente) e `plate` (Veículo) são validados como únicos no
`handleSubmit` da respectiva `PageContent`, ignorando o próprio registro durante a edição.
Em caso de colisão o modal permanece aberto e é disparado um toast `error`.

A checagem fica na `PageContent`, e não no schema zod, porque depende da lista atual — que o
schema não conhece.

## Parte 3 — Telas

### Produtos (`/dashboard/produtos`)

Mesma estrutura de Veículos: `PageHeader` + `SearchInput` + `ViewToggle` + grade ou tabela +
modal de formulário + `ConfirmDialog` de exclusão.

- Busca filtra por código e nome.
- **Card:** nome em destaque, SKU como badge pill, preço em BRL, peso e unidade; ações de
  editar e excluir no canto.
- **Tabela:** Código · Nome · Unidade · Preço unit. · Peso · ações.
- **Modal** `size="md"`, campos em grid de 2 colunas: Código / Unidade, Nome (largura total),
  Descrição (textarea, largura total), Preço / Peso.

Componentes em `components/products/`: `ProductsPageContent`, `ProductGrid`, `ProductCard`,
`ProductTable`, `ProductFormModal`. A exclusão usa o `ConfirmDialog` genérico — não há
`DeleteProductDialog`.

### Clientes (`/dashboard/clientes`)

Mesma estrutura, com duas diferenças:

- Ao lado da busca, um filtro de tipo (Todos / Clientes / Fornecedores) — componente
  `CustomerTypeFilter`, em `components/customers/`, reaproveitando o visual do `ViewToggle`
  (grupo segmentado com borda) mas com rótulos de texto no lugar de ícones. Registros marcados
  como `ambos` aparecem nos dois filtros.
- Busca filtra por nome, nome fantasia, documento e cidade.

Detalhes:

- **Card:** nome em destaque + badge do tipo, documento formatado, cidade/UF, e-mail e telefone.
- **Tabela:** Nome · Tipo · Documento · Cidade/UF · Telefone · ações.
- **Modal** `size="lg"`, dividido em duas seções tituladas — "Dados cadastrais" e "Endereço" —
  separadas por `Divider`.

Componentes em `components/customers/`: `CustomersPageContent`, `CustomerGrid`, `CustomerCard`,
`CustomerTable`, `CustomerFormModal`, `CustomerTypeFilter`.

Os badges de tipo (Produtos usa um para o SKU, Clientes para cliente/fornecedor) são compostos
inline com as classes `rounded-full` já usadas em Veículos — não vira primitiva agora, porque
os dois usos têm conteúdo e semântica diferentes.

### Navegação

O grupo "Cadastros" da `Sidebar` passa a listar Veículos, Produtos (ícone `Package`) e Clientes
(ícone `Users`), nessa ordem — a mesma do `TODO.md`.

## Estados e erros

- Validação inline pelo padrão atual de `Input` / `Select` (borda `danger-300`, mensagem com
  ícone `AlertCircle`).
- Toast de sucesso ao criar, editar e excluir.
- Toast `error` em código, documento ou placa duplicados, com o modal permanecendo aberto.
- O `EmptyState` distingue duas situações: lista vazia ("Nenhum produto cadastrado") e busca sem
  resultado ("Nenhum produto encontrado para *X*"). Veículos hoje mostra a mensagem de busca nos
  dois casos; a migração corrige isso.

## Verificação

O `FrontEnd-Next` não tem infraestrutura de teste. A verificação é `pnpm lint`, `pnpm build` e
conferência manual das três telas (incluindo Veículos, por causa da migração).

`isValidCpf`, `isValidCnpj` e as máscaras são funções puras e seriam os primeiros candidatos a
teste automatizado caso Vitest seja introduzido aqui. Fora do escopo desta entrega.

## Fases de entrega

Cada fase é verificável isoladamente.

1. Primitivas em `components/ui/` + migração de Veículos + regra de placa única.
2. Produtos: schema, `lib/format.ts`, hook, componentes, rota e item na Sidebar.
3. Clientes: schema, `document.ts`, `masks.ts`, hook, componentes, rota e item na Sidebar.

## Documentação a atualizar

- `TODO.md` — marcar "Itens" e "Clientes/Fornecedores" como feitos; registrar a decisão de
  manter Cliente e Fornecedor numa tela só.
- `CLAUDE.md` — a seção de padrões de componente afirma "No shared `<Modal>` primitive yet" e
  descreve a duplicação entre `VehicleFormModal` e `DeleteVehicleDialog`; ambas deixam de ser
  verdade.
