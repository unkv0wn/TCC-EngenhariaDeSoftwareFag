# RouteWise — Otimizador de Rotas Urbanas

Aplicação web que calcula a ordem ótima de visita para múltiplos waypoints em ambiente urbano. O usuário posiciona de 2 a 10 pontos em um mapa interativo e o sistema retorna a melhor rota usando o algoritmo A*, validada e geometrizada via OSRM, com atualizações em tempo real por SSE.

> Trabalho de Conclusão de Curso — Engenharia de Software

O `FrontEnd-Next/` (Next.js) é o frontend oficial do projeto, que vai para produção — inclui tela de login e dashboard pós-login, camada de autenticação e experiência de usuário que o núcleo original não tinha. O `frontend/` (React + Vite) é um protótipo de testes: valida o núcleo de cálculo de rotas (backend, stateless) de ponta a ponta, mas não é a versão que será entregue.

---

## Tecnologias

| Camada | Stack |
|--------|-------|
| Backend | Java 21 (Virtual Threads) + Spring Boot 3.2.5 + Maven |
| Frontend (produção, `FrontEnd-Next/`) | Next.js 16 + React 19 + TypeScript + Tailwind CSS 4 |
| Frontend (testes, `frontend/`) | React 18 + TypeScript + Vite + Leaflet.js |
| Roteamento | OSRM (Open Source Routing Machine) — API pública |
| Comunicação | REST + SSE (Server-Sent Events) |
| Testes | JUnit 5 (backend) + Vitest (frontend) |

---

## Funcionalidades

- Mapa interativo com marcadores numerados (Leaflet.js)
- Otimização de rota com A* + heurística Haversine
- Três modos de rota: `ROUND_TRIP`, `OPEN_ROUTE`, `FIXED_START_END`
- Validação e geometria de rota via OSRM (duração, distância, traçado)
- Streaming de progresso em tempo real via SSE
- Degradação graciosa: se o OSRM falhar, retorna o resultado do A* diretamente
- Backend totalmente stateless — sem banco de dados, sem autenticação (o cálculo de rota em si não depende de usuário logado)
- Tela de login e dashboard no frontend de produção (`FrontEnd-Next/`, em construção) — ainda desconectados do backend, que não expõe endpoints de autenticação hoje

---

## Estrutura do Projeto

```
TCC-EngenhariaDeSoftwareFag/
├── backend/                         # Spring Boot
│   └── src/main/java/com/routewise/
│       ├── algorithm/               # A* + Haversine
│       ├── config/                  # CORS, RestTemplate
│       ├── controller/              # RouteController (REST + SSE)
│       ├── dto/                     # DTOs de entrada/saída (+ dto/osrm)
│       ├── exception/               # GlobalExceptionHandler
│       ├── model/                   # RouteMode enum
│       ├── service/                 # Interfaces e implementações (service/impl, service/osm)
│       └── validation/              # Experimentos standalone de validação empírica (TCC)
├── frontend/                        # React + Vite (protótipo de testes, conectado ao backend, NÃO vai para produção)
│   └── src/
│       ├── components/              # MapView, WaypointPanel, RouteControls, SummaryPanel
│       ├── hooks/                   # useWaypoints, useSseListener
│       ├── services/                # routeApi.ts
│       └── types/                   # route.types.ts
├── FrontEnd-Next/                   # Next.js (frontend oficial, vai para produção)
│   ├── app/                         # Rotas (App Router): login, dashboard, dashboard/veiculos
│   ├── components/                  # auth/, dashboard/ (KpiRow, Sidebar), ui/, vehicles/
│   ├── hooks/                       # useVehicles
│   ├── lib/                         # validações (zod) e utilitários
│   └── services/                    # auth.ts — hoje sem chamadas reais ao backend
├── docs/                            # SRS, casos de uso, cronograma
│   ├── superpowers/specs/           # Design specs de features em andamento
│   └── validation-reports/          # Relatórios gerados pelos experimentos de validation/
├── scripts/                         # Utilitários PowerShell / GeoJSON de exemplo
└── REQUIREMENTS.md                  # User stories e critérios de aceite
```

---

## Pré-requisitos

- Java 21 LTS
- Apache Maven 3.9+
- Node.js 20+

Acesso à API pública do OSRM (`router.project-osrm.org`) — sem chave de API, sujeito a rate limit.

---

## Como Executar

### Backend

```bash
cd backend
mvn clean install        # compila e roda os testes
mvn spring-boot:run      # sobe na porta 8080
```

### Frontend de produção (`FrontEnd-Next/`)

```bash
cd FrontEnd-Next
pnpm install
pnpm dev                 # sobe em http://localhost:3000
```

Ainda não depende do backend rodando — login e dashboard usam dados mockados/desconectados enquanto essas telas estão em construção.

### Frontend de testes (`frontend/`)

```bash
cd frontend
npm install
npm run dev              # sobe em http://localhost:5173 (proxy /api → :8080)
```

Acesse `http://localhost:5173` com o backend rodando. Usado apenas para validar o núcleo de rotas — não é a versão entregue.

### Build de Produção

```bash
# Frontend (produção)
cd FrontEnd-Next && pnpm build

# Backend
cd backend && mvn package      # gera target/routewise-backend-*.jar
java -jar target/routewise-backend-*.jar
```

---

## Endpoints da API

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| `POST` | `/api/routes/compute` | Submete waypoints, retorna `requestId` |
| `GET` | `/api/routes/events/{requestId}` | Stream SSE de progresso e resultado |
| `GET` | `/api/routes/graph-stats` | Estatísticas do grafo OSM carregado |

### Exemplo de Request

```json
POST /api/routes/compute
{
  "waypoints": [
    { "lat": -23.5505, "lng": -46.6333 },
    { "lat": -23.5614, "lng": -46.6559 },
    { "lat": -23.5489, "lng": -46.6388 }
  ],
  "routeMode": "ROUND_TRIP"
}
```

### Eventos SSE

```
event: PROCESSING   → computação em andamento
event: COMPLETED    → resultado com waypoints ordenados, geometria e métricas
event: ERROR        → mensagem de erro
```

---

## Algoritmo

O A* modela o Problema do Caixeiro Viajante com bitmask de estados:

- **Estado:** `(índice do waypoint atual, máscara de visitados)`
- **Custo:** duração de deslocamento em segundos (matrix do OSRM)
- **Heurística:** distância Haversine até o waypoint mais distante não visitado

A complexidade temporal é `O(n² · 2ⁿ)`, adequada para até 10 waypoints.

---

## Configurações Relevantes

`backend/src/main/resources/application.properties`:

```properties
server.port=8080
spring.threads.virtual.enabled=true          # Java 21 Virtual Threads
routewise.osrm.base-url=https://router.project-osrm.org
routewise.osrm.timeout-ms=10000
routewise.sse.timeout-ms=180000              # TTL do stream SSE (3 min)
```

---

## Testes

```bash
# Backend (JUnit 5)
cd backend && mvn test

# Frontend (Vitest)
cd frontend && npm test
```

Cobertura: algoritmo A* (`AStarWaypointOptimizerTest`), cálculo Haversine (`HaversineUtilTest`), hooks React (`useSseListener`, `useWaypoints`).

---

## Documentação

| Arquivo | Conteúdo |
|---------|----------|
| `REQUIREMENTS.md` | User stories e critérios de aceite |
| `docs/SYSTEM_REQUIREMENTS.md` | SRS completo (RF, RNF, rastreabilidade) |
| `docs/USE_CASES.md` | Especificação detalhada dos casos de uso |
| `docs/COST_AND_SCHEDULE.md` | Cronograma e análise de custos |
