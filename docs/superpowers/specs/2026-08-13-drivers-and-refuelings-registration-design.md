# Cadastro de Motoristas e Abastecimentos — Design Spec

Data: 2026-08-13
Escopo: `FrontEnd-Next/` (Next.js)

## Contexto

Motoristas é o próximo item da fila de Cadastros do `TODO.md` — já estava listado lá, com a
nota "campos e regras de validação ainda não definidos". Abastecimentos não estava no `TODO.md`
original; é uma adição pedida nesta conversa, mas conecta diretamente com um modelo que já
existe no backend: `com.routewise.validation.EmpiricalFuelConsumptionCalculator`, cujo
`RefuelingRecord{date, litersRefueled, kmSincePrevious}` é a referência usada aqui para desenhar
os campos do registro de abastecimento no front.

Continua 100% front-end, seguindo o padrão de Veículos/Unidades/Produtos/Clientes
(`docs/superpowers/specs/2026-08-06-products-and-customers-registration-design.md`): sem
integração com o backend Spring, dados mockados em memória, hook por página, `Modal`/
`ConfirmDialog`/`PageHeader`/`SearchInput`/`EmptyState`/`ViewToggle` de `components/ui/`
reaproveitados sem alteração.

Motoristas precisa ser construído **antes** de Abastecimentos — cada abastecimento referencia
um motorista pelo `id`, do mesmo jeito que Produto referencia Unidade.

## Decisões de escopo

- **Motoristas — campos:** nome completo, CPF, telefone, CNH (número, categoria, validade),
  status (ativo/inativo). Sem endereço.
- **Sem vínculo motorista↔veículo no cadastro.** O `TODO.md` deixa em aberto como o Despacho
  (motorista + veículo + rota) vai funcionar; antecipar esse vínculo aqui seria projetar para um
  design que ainda não existe. Motorista é standalone.
- **CNH vencida é só um campo informativo.** Sem badge de alerta, sem bloqueio — mantém o escopo
  igual aos outros cadastros, nenhum dos quais tem lógica de alerta hoje.
- **Motoristas usa Cards + Tabela** (`ViewToggle`), mesmo padrão de Veículos/Produtos.
- **Abastecimentos — campos:** veículo, motorista, data, odômetro atual, litros abastecidos,
  preço por litro, posto (opcional). Valor total é sempre **derivado** (`litros × preço/L`),
  nunca armazenado.
- **Km rodado desde o último abastecimento é calculado e congelado na criação** (não recalculado
  a cada render). Espelha o `RefuelingRecord` do backend, que também trata isso como um campo
  gravado, não uma view derivada. Se um registro anterior do mesmo veículo for editado depois,
  os posteriores não se reajustam sozinhos — aceitável para um dataset mock com poucos registros
  seed.
- **Abastecimentos usa só Tabela**, sem Cards — é um registro/log transacional (como um extrato),
  não uma entidade com "perfil" como Veículo/Motorista. Ordenado por data decrescente.
- **Exclusão bloqueada:** apagar um Veículo ou Motorista que tenha abastecimentos registrados é
  bloqueado, mesmo padrão já usado em Unidade↔Produto (`UnitsPageContent.handleDeleteClick`).
  Apagar um Abastecimento em si não é bloqueado por nada.
- **Reaproveita helpers existentes, sem duplicar:** `isValidCpf` (`lib/validations/document.ts`),
  `maskCpf`/`maskPhone` (`lib/masks.ts`), `formatDocument`/`formatPhone`/`formatCurrency`
  (`lib/format.ts`), `requiredNumber` (`lib/validations/zodNumber.ts`). Nenhum arquivo de
  máscara/formatação novo é necessário.

## Parte 1 — Motoristas (`/dashboard/motoristas`)

### Modelo de dados — `lib/validations/driver.ts`

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `fullName` | texto | sim | |
| `cpf` | texto | sim | 11 dígitos, dígito verificador (`isValidCpf`), máscara `maskCpf` no `onChange` |
| `phone` | texto | sim | 10 ou 11 dígitos, máscara `maskPhone` no `onChange` (mesma regra do Customer) |
| `cnhNumber` | texto | sim | exatamente 11 dígitos |
| `cnhCategory` | select | sim | `A`/`B`/`C`/`D`/`E`/`AB`/`AC`/`AD`/`AE` — exporta `CNH_CATEGORIES` (pares `value`/`label`), mesmo formato de `FUEL_TYPES` |
| `cnhValidity` | data (`type="date"`) | sim | só armazenada; sem regra de "não vencida" |
| `status` | select | sim | `ativo` / `inativo`, default `ativo` na criação |

`Driver extends DriverFormData { id: string }`, mesmo padrão de `Vehicle`/`Product`.

### Unicidade

`cpf` e `cnhNumber` validados como únicos no `handleSubmit` de `DriversPageContent`, ignorando o
próprio registro durante edição — mesma mecânica de `sku`/`document`/`plate` na spec anterior.
Colisão dispara toast `error` e mantém o modal aberto.

### Tela

Estrutura idêntica a `VehiclesPageContent`: `PageHeader` + `SearchInput` (filtra por nome, CPF e
número da CNH) + `ViewToggle` + grade ou tabela + `DriverFormModal` + `ConfirmDialog` de exclusão.

- **Card:** nome em destaque, badge de status (`ativo`=verde/`success`, `inativo`=cinza), CPF
  formatado (`formatDocument`), telefone formatado (`formatPhone`), categoria + validade da CNH.
- **Tabela:** Nome · CPF · Telefone · CNH (número + categoria) · Validade · Status · ações.
- **Modal** `size="md"`, campos em grid de 2 colunas: Nome completo (largura total), CPF /
  Telefone, Número CNH / Categoria, Validade CNH / Status.

Componentes em `components/drivers/`: `DriversPageContent`, `DriverGrid`, `DriverCard`,
`DriverTable`, `DriverFormModal`.

### Bloqueio de exclusão

`DriversPageContent` importa `useRefuelings` e, em `handleDeleteClick`, conta quantos
abastecimentos têm `driverId === motorista.id`. Se `usageCount > 0`, toast `error` ("Motorista em
uso — N abastecimento(s) usam este motorista.") e a exclusão não prossegue.

## Parte 2 — Abastecimentos (`/dashboard/abastecimentos`)

### Modelo de dados — `lib/validations/refueling.ts`

| Campo | Tipo | Obrigatório | Regra |
|---|---|---|---|
| `vehicleId` | select | sim | id de `Vehicle` |
| `driverId` | select | sim | id de `Driver` |
| `date` | data (`type="date"`) | sim | não pode ser data futura |
| `odometerKm` | número | sim | maior que zero **e** maior que o último odômetro registrado para o mesmo veículo (checagem cross-record, fora do zod — ver abaixo) |
| `litersRefueled` | número | sim | maior que zero |
| `pricePerLiter` | número | sim | maior que zero |
| `location` | texto | não | posto/local, sem regra de formato |

`Refueling extends RefuelingFormData { id: string; kmSincePrevious: number | null }` —
`kmSincePrevious` não é campo do formulário; é calculado e anexado no submit (ver
`lib/refuelingCalculations.ts`), `null` quando é o primeiro abastecimento daquele veículo.

A regra de odômetro (maior que o anterior) e o cálculo de `kmSincePrevious` dependem da lista
atual de abastecimentos, então ficam em `RefuelingsPageContent.handleSubmit`, não no schema zod —
mesma justificativa já usada para a checagem de unicidade de `sku`/`document`/`plate`.

### `lib/refuelingCalculations.ts` (novo arquivo compartilhado)

```
findLatestOdometer(refuelings: Refueling[], vehicleId: string, excludeId?: string): number | null
  // maior odometerKm entre os abastecimentos do veículo (exceto o registro em edição),
  // null se não houver nenhum

calculateKmSincePrevious(odometerKm: number, previousOdometerKm: number | null): number | null
  // odometerKm - previousOdometerKm, ou null se previousOdometerKm for null

calculateTotalPrice(litersRefueled: number, pricePerLiter: number): number
  // litersRefueled * pricePerLiter — nunca armazenado, só exibido (form e tabela)
```

Usado em dois lugares:

- **No `RefuelingFormModal`**, como preview ao vivo: observa `vehicleId`/`odometerKm` via
  `watch` do react-hook-form e mostra "Km rodado desde o último: X km" (ou "Primeiro
  abastecimento registrado para este veículo" quando `findLatestOdometer` retorna `null`), mais
  o total em R$ (`formatCurrency(calculateTotalPrice(...))`) abaixo dos campos de litros/preço.
- **No submit** (`RefuelingsPageContent.handleSubmit`), pra congelar `kmSincePrevious` no
  registro e validar `odometerKm > findLatestOdometer(...)` antes de salvar — senão toast
  `error`: *"Odômetro inválido — deve ser maior que o do abastecimento anterior deste veículo
  (X km)."*

### Tela

`PageHeader` + `SearchInput` (filtra por posto e nome do motorista) + um `Select` de filtro por
veículo (mesmo papel do `CustomerTypeFilter`, mas usando a lista de veículos em vez de um enum
fixo) + `RefuelingTable` (sem Cards) + `RefuelingFormModal` + `ConfirmDialog` de exclusão (sem
bloqueio).

- **Tabela**, ordenada por `date` decrescente: Data · Veículo (placa + modelo) · Motorista ·
  Odômetro · Litros · R$/L · Total · Km rodado · Posto · ações. Veículo e Motorista são
  resolvidos de `id` para rótulo legível via `useVehicles`/`useDrivers` (mesmo padrão de
  `ProductTable` resolvendo `unit` para nome via `useUnits`).
- **Modal** `size="lg"`, campos em grid de 2 colunas: Veículo / Motorista, Data / Odômetro,
  Litros / Preço por litro, Posto (largura total, opcional). Preview de km rodado e total logo
  abaixo dos campos correspondentes (ver seção anterior).

Componentes em `components/refuelings/`: `RefuelingsPageContent`, `RefuelingTable`,
`RefuelingFormModal`.

### Bloqueio de exclusão de Veículo/Motorista

`VehiclesPageContent` (e `DriversPageContent`, Parte 1) importam `useRefuelings` e checam
`refuelings.filter(r => r.vehicleId === vehicle.id).length` (respectivamente `driverId`) antes de
permitir excluir — mesma mecânica de `UnitsPageContent` checando uso em `useProducts`.

## Navegação

`Sidebar.tsx`, grupo "Cadastros", depois de Clientes: `Motoristas` (ícone `IdCard`) e
`Abastecimentos` (ícone `Fuel`), nessa ordem — confirmar durante a implementação que ambos os
nomes existem na versão instalada de `lucide-react` (o restante do grupo já usa `Car`, `Ruler`,
`Package`, `Users` da mesma biblioteca).

## Estados e erros

- Validação inline pelo padrão atual de `Input`/`Select` (borda `danger-300`, mensagem com ícone
  `AlertCircle`).
- Toast de sucesso ao criar, editar e excluir, nas duas telas.
- Toast `error` em CPF/CNH duplicados (Motoristas), odômetro inválido (Abastecimentos), e
  exclusão bloqueada por uso (Veículo/Motorista com abastecimentos) — modal permanece aberto
  quando o erro é de submit, fecha normalmente quando é de exclusão bloqueada.
- `EmptyState` distingue lista vazia de busca sem resultado, nas duas telas (correção já aplicada
  em Produtos/Clientes; Motoristas e Abastecimentos nascem corretos).

## Verificação

Sem infraestrutura de teste automatizado em `FrontEnd-Next` (consistente com os cadastros
anteriores). Verificação: `pnpm lint`, `pnpm build`, e passagem manual pelas duas telas via
`pnpm dev` + skill `webapp-testing` (Playwright) — criar/editar/excluir/buscar/filtrar em cada
uma, e confirmar os dois bloqueios cruzados de exclusão (Veículo e Motorista em uso por
Abastecimento).

## Fases de entrega

Cada fase é verificável isoladamente.

1. **Motoristas:** `driver.ts`, `useDrivers.ts`, componentes de `components/drivers/`, rota
   `/dashboard/motoristas`, item na Sidebar.
2. **Abastecimentos:** `refueling.ts`, `refuelingCalculations.ts`, `useRefuelings.ts`,
   componentes de `components/refuelings/`, rota `/dashboard/abastecimentos`, item na Sidebar,
   e os dois bloqueios cruzados de exclusão em `VehiclesPageContent`/`DriversPageContent`.

## Documentação a atualizar

- `TODO.md` — marcar "Motoristas" como feito; registrar Abastecimentos como cadastro adicional
  (fora da lista original) e a decisão de campos/regras tomada aqui.
