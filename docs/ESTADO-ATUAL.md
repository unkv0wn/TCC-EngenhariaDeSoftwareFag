# RouteWise — Estado atual do projeto

Inventário do que existe hoje no backend e nos dois frontends. Gerado a partir do código-fonte,
não é um documento de planejamento — reflete o que está implementado, mockado ou pendente neste momento.

---

## 1. Backend (`backend/`) — Spring Boot

**Stack:** Java 21, Spring Boot 3.2.5, Maven.

**Dependências principais** (`pom.xml`):
- `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`
- `spring-boot-starter-data-jpa` + `postgresql` (runtime) + `flyway-core` — **wireado mas não usado**: existe uma migration (`V1__init.sql`) e nenhuma entidade/repositório JPA aparece em lugar nenhum do código. Persistência está preparada na infra, mas o fluxo de cálculo de rota não usa banco.
- `de.topobyte:osm4j-core/xml/pbf` — parsing de `.osm`/`.pbf`
- `org.apache.commons:commons-math3` — usado só pelo pacote `validation/` (não é produção)
- `spring-boot-starter-test` (JUnit 5)

### Estrutura de pacotes (`src/main/java/com/routewise/`)

```
RouteWiseApplication.java        — entry point Spring Boot

algorithm/
  AStarWaypointOptimizer.java    — A* sobre estado (waypoint, bitmask de visitados); resolve
                                    o TSP variante para ROUND_TRIP / OPEN_ROUTE / FIXED_START_END,
                                    heurística admissível (mínima aresta de entrada)
  HaversineUtil.java             — distância em linha reta, usado como fallback quando o OSRM
                                    está indisponível

config/
  CorsConfig.java                 — libera origens configuradas (default localhost:5173) para /api/**
  RestTemplateConfig.java         — bean RestTemplate usado pelo cliente OSRM

controller/
  RouteController.java            — único controller REST (endpoints abaixo)

dto/
  RouteRequestDto.java             — entrada {waypoints[2..10], routeMode}
  RouteResultDto.java              — saída (waypoints ordenados, distância/duração, geometria,
                                      segmentos, validação OSRM)
  SseEventDto.java                  — payload de evento SSE (PROCESSING/COMPLETED/ERROR)
  WaypointDto.java                  — {lat, lng}
  osrm/OsrmRouteResponse.java, OsrmTableResponse.java — mapeamento das respostas do OSRM

exception/
  GlobalExceptionHandler.java       — mapeia erros de validação/domínio para ProblemDetail (RFC 9457)
  OsrmClientException.java          — falhas HTTP do OSRM
  RouteComputationException.java    — erros de domínio no cálculo de rota

model/
  RouteMode.java                    — enum ROUND_TRIP / OPEN_ROUTE / FIXED_START_END

service/
  IOsrmClient.java, IRouteOptimizerService.java, ISseEventService.java  — interfaces
  impl/
    OsrmClientServiceImpl.java      — chama OSRM /table e /route; converte lat/lng interno para
                                       lon/lat (ordem do OSRM)
    RouteOptimizerServiceImpl.java  — orquestra o pipeline: PROCESSING → matriz OSRM (fallback
                                       Haversine) → A* → geometria OSRM (degradação graciosa) →
                                       COMPLETED/ERROR via SSE
    SseEventServiceImpl.java        — gerencia SseEmitter em memória por requestId
  osm/
    OsmGraphService.java            — carregador opcional de grafo OSM (osm4j), desligado a menos
                                       que `routewise.osm.data-path` esteja configurado

validation/  (44 arquivos)          — código de pesquisa/experimento standalone, NÃO é produção
                                       (commons-math3 só serve pra isso). Cobre: calculadoras
                                       empíricas de consumo de combustível e vida útil de pneu
                                       (calibradas a partir de CSVs), modelagem de custo por tipo
                                       de via, análise de ocupação de carga e desgaste por eixo,
                                       experimentos de sensibilidade de custo, geração de cenários
                                       estatísticos, e vários scripts "Example"/"Experiment" que
                                       geram relatórios em Markdown (ex: ToledoRouteComparisonExample,
                                       FleetRouteSimulationExample) — é o kit de validação empírica
                                       do TCC, independente da API de roteamento em si.
```

### Endpoints REST (`RouteController`, base `/api/routes`)

| Método | Path | Função |
|---|---|---|
| `POST` | `/api/routes/compute` | Valida `{waypoints, routeMode}`, gera `requestId`, retorna `202 Accepted` |
| `GET` | `/api/routes/events/{requestId}` | Stream SSE — dispara o cálculo em virtual thread, emite `PROCESSING` → `COMPLETED`/`ERROR` |
| `GET` | `/api/routes/graph-stats` | Estatísticas do grafo OSM carregado (só relevante se `routewise.osm.data-path` estiver configurado) |

Outros recursos: `application.properties`, migration Flyway `V1__init.sql`, `empirical-data/` (CSVs + `FONTES.md`). Testes existem para `algorithm/` (`AStarTieBreakBenchmarkTest`, `AStarWaypointOptimizerTest`, `HaversineUtilTest`) e para várias calculadoras de `validation/`.

---

## 2. `frontend/` — React + Vite (protótipo de testes)

**Stack:** React 19.2, Vite 8, TypeScript ~6.0, `leaflet` + `react-leaflet`. Sem router, sem gerenciador de estado global — apenas estado de componente + hooks customizados. SPA de página única (`App.tsx`). Testes: Vitest + Testing Library (`useSseListener.test.ts`, `useWaypoints.test.ts`).

**Componentes** (`src/components/`):

| Componente | Função |
|---|---|
| `MapView.tsx` | Mapa Leaflet: clique adiciona waypoint, marcadores numerados/coloridos por papel (início/fim/round-trip), auto-fit de bounds, polylines por segmento (clicáveis) |
| `WaypointPanel.tsx` | Lista waypoints (lat/lng, botão remover), badge de contagem (máx 10) |
| `RouteControls.tsx` | Checkbox de modo de rota, botão "Estimate Route" (spinner), botão "Reset" |
| `SummaryPanel.tsx` | Distância/duração/modo total, detalhamento por segmento (clicável, sincroniza com o mapa), badge de validação OSRM |
| `StatusSnackbar.tsx` | Toast de status do cálculo (SUBMITTING/PROCESSING/COMPLETED/ERROR) |

**Hooks** (`src/hooks/`):

| Hook | Função |
|---|---|
| `useWaypoints.ts` | Estado do array de waypoints, limite 2–10, add/remove/clear |
| `useSseListener.ts` | Wrapper do `EventSource` nativo, escuta PROCESSING/COMPLETED/ERROR, fecha stream em evento terminal |

**Services** (`src/services/routeApi.ts`): `submitRoute()` (`POST /api/routes/compute`), `getSseUrl()` (monta URL do SSE). Espera proxy do Vite pro backend Spring Boot.

**Types** (`src/types/route.types.ts`): espelham os DTOs do backend 1:1.

**Status:** demo funcional de ponta a ponta (mapa → waypoints → cálculo → SSE → resultado), fala direto com o backend real. Sem autenticação, sem persistência, sem outras páginas — protótipo de propósito único pra validar o algoritmo/integração.

---

## 3. `FrontEnd-Next/` — Next.js (frontend de produção)

**Stack:** Next.js 16.2.12 (App Router), React 19.2, TypeScript 5, Tailwind CSS 4, `react-hook-form` + `zod`, `lucide-react`, `clsx`/`tailwind-merge`. Sem lib de estado global, sem testes configurados.

### Rotas (`app/`)

| Rota | Arquivo | Status |
|---|---|---|
| `/` | `app/page.tsx` | Redireciona direto pra `/login` — não é uma landing page real |
| `/login` | `app/login/page.tsx` | Renderiza `LoginForm` |
| `/dashboard` | `app/dashboard/page.tsx` | Sidebar + `KpiRow`; abaixo tem um placeholder literal: *"(espaço em aberto — definimos depois o que vem aqui)"* |
| `/dashboard/veiculos` | `app/dashboard/veiculos/page.tsx` | `VehiclesPageContent` — CRUD de veículos completo no client, sem backend |
| Layout raiz | `app/layout.tsx` | Fontes (Nunito, Geist Mono), `ToastProvider` |

### Componentes por pasta

- **`components/auth/`**
  - `LoginForm.tsx` — formulário com react-hook-form + zod, mostrar/ocultar senha, chama `services/auth.ts` (stub — ver abaixo)
- **`components/dashboard/`**
  - `Sidebar.tsx` — nav lateral agrupada (Geral → Dashboard; Cadastros → Veículos; Rotas → "Nova rota"/"Minhas rotas" **sem `href`**, ainda não ligadas)
  - `KpiRow.tsx`, `KpiTile.tsx` — grid de KPIs (presentacional, provavelmente números estáticos)
- **`components/ui/`** — design system básico: `Button`, `Checkbox`, `Divider`, `Input`, `Select`, `SocialButton`, mais `toast/` (`ToastProvider`, `ToastCard`, `ToastViewport`) — sistema de notificação funcional
- **`components/vehicles/`** — a área mais completa:
  - `VehiclesPageContent.tsx` — orquestrador: busca/filtro, toggle de view, criar/editar/deletar
  - `VehicleCard.tsx`, `VehicleGrid.tsx`, `VehicleTable.tsx`, `ViewToggle.tsx`
  - `VehicleFormModal.tsx` — modal criar/editar, react-hook-form + zod (`vehicleSchema`)
  - `DeleteVehicleDialog.tsx` — confirmação de exclusão

### Hooks (`hooks/`)

- `useToast.ts` — wrapper do `ToastContext`
- `useVehicles.ts` — **estado 100% local/em memória**: 4 veículos hardcoded como seed, create/update/delete só mutam estado React via `crypto.randomUUID()` — sem chamada de API, sem persistência entre reloads

### `lib/`

- `validations/auth.ts` — zod `loginSchema` (email, senha ≥8 chars, rememberMe)
- `validations/vehicle.ts` — zod `vehicleSchema`: regex de placa brasileira (formato antigo `ABC-1234` e Mercosul `ABC1D23`), modelo/marca/cor obrigatórios, validação de ano, capacidade > 0, tipo de combustível (diesel/gasolina/etanol/elétrico)
- `utils.ts` — `cn()` (clsx + tailwind-merge)

### `services/auth.ts` — **mockado/desconectado, de propósito**

```ts
// Backend ainda não conectado — chamada de rede removida temporariamente.
export async function login(_data: LoginFormData): Promise<AuthResponse> {
  throw new Error("login() está desconectado do backend por enquanto.");
}
export function loginWithProvider(provider: "google" | "microsoft") {
  console.warn(`loginWithProvider(${provider}) está desconectado do backend por enquanto.`);
}
```

O formulário de login está todo pronto (validação, estados de erro/sucesso, UI) mas qualquer submit falha por design. **Não existe nenhuma chamada ao backend Spring Boot em `FrontEnd-Next`** — nenhum `fetch`/`axios` fora desse stub.

### Resumo — funcional vs. pendente

| Funcional (só client, sem backend) | Mockado/placeholder | Ausente |
|---|---|---|
| CRUD de veículos (criar/editar/deletar/buscar/toggle view) | Login (falha por design) | Qualquer conexão com `/api/routes/*` |
| Notificações toast | Conteúdo principal do dashboard | UI de otimização de rota (mapa, waypoints, SSE) — existe só no `frontend/` |
| Validação de formulários | Nav "Nova rota"/"Minhas rotas" (sem href) | |
| Navegação entre rotas construídas | Página `/` (só redirect) | |
