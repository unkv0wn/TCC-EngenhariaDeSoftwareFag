# TODO — Roadmap do RouteWise

Sequência de construção e decisões já tomadas. O inventário do que existe hoje é o
`docs/ESTADO-ATUAL.md`; aqui ficam o que falta e o porquê das escolhas.

**Atualizado em:** 2026-10-24.

## Fluxo de negócio

Combinado no início, e é o que está implementado hoje:

```
Pedido (cliente + itens)
   → fica aguardando, se acumula com outros
   → vira faturado (pronto pra sair)
   → os faturados viram os waypoints de uma Rota
   → Rota é despachada num Veículo + Motorista
   → o motorista abre a rota no app e registra as entregas
   → a entrega fecha o pedido; o que não saiu volta pra fila de faturados
```

**O que mudou em relação ao plano original:** "Faturamento" era um módulo à parte, que agruparia
pedidos num lote faturado e o lote viraria a rota. Na prática ele virou **um status do pedido**
(`faturado`) mais a seleção na tela de gerar rota, que oferece exatamente os faturados. Um lote
intermediário não ganhava nada: a rota já é o agrupamento, e um pedido não é faturado duas vezes
porque a máquina de status não permite. Da mesma forma, "Despacho" não virou tela própria — a rota
nasce com `driverId` e `vehicleId`.

## Construído

1. **Cadastros** — todos ligados ao backend
   - [x] Veículos
   - [x] Unidades de Medida (virou pré-requisito de Produtos)
   - [x] Produtos
   - [x] Clientes/Fornecedores (uma tela, campo `type`)
   - [x] Motoristas
   - [x] Abastecimentos (litros/odômetro/preço, km rodado calculado do histórico)
   - [x] Formas e Condições de Pagamento
   - [x] Configurações da empresa (é a origem da rota)
2. **Pedidos**
   - [x] Novo pedido (cliente + itens)
   - [x] Máquina de status (`aguardando → faturado → em_rota → entregue`, + `cancelado`)
3. **Rotas**
   - [x] Gerar rota a partir dos pedidos faturados — mapa + A* + SSE migrados do protótipo
   - [x] Lista de rotas salvas
   - [x] Despacho (veículo + motorista na própria rota)
   - [x] Acompanhamento: mapa, paradas, log de eventos
   - [x] Compartilhar com o motorista por código legível + QR + deep link
4. **App do motorista** (`mobile/`, Flutter)
   - [x] Abrir rota por QR, deep link ou código digitado
   - [x] Registrar chegada/entrega/ocorrência, com fila offline por rota
   - [x] Encerrar a rota no fim do dia
   - [x] Login obrigatório com usuário, senha e aparelho cadastrado
   - [x] Tela inicial com as rotas do próprio motorista — ele começa o dia sem depender
     de o gestor lhe mandar nada
   - [x] Rota alheia recusada mesmo com o código na mão
5. **Integração**
   - [x] Ciclo do pedido fechado — a entrega em campo fecha o pedido
     (`RouteOrderLifecycle`)
   - [x] Data e hora da entrega no pedido (`delivered_at`), vinda do próprio evento de campo
   - [x] Pedido travado pra edição a partir de `faturado` — entregue é estado final
   - [x] API de integração para terceiros: credencial com segredo em BCrypt, token por
     client-credentials, escopo por caminho, três endpoints de leitura
     (pedidos, rotas, entregas) e registro de toda chamada
   - [x] Tela do gestor para criar/revogar credencial, gerar novo segredo e ler o registro
     de acesso (`/dashboard/integracoes`)
   - [x] Segredo do JWT fora do código, em `JWT_SECRET` (`.env` versionado só como exemplo)

## Em aberto

Em ordem de valor, na avaliação atual:

1. **Autenticação real no painel.** Hoje `/api/auth/login` emite token pra qualquer e-mail, sem
   senha, e todo `/api/**` fora de `/api/mobile/**` e `/api/integration/**` responde sem
   autenticação — inclusive `/api/api-clients`, que cria credencial de integração, e
   `/api/mobile-users`, que concede acesso ao app. A trava de aparelho do app tem uma porta aberta
   ao lado. Enquanto a 8080 só é alcançável na LAN, é problema contido; para o texto do TCC,
   "autenticação simplificada, fora do escopo" é posição defensável desde que **escrita**.
2. **Tela de registros de login do app.** Hoje só o último sucesso é guardado
   (`mobile_users.last_login_at`); toda recusa é descartada num `log.warn`. O gestor não distingue
   senha errada de aparelho não cadastrado de acesso bloqueado, e uma tentativa de entrar com
   celular não autorizado — o evento que o cadastro de aparelho existe pra impedir — não aparece em
   lugar nenhum. Desenho já discutido: tabela append-only com o desfecho de cada tentativa, FK
   anulável pra sobreviver à exclusão do acesso, e gravação em transação separada (senão a recusa
   lança e desfaz o próprio log). Esse desenho agora existe construído em outro lugar:
   `integration_request_log`, com a FK anulável e o `REQUIRES_NEW` — serve de molde.
3. **Mapa da rota no app** (`route_map_screen.dart`). Previsto no spec e não construído. A
   geometria já vem pronta no `result` da rota — é tela de leitura, não precisa de serviço novo. O
   item mais barato da lista.
4. **Testes de controller e repositório.** Os 68 testes do backend cobrem A*, as calculadoras
   empíricas e o ciclo do pedido. CRUD e máquinas de status — o que muda mais — não têm rede de
   proteção. Precisaria de Testcontainers ou Postgres de teste, que não estão montados.
5. **Modo escuro de verdade.** Nenhum componente usa `dark:`; o escuro só repinta o `body`.
   Cartões, modais e inputs têm classe clara fixa.
6. **Anexo de documento/foto.** Mock declarado nos dois lados. O bloqueio não é código, é decisão:
   onde armazenar (disco, S3, `bytea`), limite de tamanho, retenção. **Adiado por escolha —
   não é necessário por enquanto.**
7. **Máquina de status do pedido duplicada** entre `OrderServiceImpl` e `hooks/useOrders.ts`.

## Decisões tomadas

- Sequência: Cadastros → Pedidos → Rotas, de baixo pra cima; cada etapa é pré-requisito da
  próxima.
- Cada módulo passa por brainstorming (mockups + perguntas) antes de virar spec e implementação.
- Cliente e Fornecedor numa tela só (`/dashboard/clientes`), distinguidos por `type`
  (`cliente`/`fornecedor`/`ambos`) com filtro na listagem.
- Unidade de Medida é cadastro próprio, não enum em código; excluir unidade em uso por produto é
  bloqueado.
- CEP e CNPJ são consultados automaticamente (ViaCEP e BrasilAPI, gratuitas e sem chave) e
  preenchem o formulário; os campos continuam editáveis.
- Primitivas de UI compartilhadas moram em `components/ui/` — cada cadastro novo reusa em vez de
  duplicar overlay/cabeçalho/busca/estado vazio. Inventário no `CLAUDE.md`.
- Abastecimentos referencia Veículo e Motorista por id; excluir um com abastecimentos é bloqueado,
  mesmo padrão de Unidade↔Produto.
- `kmSincePrevious` de um abastecimento é calculado e congelado na criação — ver
  `docs/superpowers/specs/2026-08-13-drivers-and-refuelings-registration-design.md`.
- O app do motorista tem superfície própria (`/api/mobile/**`), separada do CRUD do gestor: é
  escopada a uma rota pelo código de acesso, então o celular nunca segura id interno de rota.
- A fila offline é **por rota**, não única. Com uma fila só, o evento pendente de ontem subia pra
  rota aberta hoje.
- O aparelho autorizado do app é o `Settings.Secure.ANDROID_ID`, **não o IMEI** — desde o Android
  10 o IMEI exige permissão privilegiada, inacessível a app de terceiro, e a política do Google
  Play proíbe usá-lo como identificador persistente. O app mostra o identificador na tela de login
  porque o Android não o exibe em tela nenhuma do sistema, e sem isso o gestor não conseguiria
  cadastrá-lo.
- Os pedidos saem de `faturado` na **criação** da rota, não quando o caminhão sai: a tela de gerar
  rota oferece justamente os faturados, e deixá-los lá permitiria o mesmo pedido entrar em duas
  rotas. Ver `RouteOrderLifecycle`.
- A integração é **só leitura**. Escrita externa — um ERP criando pedido — traz validação e
  idempotência (o que acontece quando o mesmo pedido chega duas vezes?) que merecem decisão
  própria, e não entraram.
- A integração usa client-credentials com token curto, não chave de API longa: chave vazada
  funciona até alguém notar, enquanto aqui o que viaja por chamada é um token que expira sozinho e
  revogar o cliente corta o próximo. O preço é que o escopo vai **dentro** do token e só muda na
  renovação — por isso a validade é curta.
- Caminho de integração sem escopo mapeado é recusado com 403, não liberado. Negar por omissão faz
  um endpoint novo que ninguém mapeou aparecer como defeito, em vez de servir dado sem escopo.
- O registro de acesso da integração é gravado em transação separada (`REQUIRES_NEW`): recusa é
  sinalizada por exceção, e o rollback apagaria justamente as linhas que importam.
- A integração tem DTOs próprios, não os do painel: o que sai é contrato prometido a terceiro, e
  reusar as formas internas tornaria cada refatoração uma quebra pra quem integrou.
- `frontend/` é protótipo congelado — não recebe feature nova.
