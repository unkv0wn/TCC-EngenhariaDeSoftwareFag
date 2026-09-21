# RouteWise — Otimizador de Rotas Urbanas

Sistema web de gestão de entregas com otimização de rotas. O núcleo calcula a ordem ótima de visita para múltiplos pontos usando A*, com a matriz de custo vinda do OSRM e o progresso transmitido em tempo real por SSE. Em volta desse núcleo existe um painel com cadastros (veículos, motoristas, produtos, clientes, …), pedidos e rotas geradas a partir desses pedidos.

> Trabalho de Conclusão de Curso — Engenharia de Software

O `FrontEnd-Next/` (Next.js) é o frontend oficial, o que vai para produção, e já consome a API real do backend. O `frontend/` (React + Vite) é o protótipo de testes que validou o núcleo de cálculo de rotas ponta a ponta — está congelado e não é a versão entregue.

---

## Tecnologias

| Camada | Stack |
|--------|-------|
| Backend | Java 21 (Virtual Threads) + Spring Boot 3.2.5 + Maven |
| Banco | PostgreSQL 16 + Spring Data JPA + Flyway |
| Frontend (produção, `FrontEnd-Next/`) | Next.js 16 + React 19 + TypeScript + Tailwind CSS 4 + Leaflet |
| Frontend (testes, `frontend/`) | React 18 + TypeScript + Vite + Leaflet |
| Roteamento | OSRM (Open Source Routing Machine) — API pública |
| Comunicação | REST + SSE (Server-Sent Events) |
| Testes | JUnit 5 (backend) + Vitest (protótipo) |

---

## Funcionalidades

**Núcleo de rotas**

- Otimização da ordem de visita com A* sobre a matriz de duração do OSRM
- Três modos de rota: `ROUND_TRIP`, `OPEN_ROUTE`, `FIXED_START_END`
- Geometria e métricas da rota (duração, distância, traçado) via OSRM
- Streaming de progresso em tempo real por SSE
- Degradação graciosa em dois níveis: sem o `/table` do OSRM a matriz é estimada por Haversine a 40 km/h; sem o `/route`, a ordem otimizada é devolvida sem o traçado

**Painel**

- Cadastros: veículos, motoristas, abastecimentos, unidades de medida, produtos, clientes/fornecedores, formas e condições de pagamento
- Pedidos com itens, histórico e transições de status validadas
- Geração de rota a partir dos pedidos selecionados, com mapa interativo e acompanhamento do cálculo
- Histórico de rotas salvas
- Configurações da empresa (endereço/coordenada usados como ponto de partida das rotas)
- Preenchimento automático de CEP (ViaCEP), CNPJ (BrasilAPI), marcas/modelos de veículo (FIPE) e geocodificação de endereço (Nominatim)

**Autenticação** — existe uma tela de login que gera um JWT e protege a navegação do painel, mas o backend **não valida token em nenhum endpoint** e não há cadastro de usuários. Ver [Autenticação](#autenticação) abaixo.

---

## Estrutura do Projeto

```
TCC/
├── backend/                         # Spring Boot
│   └── src/main/java/com/routewise/
│       ├── algorithm/               # A* + Haversine
│       ├── auth/                    # JwtService (geração de token)
│       ├── config/                  # CORS, RestTemplate
│       ├── controller/              # 13 controllers REST (+ SSE)
│       ├── dto/                     # DTOs de entrada/saída (+ dto/osrm)
│       ├── entity/                  # 13 entidades JPA
│       ├── exception/               # GlobalExceptionHandler + exceções de domínio
│       ├── model/                   # RouteMode enum
│       ├── repository/              # 11 repositórios Spring Data
│       ├── service/                 # Interfaces I* e implementações (service/impl, service/osm)
│       └── validation/              # Experimentos standalone de validação empírica (TCC)
│   └── src/main/resources/db/migration/   # V1..V14 — Flyway é dono do schema
├── FrontEnd-Next/                   # Next.js (frontend oficial, produção)
│   ├── app/                         # App Router: login, dashboard e subrotas, /api/login
│   ├── components/                  # um diretório por módulo + ui/ (primitivas compartilhadas)
│   ├── hooks/                       # use<Recurso> — lista + loading + erro + CRUD
│   ├── lib/                         # apiClient, validações (zod), formatação, cálculos
│   ├── services/                    # chamadas HTTP por recurso + APIs públicas de terceiros
│   └── proxy.ts                     # guarda de /dashboard/* (verifica o JWT do cookie)
├── frontend/                        # React + Vite (protótipo congelado, não vai para produção)
├── docs/                            # SRS, requisitos, casos de uso, cronograma
│   ├── superpowers/specs/           # design specs por feature
│   ├── superpowers/plans/           # planos de implementação
│   └── validation-reports/          # relatórios gerados pelos experimentos de validation/
├── scripts/                         # utilitários PowerShell / GeoJSON de exemplo
└── docker-compose.yml               # stack completa (banco + backend + frontend)
```

---

## Pré-requisitos

- Java 21 LTS
- Apache Maven 3.9+
- Node.js 20+ e pnpm
- Docker (para o Postgres local, ou para subir a stack inteira)

Acesso à API pública do OSRM (`router.project-osrm.org`) — sem chave, sujeito a rate limit.

---

## Como Executar

### Tudo de uma vez (Docker)

```bash
docker compose up --build
```

Sobe banco, backend e frontend juntos. Detalhes em [`README-DOCKER.md`](README-DOCKER.md).

### Backend

```bash
docker compose -f backend/docker-compose.yml up -d   # Postgres em :5434
cd backend
mvn clean install        # compila e roda os testes
mvn spring-boot:run      # sobe na porta 8080
```

O banco precisa estar de pé: o Flyway aplica as migrations no start e o Hibernate valida as entidades contra o schema (`ddl-auto=validate`).

### Frontend de produção (`FrontEnd-Next/`)

```bash
cd FrontEnd-Next
pnpm install
pnpm dev                 # sobe em http://localhost:3000
```

Precisa de um `.env.local` com:

```
NEXT_PUBLIC_API_URL=http://localhost:8080
BACKEND_URL=http://localhost:8080
JWT_SECRET=<mesmo valor de routewise.jwt.secret no backend>
```

Com o backend rodando, acesse `http://localhost:3000` — o login redireciona para o dashboard.

### Frontend de testes (`frontend/`)

```bash
cd frontend
npm install
npm run dev              # http://localhost:5173 (proxy /api → :8080)
```

Usado apenas para validar o núcleo de rotas. Não é a versão entregue.

### Build de Produção

```bash
cd FrontEnd-Next && pnpm build

cd backend && mvn package      # gera target/routewise-backend-*.jar
java -jar target/routewise-backend-*.jar
```

---

## Endpoints da API

Todos sob `/api`. Os cadastros seguem o mesmo formato: `GET` (lista), `POST` (cria), `PUT /{id}`, `DELETE /{id}`.

| Recurso | Caminho base | Extras |
|---------|--------------|--------|
| Login | `/api/auth/login` | `POST` apenas |
| Veículos, Motoristas, Unidades, Produtos, Clientes, Abastecimentos, Formas de pagamento, Condições de pagamento | `/api/vehicles`, `/api/drivers`, `/api/units`, `/api/products`, `/api/customers`, `/api/refuelings`, `/api/payment-methods`, `/api/payment-conditions` | CRUD |
| Pedidos | `/api/orders` | CRUD + `PATCH /{id}/status` |
| Rotas salvas | `/api/routes` | `GET`, `GET /{id}`, `POST`, `PATCH /{id}/status`, `DELETE /{id}` |
| Cálculo de rota | `/api/routes/compute` | `POST` — devolve `requestId` |
| Stream de progresso | `/api/routes/events/{requestId}` | `GET` — SSE |
| Estatísticas do grafo OSM | `/api/routes/graph-stats` | `GET` |
| Configurações da empresa | `/api/settings` | `GET` + `PUT` |

Erros seguem RFC 9457 (problem details), produzidos pelo `GlobalExceptionHandler`.

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
- **Custo:** duração de deslocamento em segundos (matriz `/table` do OSRM)
- **Heurística:** soma da menor aresta de entrada de cada waypoint ainda não visitado, mais — no modo `ROUND_TRIP` — um limite inferior para o retorno à origem. É admissível porque todo nó não visitado precisa ser alcançado ao menos uma vez, e a entrada mais barata possível é justamente esse mínimo.
- **Desempate:** entre estados com o mesmo `f = g + h`, expande primeiro o de maior `g`. Não muda o custo ótimo encontrado, só reduz quantos estados são expandidos até chegar nele.

A complexidade temporal é `O(n² · 2ⁿ)`. O limite validado é de 2 a 15 waypoints — em 15 são cerca de 490 mil estados, ainda abaixo de um segundo.

---

## Configurações Relevantes

`backend/src/main/resources/application.properties`:

```properties
server.port=8080
spring.threads.virtual.enabled=true          # Java 21 Virtual Threads
routewise.osrm.base-url=https://router.project-osrm.org
routewise.osrm.timeout-ms=10000
routewise.osm.data-path=                     # vazio: grafo OSM desligado (só alimenta /graph-stats)
routewise.sse.timeout-ms=180000              # TTL do stream SSE (3 min)
spring.jpa.hibernate.ddl-auto=validate       # Flyway é dono do schema
```

Conexão com o banco via `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`, com padrão apontando para o compose local (`localhost:5434`).

---

## Autenticação

O fluxo atual é parcial e vale conhecer antes de confiar nele:

- `POST /api/auth/login` emite um JWT para **qualquer** e-mail recebido — não há verificação de senha nem tabela de usuários no backend.
- A conferência de credenciais acontece no Next, em `app/api/login/route.ts`, contra um par fixo em código. O token volta num cookie `httpOnly` chamado `routewise`.
- `proxy.ts` (o middleware do Next 16) protege `/dashboard/*`, validando assinatura e expiração do cookie.
- O backend **não tem Spring Security nem filtro de JWT**: todos os endpoints `/api/**` respondem sem autenticação, e o token nunca é enviado nas chamadas de CRUD.

Ou seja: a proteção existente é de navegação, não de API. Fechar isso (usuários no banco + filtro JWT no backend) é um passo em aberto.

---

## Testes

```bash
# Backend (JUnit 5)
cd backend && mvn test

# Protótipo (Vitest)
cd frontend && npx vitest run
```

A cobertura hoje é do núcleo matemático: A* (`AStarWaypointOptimizerTest`, `AStarTieBreakBenchmarkTest`), Haversine (`HaversineUtilTest`) e os cálculos empíricos de combustível, pneus e ocupação de carga. Não há testes de controller, service ou repositório, e o `FrontEnd-Next/` ainda não tem suíte configurada — os testes de hooks (`useWaypoints`, `useSseListener`) estão no protótipo congelado.

---

## Documentação

| Arquivo | Conteúdo |
|---------|----------|
| `docs/REQUIREMENTS.md` | User stories e critérios de aceite |
| `docs/SYSTEM_REQUIREMENTS.md` | SRS completo (RF, RNF, rastreabilidade) |
| `docs/USE_CASES.md` | Especificação detalhada dos casos de uso |
| `docs/COST_AND_SCHEDULE.md` | Cronograma e análise de custos |
| `docs/ESTADO-ATUAL.md` | Retrato do estado atual da construção |
| `docs/formulas-a-star-e-custo.md` | Formulação matemática do A* e do modelo de custo |
| `docs/superpowers/specs/` e `docs/superpowers/plans/` | Design specs e planos por feature |
| `docs/validation-reports/` | Relatórios gerados pelos experimentos de `com.routewise.validation` |
| `TODO.md` | Roadmap dos módulos e decisões já tomadas |
| `README-DOCKER.md` | Como rodar a stack completa com Docker |
