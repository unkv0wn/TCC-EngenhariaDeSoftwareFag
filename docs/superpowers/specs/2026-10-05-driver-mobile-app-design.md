# App Mobile do Motorista (Flutter) — Design Spec

Data: 2026-10-05
Escopo: `mobile/` (Flutter, novo) + `backend/` (superfície de API própria, por token)

## Contexto

A tela de acompanhamento (`docs/superpowers/specs/2026-09-21-route-tracking-screen-design.md`)
criou o log de eventos de rota e deixou o motorista como **autor de um evento registrado pelo
gestor** — porque o motorista não tem por onde entrar no sistema. Este app fecha esse ciclo: é
ele quem registra chegada, entrega e ocorrência, de onde o fato acontece.

É a **quarta peça** do repositório, ao lado de `backend/`, `FrontEnd-Next/` e `frontend/`.

## Decisões de escopo

- **Flutter, app nativo** — decisão do Jean. Não reaproveita nada do `FrontEnd-Next`: componente,
  hook e classe Tailwind não atravessam a fronteira. O que atravessa é o **contrato da API** e a
  **paleta** (os tokens de `globals.css` viram um `ThemeData`). Assumir que isso é "o mesmo
  front em outra tela" é o erro mais caro possível aqui.
- **Só o motorista.** O gestor continua no `FrontEnd-Next`. Nada de cadastro, pedido ou geração
  de rota no app — ele consome uma rota já despachada.
- **Acesso por token de rota, sem login.** O gestor manda o link pelo WhatsApp (o botão "Enviar"
  do card já faz isso); o link abre o app por deep link e carrega a rota. Sem tabela de usuários,
  sem senha — e sem tocar na autenticação decorativa que existe hoje.
- **Superfície de API separada, `/api/mobile/**`.** O app **não** consome o CRUD do gestor. Ele
  enxerga uma rota só, pelo token, e só pode fazer o que um motorista faz. Reaproveitar
  `/api/routes/{id}` entregaria ao celular o id interno e, com ele, o caminho pro resto da API
  (que está aberta — ver "Authentication" no `CLAUDE.md`).
- **Offline é requisito, não enfeite.** Entrega acontece em galpão, subsolo e estrada. Evento
  registrado sem rede entra numa fila local e sobe quando volta. Sem isso o app é inútil
  exatamente quando mais importa.
- **Câmera fica para depois.** O anexo de canhoto/foto continua mock, igual na tela do gestor,
  pelo mesmo motivo: não há decisão sobre armazenamento. A diferença é que aqui o botão já
  nasce no lugar certo do fluxo, pronto pra ligar.

## Parte 1 — Backend: acesso por token

### Migration — `V16__add_route_access_token.sql`

```sql
ALTER TABLE routes ADD COLUMN access_token VARCHAR(43);
CREATE UNIQUE INDEX idx_routes_access_token ON routes (access_token);
```

43 caracteres = 32 bytes aleatórios em base64url, gerados com `SecureRandom` quando a rota é
salva. Nulo nas rotas antigas — elas ganham token na primeira vez que forem compartilhadas.

**O token é um segredo portador.** Quem tem o link tem a rota. Isso é aceitável aqui (o link vai
pro WhatsApp do motorista escalado, e expõe uma rota, não o sistema), mas precisa estar escrito:
não é autenticação, é uma URL não-adivinhável. Se vazar, o gestor regenera.

### Endpoints — `MobileRouteController` (`/api/mobile/routes/{token}`)

| Método | Caminho | Retorno |
|---|---|---|
| `GET` | `/api/mobile/routes/{token}` | a rota: paradas na ordem otimizada, motorista, veículo, geometria e eventos já registrados |
| `POST` | `/api/mobile/routes/{token}/events` | registra evento; `authorRole` é sempre `motorista`, forçado no servidor |
| `POST` | `/api/mobile/routes/{token}/events/batch` | sincroniza a fila offline de uma vez |

Token desconhecido → **404**, nunca 403: um 403 confirmaria que o token existe.

O `batch` recebe uma lista e devolve o resultado item a item — um evento rejeitado não pode
derrubar os outros, senão o app perde registros que o motorista fez em campo.

### Idempotência da fila offline

Cada evento carrega um `clientEventId` (UUID gerado no celular). O backend guarda numa coluna
única e ignora repetição. Sem isso, perder a resposta de rede num túnel faz o motorista
reenviar e duplicar a entrega — e o progresso da rota passa a mentir.

```sql
ALTER TABLE route_events ADD COLUMN client_event_id UUID;
CREATE UNIQUE INDEX idx_route_events_client_id ON route_events (client_event_id);
```

## Parte 2 — App (`mobile/`)

### Estrutura

```
mobile/
  lib/
    main.dart
    theme/app_theme.dart          # tokens de globals.css -> ThemeData
    models/                       # Route, Stop, RouteEvent (espelham os DTOs)
    services/api_client.dart      # cliente HTTP da superfície /api/mobile
    services/event_queue.dart     # fila offline (sqflite) + sincronização
    screens/
      open_route_screen.dart      # deep link ou código colado
      route_summary_screen.dart   # resumo + lista de paradas
      stop_detail_screen.dart     # a tela onde o motorista passa o dia
      route_map_screen.dart       # mapa da rota inteira
      event_form_sheet.dart       # bottom sheet de registro
    widgets/
```

### Telas

**1. Abrir rota** — primeira abertura. Deep link `routewise://rota/<token>` resolve direto; sem
ele, um campo pra colar o código. O token fica guardado, então o app abre direto na rota do dia
nas vezes seguintes.

**2. Resumo da rota** — saída, motorista, veículo + capacidade, total de paradas, distância e
tempo estimados. Barra de progresso ("3 de 9 entregues"). Botão grande: **"Iniciar rota"**
(registra o evento `saida`).

**3. Lista de paradas** — cada item com o número da parada (o mesmo do mapa do gestor), nome do
cliente, bairro, quantidade de pedidos e estado (pendente / entregue). A próxima parada vem
destacada no topo.

**4. Detalhe da parada** — a tela principal. Cliente, endereço completo, telefone (toque liga),
lista de pedidos com valores. Três ações, em botões grandes:

```
┌─────────────────────────────┐
│  3  Padaria Trigo Dourado   │
│  Rua Guaíra, 560 — Centro   │
│  2 pedidos · R$ 487,00      │
├─────────────────────────────┤
│  [ Navegar até aqui ]       │  abre Google Maps / Waze
│  [ Cheguei ]                │  evento `chegada`
│  [ Entreguei ]              │  evento `entrega`
│  [ Registrar ocorrência ]   │  evento `ocorrencia` (exige observação)
│  [ Anexar canhoto ]         │  MOCK
└─────────────────────────────┘
```

**5. Mapa** — a rota inteira com as paradas numeradas, mesma geometria que o gestor vê (vem
pronta no `result` da rota). Leitura, não navegação — navegação é delegada ao app de mapas do
celular, que o motorista já conhece e que tem trânsito em tempo real.

### Regras de interface

- Alvo de toque mínimo **48dp**; ações principais com largura total. O motorista usa de pé,
  com uma mão, às vezes de luva.
- Confirmação em **"Entreguei"** e em qualquer ação destrutiva — é o toque que muda o estado da
  rota, e desfazer significa pedir pro gestor apagar o evento.
- Estado de sincronização sempre visível: um selo "N eventos pendentes" quando a fila não está
  vazia. O motorista precisa saber que o registro ainda não subiu.
- Tema claro apenas, por ora — mesmo recorte do `FrontEnd-Next`, que também só tem claro de
  verdade.

### Paleta

`theme/app_theme.dart` replica os tokens de `FrontEnd-Next/app/globals.css` — `primary` roxo
(`#8b5cf6`/`#7c3aed`), `success` verde, `danger` vermelho, `warning` amarelo, escala de cinza.
São dois arquivos com a mesma verdade em linguagens diferentes: mudar a marca exige mudar os
dois, e o `CLAUDE.md` precisa dizer isso.

## Fora de escopo

- GPS/telemetria contínua. O app registra eventos pontuais, não rastreia o veículo.
- Upload real de foto/canhoto.
- Push notification (gestor avisando mudança de rota).
- iOS na primeira entrega — Android primeiro, que é o aparelho da frota.
- Login de motorista. Se um dia existir, o token vira detalhe de implementação do deep link.

## Ordem de construção sugerida

1. Backend: token + `/api/mobile/**` + idempotência (dá pra testar com `curl`, sem app nenhum).
2. Flutter: esqueleto, tema, cliente de API, abrir rota por código colado.
3. Resumo + lista de paradas + detalhe da parada com os três eventos.
4. Fila offline e selo de sincronização.
5. Deep link e o link do WhatsApp apontando pro app.
6. Mapa.
