# Tela de Acompanhamento de Rota — Design Spec

Data: 2026-09-21
Escopo: `FrontEnd-Next/` (Next.js) + `backend/` (Spring Boot, nova entidade)

## Contexto

Hoje o botão "Acompanhar" do card de rota abre o `RouteDetailModal` — um mockup declarado como
tal no próprio Javadoc do arquivo: mostra mapa e lista de paradas, mas `completedStops` é um
número congelado, salvo junto com a rota e que nunca muda. Não existe nenhum registro do que
aconteceu durante a viagem.

Esta spec troca esse modal por uma tela própria, que passa a ser o lugar onde a rota é
acompanhada de verdade: o que foi entregue, o que deu errado, e os documentos da viagem.

É o primeiro módulo do projeto em que o front-end nasce junto com uma entidade nova no backend
(os cadastros anteriores vieram depois, sobre tabelas já existentes).

## Decisões de escopo

- **Página própria, não modal** — `/dashboard/rotas/[id]`. URL compartilhável (dá para mandar o
  link de uma rota específica), espaço para mapa grande e painel lateral, e histórico de
  navegação funcionando. O botão "Acompanhar" passa a navegar em vez de abrir modal.
- **`RouteDetailModal` é removido**, não mantido em paralelo. Manter os dois significaria duas
  telas de acompanhamento divergindo — e o modal já era declaradamente provisório.
- **Top bar troca o painel lateral, não o mapa.** O mapa fica fixo ocupando a maior parte da
  tela; as abas **Paradas / Eventos / Documentos** escolhem o que aparece ao lado. Um conteúdo
  por vez, sem transformar a página numa rolagem longa.
- **Eventos persistem no backend de verdade** — tabela `route_events`, com migration, entidade,
  serviço e endpoints. É o que tira a tela do status de mockup. Sem isso o registro sumiria a
  cada recarga, e um log de ocorrências que some não serve para nada.
- **Documentos são mock, explicitamente.** Upload de arquivo exige decidir onde armazenar
  (disco, S3, coluna `bytea`), limites de tamanho e políticas de retenção — nada disso está
  decidido, e não é o que dá valor agora. A aba mostra uma lista de anexos fictícios com o
  visual final, e o botão de anexar abre um seletor que só adiciona o nome do arquivo à lista em
  memória. O componente deixa claro no código que é mock, igual `lib/routePrototype.ts`.
- **Autoria do evento é um campo do formulário**, não autenticação. Não existe login por usuário
  (ver "Authentication" no `CLAUDE.md`): quem registra escolhe entre **Motorista** e **Gestor**.
  Escolhendo motorista, o nome vem preenchido com o motorista escalado na rota. Quando houver
  login de verdade, esse campo vira derivado da sessão — o formato do dado não muda.
- **`completedStops` deixa de ser um número solto.** Passa a ser derivado dos eventos de entrega:
  a parada está concluída quando existe um evento do tipo `entrega` apontando para ela. A coluna
  continua na tabela `routes` por compatibilidade com as rotas já salvas, mas a tela não a usa
  mais para desenhar o progresso.

## Parte 1 — Backend: eventos de rota

### Migration — `V15__create_route_events.sql`

```sql
CREATE TABLE route_events (
  id          UUID PRIMARY KEY,
  route_id    UUID NOT NULL REFERENCES routes (id) ON DELETE CASCADE,
  stop_index  INTEGER,
  type        VARCHAR(20)  NOT NULL,
  author_role VARCHAR(20)  NOT NULL,
  author_name VARCHAR(120) NOT NULL,
  notes       TEXT,
  occurred_at TIMESTAMPTZ  NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_route_events_route ON route_events (route_id, occurred_at);
```

`ON DELETE CASCADE` porque um evento não existe fora da sua rota — apagar a rota apaga o log.
É a primeira FK real do schema; os cadastros anteriores referenciam por UUID solto e resolvem
no front. Aqui a FK se justifica porque a relação é de composição, não de referência.

`stop_index` é nulo para eventos que não pertencem a uma parada específica (saída, abastecimento,
ocorrência no caminho). Quando preenchido, é o índice da parada na ordem otimizada — o mesmo
número que o marcador mostra no mapa.

### Tipos de evento

| `type` | Uso | Exige `stop_index` |
|---|---|---|
| `saida` | Veículo deixou o depósito | não |
| `chegada` | Chegou na parada | sim |
| `entrega` | Entrega concluída na parada | sim |
| `ocorrencia` | Problema: cliente ausente, recusa, avaria, trânsito | não (opcional) |
| `abastecimento` | Parou para abastecer | não |
| `retorno` | Voltou ao depósito | não |

Lista fechada, validada no backend. Segue o padrão de `Order.status` (string validada em
serviço) em vez de `enum` JPA — coerente com o que já existe, embora a observação do `CLAUDE.md`
sobre string versus enum valha aqui também.

`author_role` aceita `motorista` ou `gestor`.

### Endpoints — `RouteEventController`

| Método | Caminho | Retorno |
|---|---|---|
| `GET` | `/api/routes/{routeId}/events` | lista ordenada por `occurredAt` |
| `POST` | `/api/routes/{routeId}/events` | 201 + o evento criado |
| `DELETE` | `/api/routes/{routeId}/events/{id}` | 204 |

Não há `PUT`: um log de eventos se corrige apagando e registrando de novo, não editando.

**Sobre o path base:** `/api/routes` já é compartilhado por `RouteController` (compute/SSE) e
`SavedRouteController` (CRUD). `GET /api/routes/events/{requestId}` (SSE) e
`GET /api/routes/{routeId}/events` (este) não colidem — um exige o literal `events` no primeiro
segmento, o outro no segundo. Vale registrar porque é o tipo de coisa que quebra silenciosamente
se alguém inverter a ordem depois.

### Camadas

`RouteEvent` (entidade) · `RouteEventRepository` (`findByRouteIdOrderByOccurredAtAsc`) ·
`IRouteEventService` + `RouteEventServiceImpl` · `RouteEventDto` / `RouteEventRequestDto`.
Mesmo formato dos outros módulos: `@Transactional` na classe, `readOnly` nas leituras,
`ResourceNotFoundException` quando a rota ou o evento não existe.

## Parte 2 — Front-end: `/dashboard/rotas/[id]`

### Estrutura

```
app/dashboard/rotas/[id]/page.tsx        # server component, resolve o id e renderiza a tela
components/rotas/tracking/
  RouteTrackingScreen.tsx                # composição: header + top bar + mapa + painel
  TrackingTabs.tsx                       # a top bar (Paradas | Eventos | Documentos)
  StopsPanel.tsx                         # paradas, com estado derivado dos eventos
  EventsPanel.tsx                        # linha do tempo + formulário de registro
  EventFormModal.tsx                     # registrar evento
  DocumentsPanel.tsx                     # MOCK — anexos fictícios
hooks/useRouteEvents.ts
services/routeEvents.ts
lib/validations/routeEvent.ts
```

### Layout

```
┌────────────────────────────────────────────────────────────┐
│ ← Rotas   Rota de 21/09  ·  Saída 20:02      [Em rota ▾]   │  header
│ Carlos Eduardo Santos · ABC-1234 — Sprinter · 1.500 kg     │
├────────────────────────────────────────────────────────────┤
│ [ Paradas ]  [ Eventos ]  [ Documentos ]                   │  top bar
├──────────────────────────────┬─────────────────────────────┤
│                              │                             │
│           MAPA               │      painel da aba          │
│      (RouteMap, paradas      │      selecionada            │
│       numeradas)             │                             │
└──────────────────────────────┴─────────────────────────────┘
```

Em telas estreitas as duas colunas viram uma, mapa primeiro (`lg:grid-cols-[1fr_380px]`).

### Abas

- **Paradas** — a lista que hoje está no modal, com o número da parada batendo com o marcador do
  mapa e o badge de pedidos quando a parada agrupa mais de um. Concluída quando existe evento
  `entrega` apontando para ela. Cada parada tem uma ação rápida "Registrar entrega", que abre o
  formulário de evento já preenchido com tipo e `stopIndex`.
- **Eventos** — linha do tempo em ordem cronológica: hora, tipo (com ícone e cor), autor
  (`Motorista · Carlos` / `Gestor`), parada referenciada quando houver, e a observação.
  Botão "Registrar evento" no topo; excluir por item, com `ConfirmDialog`.
- **Documentos** — **mock**. Lista de anexos fictícios (canhoto assinado, nota fiscal, foto da
  entrega) com nome, tipo, tamanho e data. "Anexar documento" adiciona à lista em memória.

### Reaproveitamento

`Modal`, `ConfirmDialog`, `PageHeader`, `EmptyState`, `LoadingState`, `ConfirmDialog`, `Textarea`
e o `toast` saem de `components/ui/` sem alteração. O mapa é o `RouteMap` já existente, com a
numeração introduzida em `72e3962`. `useRouteEvents` segue o formato dos outros hooks (lista +
`isLoading` + `error` + operações que recarregam do servidor).

## Fora de escopo

- Posição real do veículo (GPS/telemetria) — não existe fonte de dado para isso.
- Upload real de arquivo.
- Notificação ao gestor quando o motorista registra ocorrência.
- Acesso do motorista pelo celular com login próprio — hoje qualquer um com o link registra
  evento escolhendo o papel no formulário.
