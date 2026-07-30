# Cadastro de Veículos — Design Spec

Data: 2026-07-30
Escopo: `FrontEnd-Next/` (Next.js)

## Contexto

Primeira tela de cadastro do sistema, entrando como uma nova área da aplicação (não
está relacionada às telas de rota/waypoints já existentes em `frontend/`). Segue o
mesmo padrão do dashboard (ver `docs/superpowers/specs/2026-07-29-dashboard-homepage-design.md`):
100% front-end por enquanto, sem integração com o backend Spring, dados mockados.

Design validado interativamente com o usuário via mockups (companion visual), reaproveitando
a paleta e os componentes de UI já existentes (`components/ui/Button.tsx`, `components/ui/Input.tsx`,
tokens de `app/globals.css`), com estética Stripe/Vercel (cantos arredondados, sombras
discretas, tipografia limpa).

## Decisões de escopo

- **Persistência:** nenhuma. Estado em memória (React), inicializado com uma lista mock
  de veículos de exemplo. Ao recarregar a página (F5), volta ao estado inicial — mesmo
  comportamento de `services/auth.ts` hoje (desconectado do backend).
- **Sem `services/vehicles.ts`:** diferente de `auth.ts` (que mocka uma chamada de rede
  que falha), aqui não há chamada de rede alguma a simular — CRUD é só manipulação de
  estado local. Um service faria sentido quando o backend expuser endpoints reais.
- **CRUD completo:** criar, editar e excluir fazem parte desta entrega (não é só
  cadastro/leitura).
- **Navegação:** novo grupo **"Cadastros"** na Sidebar (ao lado de "Geral" e "Rotas"),
  com o item "Veículos" apontando para `/dashboard/veiculos`. Grupo pensado para acomodar
  futuras telas de cadastro além de veículos.
- **Sem guarda de rota:** mesma situação do `/dashboard` — acessível diretamente por URL,
  sem checar sessão.

## Dados do veículo

| Campo | Tipo | Obrigatório | Observação |
|---|---|---|---|
| Placa | texto | sim | aceita formato antigo (`ABC-1234`) e Mercosul (`ABC1D23`) |
| Modelo | texto | sim | |
| Marca | texto | sim | |
| Ano | número | sim | |
| Cor | texto | sim | |
| Capacidade | número (kg) | sim | capacidade de carga |
| Tipo de combustível | select | sim | Diesel / Gasolina / Etanol / Elétrico |

## Layout

### Página `/dashboard/veiculos`

Sidebar (com o novo grupo "Cadastros" ativo em "Veículos") + área principal:

1. Header: título "Veículos" + subtítulo "Gerencie os veículos da sua frota", com:
   - toggle de visualização (ícones grade/lista) — alterna entre cards e tabela
   - botão primário "+ Novo veículo" à direita, abre o modal de criação
2. Corpo: grid de cards (visualização padrão) **ou** tabela, conforme o toggle.

### Visualização em cards (padrão)

Grid de **4 colunas**. Cada `VehicleCard`:
- topo: badge da placa (fundo `primary-50`, texto `primary-700`) + badge do combustível
  (cor varia por tipo, ex. verde para Diesel, vermelho para Gasolina, âmbar para Etanol)
- modelo (destaque) + marca/ano abaixo
- rodapé do card, separado por borda: capacidade em kg
- clicar no card abre o `VehicleFormModal` em modo edição
- cada card tem uma ação de exclusão (ícone, com confirmação — ver `DeleteVehicleDialog`)

### Visualização em tabela

Colunas: Placa, Modelo, Marca, Ano, Capacidade, + coluna de ações (editar/excluir).
Mesmo estilo de tabela usado nos mockups (header `gray-50`, bordas `gray-200`,
`rounded-xl`).

### Modal de criar/editar (`VehicleFormModal`)

Reaproveitado para os dois modos — título muda ("Novo veículo" / "Editar veículo") e os
campos vêm pré-preenchidos no modo edição. Estrutura:
- header: título + botão de fechar (X)
- corpo: campos em pares por linha (Placa/Ano, Marca/Modelo, Cor/Capacidade), select de
  combustível ocupando a linha inteira
- footer: botão secundário "Cancelar" + botão primário "Salvar veículo"

Implementado com `react-hook-form` + `zodResolver`, seguindo exatamente o padrão de
`components/auth/LoginForm.tsx` (mesmos componentes `Input`, mesmo tratamento de erro).

### Confirmação de exclusão (`DeleteVehicleDialog`)

Modal simples de confirmação ("Excluir veículo ABC-1D23? Essa ação não pode ser
desfeita.") com botão secundário "Cancelar" e botão de perigo "Excluir" (`danger-600`).

## Componentes a criar

- `app/dashboard/veiculos/page.tsx` — compõe Sidebar + header + grid/tabela + estado dos modais.
- `components/vehicles/VehicleGrid.tsx` — grid de `VehicleCard`, 4 colunas.
- `components/vehicles/VehicleCard.tsx` — card individual (props: veículo, `onEdit`, `onDelete`).
- `components/vehicles/VehicleTable.tsx` — visualização em tabela.
- `components/vehicles/ViewToggle.tsx` — botão de alternância cards/tabela (componente controlado; o estado de qual visualização está ativa vive em `page.tsx` e decide se renderiza `VehicleGrid` ou `VehicleTable`).
- `components/vehicles/VehicleFormModal.tsx` — modal de criar/editar.
- `components/vehicles/DeleteVehicleDialog.tsx` — modal de confirmação de exclusão.
- `lib/validations/vehicle.ts` — schema zod (`vehicleSchema`, `VehicleFormData`), incluindo
  regex de placa cobrindo os dois formatos.
- `hooks/useVehicles.ts` — estado em memória (lista inicial mock + `createVehicle`,
  `updateVehicle`, `deleteVehicle`).
- Atualização de `components/dashboard/Sidebar.tsx` — novo grupo "Cadastros" com item
  "Veículos".

## Estilo visual

- Nenhuma cor nova: reaproveita `primary-*` (roxo) e `gray-*` de `app/globals.css`.
  Badges de combustível usam os tokens `success-*` (Diesel), `danger-*` (Gasolina) e
  `warning-*` (Etanol) já existentes.
- Componentes `Button` e `Input` de `components/ui/` reaproveitados sem alteração.
- Modal centralizado, com overlay escuro semitransparente (`rgba(17,24,39,0.45)`),
  `rounded-2xl`, sombra pronunciada (`shadow-2xl`-like) — consistente com o restante do
  app (cards `rounded-xl`/`rounded-lg`, bordas `gray-200`).

## Testes

- `lib/validations/vehicle.ts`: teste Vitest cobrindo os dois formatos válidos de placa
  e casos inválidos, e obrigatoriedade dos demais campos.
- `VehicleFormModal`: teste cobrindo submissão válida (criação), submissão válida com
  dados pré-preenchidos (edição) e exibição de erros de validação.
- `useVehicles`: teste cobrindo `createVehicle`/`updateVehicle`/`deleteVehicle` sobre o
  estado em memória.

Segue o padrão de testes já usado no projeto (Vitest, ver `frontend/src/hooks` para
exemplos de teste de hook).

## Fora de escopo (explícito)

- Qualquer integração com o backend Spring (endpoint, model, persistência real).
- `services/vehicles.ts` — não há chamada de rede a mockar nesta entrega.
- Persistência entre recarregamentos (localStorage ou similar).
- Guarda de rota / autenticação.
- Paginação ou busca/filtro na lista de veículos (relevante só quando o volume justificar).
- Outras telas do grupo "Cadastros" além de Veículos (motoristas, clientes, etc.) — ficam
  para specs futuras.
