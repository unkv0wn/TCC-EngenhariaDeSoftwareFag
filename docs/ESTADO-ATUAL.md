# RouteWise — Estado atual do projeto

Inventário do que existe hoje nas quatro peças do repositório. Levantado a partir do código-fonte;
não é documento de planejamento — o roadmap é o `TODO.md`.

**Atualizado em:** 2026-10-21.

> Documento de inventário envelhece rápido. Se alguma afirmação aqui contradisser o código, o
> código é que vale — e vale corrigir esta página.

---

## Visão geral

Quatro peças, desenvolvidas separadamente:

| Peça | Stack | Papel |
|---|---|---|
| `backend/` | Java 21, Spring Boot 3.2.5 | Núcleo funcional (A*) + API REST + persistência |
| `FrontEnd-Next/` | Next.js 16.2.12, React 19.2, Tailwind 4 | **Painel do gestor — é o que vai pra produção** |
| `mobile/` | Flutter, Dart 3.5+ | App de campo do motorista (Android) |
| `frontend/` | React 19, Vite 8, Leaflet | Protótipo de validação do A* — **congelado, não entrega** |

O fluxo de negócio implementado hoje:

```
Pedido (cliente + itens)  →  faturado  →  Rota (A* sobre os endereços)
                                            ↓
                              motorista abre a rota no app por código/QR
                                            ↓
                              registra chegada/entrega/ocorrência em campo
                                            ↓
                              entrega fecha o pedido (entregue)
                                            ↓
                              rota encerrada; o não entregue volta pra fila
```

---

## 1. Backend (`backend/`)

**Dependências principais:** `spring-boot-starter-web`, `-validation`, `-actuator`, `-data-jpa`,
`postgresql`, `flyway-core`, `jjwt` 0.12.6, `spring-security-crypto` (só o BCrypt — **não** o
starter de segurança), `osm4j` 1.4.1 (core/xml/pbf), `commons-math3` 3.6.1 (só o pacote
`validation/`).

### Persistência — ativa

PostgreSQL via JPA + Flyway. **15 entidades, 13 repositórios, 18 migrações** (`V1__init.sql`,
baseline vazio, até `V18__create_mobile_users.sql`). `ddl-auto=validate`: o Hibernate confere as
entidades contra o schema e nunca gera DDL — toda mudança de schema passa por migração nova.

Entidades: `Vehicle`, `Driver`, `Unit`, `Product`, `Customer`, `Refueling`, `PaymentMethod`,
`PaymentCondition`, `Order` (+ `OrderItem`, `OrderHistoryEntry`), `SavedRoute`, `RouteEvent`,
`CompanySettings`, `MobileUser`.

Serviços seguem `I<Nome>Service` + `<Nome>ServiceImpl`, `@Transactional` na classe com
`@Transactional(readOnly = true)` nas leituras. Exclusões referenciais são barradas na camada de
serviço, não por cascade de FK.

### Pacotes (`src/main/java/com/routewise/`)

```
algorithm/     AStarWaypointOptimizer, HaversineUtil
auth/          JwtService, MobileSession, MobileAuthFilter, RouteAccessCodeGenerator
config/        CorsConfig, RestTemplateConfig, MobileAuthFilterConfig
controller/    17 controllers REST
dto/           ~44 records de entrada/saída (+ dto/osrm/)
entity/        15 entidades JPA
exception/     GlobalExceptionHandler (RFC 9457) + 7 exceções de domínio
model/         RouteMode
repository/    13 interfaces Spring Data
service/       CRUD + RouteStopResolver, RouteOrderLifecycle, RouteStatus
service/impl/  OsrmClientServiceImpl, RouteOptimizerServiceImpl, SseEventServiceImpl
service/osm/   OsmGraphService (desligado por padrão)
validation/    ~50 experimentos de TCC — não fazem parte do pipeline de requisição
```

### Endpoints REST

Todos sob `/api`. Os CRUDs seguem a mesma forma (`GET`, `POST`, `PUT /{id}`, `DELETE /{id}`).

| Base | Controller | Observação |
|---|---|---|
| `/api/auth/login` | `AuthController` | emite JWT pra **qualquer** e-mail, sem conferir senha |
| `/api/vehicles`, `/api/drivers`, `/api/units`, `/api/products`, `/api/customers`, `/api/refuelings`, `/api/payment-methods`, `/api/payment-conditions` | um por recurso | CRUD puro |
| `/api/orders` | `OrderController` | CRUD + `PATCH /{id}/status` |
| `/api/routes` | `SavedRouteController` | `GET`, `GET /{id}`, `POST`, `PATCH /{id}/status`, `DELETE /{id}` |
| `/api/routes/compute`, `/api/routes/events/{requestId}`, `/api/routes/graph-stats` | `RouteController` | pipeline de cálculo |
| `/api/routes/{routeId}/events` | `RouteEventController` | log de acompanhamento |
| `/api/mobile/auth/login` | `MobileAuthController` | login real do app (senha + aparelho cadastrado) |
| `/api/mobile/routes/{code}` | `MobileRouteController` | `GET`, `POST /finish`, `POST /events`, `POST /events/batch` — **exige sessão de motorista** |
| `/api/mobile-users` | `MobileUserController` | o gestor concede/revoga acesso ao app |
| `/api/settings` | `CompanySettingsController` | `GET` + `PUT`, linha única |

### Pipeline de cálculo de rota

Dois passos, REST + SSE, e **não toca banco**:

1. `POST /api/routes/compute` valida, guarda o pedido num `ConcurrentHashMap` em memória e devolve
   `{requestId}` (202).
2. `GET /api/routes/events/{requestId}` abre o `SseEmitter` e despacha o cálculo numa virtual
   thread (Java 21).
3. `RouteOptimizerServiceImpl` emite `PROCESSING`, busca a matriz de durações no OSRM (`/table`),
   roda o A*, pede a geometria (`/route`) e emite `COMPLETED` ou `ERROR`.

Duas degradações deliberadas: sem o `/table` do OSRM a matriz é aproximada por Haversine a 40 km/h
(muda a **entrada da otimização**, não só o desenho); sem o `/route` a ordem otimizada volta sem
geometria, com `osrmValidation: UNAVAILABLE`.

### A*

TSP variante por A* sobre estado `(waypoint atual, bitmask de visitados)`; custo é duração em
segundos. Heurística admissível: para cada nó não visitado, a menor aresta de **entrada** na matriz
(+ limite inferior do retorno, no `ROUND_TRIP`). Desempate por maior `g` — muda só quantos estados
são expandidos, nunca o custo. Limite de 2–15 waypoints (espaço `N × 2^N`). Três modos, todos
implementados: `ROUND_TRIP`, `OPEN_ROUTE`, `FIXED_START_END`.

### Autenticação — duas histórias diferentes

**App do motorista (`/api/mobile/**`) — real e exigida.** `MobileAuthController` confere senha em
BCrypt contra `mobile_users` e recusa se o `deviceId` do pedido não for o que o gestor cadastrou.
`MobileAuthFilter`, registrado só para `/api/mobile/*`, recusa com 401 qualquer outra chamada sem
token válido. Os tokens do app levam `typ: "mobile"`, então um token do painel não passa por eles.
O `deviceId` é o `Settings.Secure.ANDROID_ID` — **não é IMEI**, que o Android não entrega a app de
terceiro desde a versão 10.

**Painel (`/api/auth/login`) — só navegacional.** `AuthController` emite token pra qualquer e-mail,
sem senha e sem tabela de usuário. A conferência de credencial mora no front
(`app/api/login/route.ts`, par fixo no código) e o `proxy.ts` do Next valida o cookie só pra
decidir se a tela abre. O `lib/apiClient.ts` **não manda** `Authorization`. Resultado: todo
`/api/**`, menos `/api/mobile/**`, responde sem autenticação — inclusive `/api/mobile-users`, que é
quem concede acesso ao app. O segredo do JWT está fixo no `application.properties` e duplicado no
`docker-compose.yml` da raiz.

### Ciclo pedido ↔ rota

`RouteOrderLifecycle` é o único lugar que liga os dois, em quatro momentos: rota criada
(`faturado → em_rota`), entrega registrada (`em_rota → entregue`, só os pedidos daquela parada),
rota encerrada e rota excluída (`em_rota → faturado`, o "retorno à empresa"). O vínculo vive em
`LabeledStop.orderIds`, dentro do JSON de paradas — `SavedRoute` guarda só a contagem. **Rotas
salvas antes desse campo não têm vínculo e são ignoradas em silêncio:** o dado nunca foi registrado
e não é recuperável.

### Grafo OSM

`OsmGraphService` carrega um `.osm`/`.pbf` em memória no startup, mas está **desligado por padrão**
(`routewise.osm.data-path` em branco). Não alimenta o A* — a matriz vem do OSRM. Serve só ao
`GET /api/routes/graph-stats`.

### Testes

**12 classes, 63 testes.** Dez em `algorithm/` e `validation/` (o otimizador, o benchmark de
desempate, Haversine, as calculadoras empíricas). Duas em `service/`: `RouteStopResolverTest` e
`RouteOrderLifecycleTest`, cobrindo o ciclo do pedido.

Não há teste de controller nem de repositório, e nenhum `@SpringBootTest` (precisaria de Postgres
de pé ou Testcontainers, que não estão montados).

⚠️ **`mvn test` não é somente leitura.** `AStarTieBreakBenchmarkTest` reescreve
`docs/refactor-math-result/comparacao-tempos.md` e `comparacao_15.md` com os tempos da máquina que
rodou. Conferir `git status docs/` depois da suíte.

---

## 2. `FrontEnd-Next/` — painel do gestor (produção)

**Stack:** Next.js 16.2.12 (App Router), React 19.2.4, TypeScript 5, Tailwind 4, `react-hook-form`
+ `zod` 4, `lucide-react`, `clsx`/`tailwind-merge`, `leaflet` + `react-leaflet`, `jose`.
**Sem testes e sem runner configurado.**

**Está ligado ao backend.** `lib/apiClient.ts` é o wrapper de fetch (lê `NEXT_PUBLIC_API_URL`,
lança `ApiError` com a mensagem do problem detail); `services/<recurso>.ts` envolve os endpoints;
`hooks/use<Recurso>.ts` guarda a lista + `isLoading`/`error` e expõe create/update/delete que
recarregam do servidor.

### Telas (`app/`)

| Rota | Conteúdo |
|---|---|
| `/` | redireciona pra `/login` |
| `/login` | `LoginForm` → `app/api/login/route.ts` (cookie httpOnly `routewise`) |
| `/dashboard` | KPIs |
| `/dashboard/veiculos`, `/motoristas`, `/abastecimentos` | cadastros de frota |
| `/dashboard/acessos-app` | **acessos do app do motorista** (usuário, senha, aparelho autorizado) |
| `/dashboard/unidades`, `/produtos` | catálogo |
| `/dashboard/clientes` | clientes/fornecedores (campo `type`) |
| `/dashboard/formas-pagamento`, `/condicoes-pagamento` | financeiro |
| `/dashboard/pedidos` | pedidos + máquina de status |
| `/dashboard/rotas` | lista de rotas salvas |
| `/dashboard/rotas/gerar` | gera a rota a partir dos pedidos faturados (mapa + A* + SSE) |
| `/dashboard/rotas/[id]` | acompanhamento: mapa, paradas, eventos, documentos |
| `/dashboard/configuracoes` | dados da empresa (origem da rota) |

`proxy.ts` (o Next 16 renomeou `middleware.ts` → `proxy.ts`) protege `/dashboard/:path*`.

### Estrutura

- **`components/`** — 15 pastas por feature + `components/ui/` com 19 primitivas compartilhadas
  (`Button`, `Input`, `Modal`, `ConfirmDialog`, `PageHeader`, `CreateButton`, `SearchInput`,
  `EmptyState`, `LoadingState`, `ViewToggle`, `Pagination`, `ActionsMenu`, `SearchableSelect`,
  `toast/`…). Cada cadastro repete a forma `<Feature>PageContent` + `<Feature>FormModal` +
  `<Feature>Table` (+ `Card`/`Grid` onde há visão de cartões). **Não há primitiva `Select`** —
  dropdown com busca é `SearchableSelect`; filtro simples é um `<select>` nativo estilizado.
- **`hooks/`** — 16, um por recurso, mais `usePagination`, `useToast`, `useRouteComputation`.
- **`services/`** — 18, incluindo quatro APIs públicas de terceiros chamadas direto do navegador,
  todas gratuitas e sem chave: ViaCEP, BrasilAPI (CNPJ), FIPE via BrasilAPI, Nominatim
  (geocoding, com CEP v2 da BrasilAPI como reserva e pin manual como último recurso).
- **`lib/validations/`** — 15 schemas Zod, um por recurso.

### Pontos mockados, declarados como mock

- `components/rotas/tracking/DocumentsPanel.tsx` — a aba de anexos (canhoto, nota, foto) mostra o
  formato final, mas nada sobe arquivo: falta decidir armazenamento, limite e retenção.
- `lib/routePrototype.ts` — coordenadas aproximadas por cidade + jitter, ainda importado por
  `GerarRotaScreen` e `GeocodeConfirmModal` como reserva quando o cliente não tem coordenada.

### Lacunas conhecidas

- **Modo escuro pela metade:** nenhum componente usa `dark:`. O escuro só repinta o fundo/texto do
  `body` pelas variáveis CSS; cartões, modais e inputs têm classe clara fixa e não acompanham.
- Máquina de status do pedido duplicada: `VALID_STATUS_TRANSITIONS` em `hooks/useOrders.ts` espelha
  a constante do `OrderServiceImpl`. Mudar uma exige mudar a outra.

---

## 3. `mobile/` — app do motorista (Flutter)

Android primeiro. Dart 3.5+, `http`, `shared_preferences`, `connectivity_plus`, `url_launcher`,
`mobile_scanner` (QR), `app_links` (deep link), `intl`, `uuid`. Consome **só** `/api/mobile/**`.

**Telas:** `login_screen` (usuário, senha e o identificador deste aparelho, com botão de copiar —
é como o gestor recebe o código pra cadastrar), `open_route_screen` (QR, deep link ou código
digitado), `route_summary_screen`, `stop_detail_screen`, `scan_code_screen`.

**Serviços:** `api_client` (quatro chamadas + login, com `Authorization` em todas), `session_service`
e `session_controller` (sessão entre aberturas), `device_identity` (canal nativo lendo o
`ANDROID_ID`, com UUID gravado como reserva), `route_controller` (estado da rota juntando servidor e
fila), `event_queue` (**uma fila por rota**), `deep_link_service`.

A regra que organiza o app: **registrar nunca falha por falta de rede.** O evento entra na fila, a
tela já reflete o efeito, e a sincronização acontece quando a conexão voltar.

**Testes:** 4 arquivos / 39 testes — contrato de wire com `/api/mobile/**`, o que vale como código
de acesso, a fila por rota e o contrato do login. `flutter analyze` é o portão de compilação real.

**Pendente:** tela de mapa da rota (`route_map_screen.dart`, prevista no spec e não construída) e
upload real de foto/canhoto (mesma decisão de armazenamento pendente do painel).

Spec: `docs/superpowers/specs/2026-10-05-driver-mobile-app-design.md`.

---

## 4. `frontend/` — protótipo congelado

React 19.2, Vite 8, TypeScript ~6.0, Leaflet. SPA de página única, sem router e sem estado global.
Validou o núcleo de ponta a ponta (mapa → waypoints → cálculo → SSE → resultado) falando direto com
o backend. Testes: Vitest + Testing Library (`useWaypoints`, `useSseListener`) — **os únicos testes
de frontend do repositório**.

**Não entrega e não recebe feature nova.** Fica como referência; UI nova vai no `FrontEnd-Next/`.

---

## Resumo honesto

| Funciona de ponta a ponta | Mockado, declarado como mock | Ausente |
|---|---|---|
| Cadastros (8 recursos), com CEP/CNPJ/FIPE automáticos | Anexos de documento/foto (painel e app) | Autenticação real no painel |
| Pedidos com máquina de status | Coordenada aproximada por cidade (reserva) | Tela de registros de login do app |
| Geração de rota (A* + OSRM + SSE) | | Mapa da rota no app do motorista |
| Acompanhamento da rota (mapa, paradas, eventos) | | Testes de controller/repositório |
| App do motorista: login com aparelho, rota por QR, eventos offline | | Modo escuro de verdade |
| Ciclo do pedido fechado (faturado → em rota → entregue) | | |
