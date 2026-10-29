# RouteWise — rodando com Docker

Sobe o sistema inteiro (banco + backend + frontend) com um comando só, sem precisar
instalar Java, Node ou Postgres na máquina.

## Pré-requisito

[Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e aberto.

## Antes de subir: o arquivo `.env`

O compose exige a variável `JWT_SECRET` e **não sobe sem ela** — é o segredo que assina o
token do login, e ele não fica mais no código. Copia o modelo e gera um valor:

```bash
cp .env.example .env
```

```bash
node -e "console.log(require('crypto').randomBytes(48).toString('base64url'))"
```

Cola o valor gerado no `.env`, na linha `JWT_SECRET=`. O arquivo é ignorado pelo git, então
cada máquina tem o seu. Trocar o valor depois derruba os logins em aberto — é esperado.

## Como rodar

Na raiz do projeto (onde está este arquivo):

```bash
docker compose up -d
```

Só isso. A primeira vez demora alguns minutos — compila o backend com Maven e o
frontend com o Next; depois sobe em segundos, porque as imagens já existem.

O `-d` devolve o terminal e deixa a stack rodando. Para ver o que está acontecendo:

```bash
docker compose logs -f
```

Mudou código? Aí sim precisa reconstruir a imagem:

```bash
docker compose up -d --build
```

Quando os três serviços estiverem de pé, acessa:

**http://localhost:3000**

Não precisa esperar nem conferir ordem: o backend só sobe depois que o banco
responde, e o frontend só sobe depois que o backend atende na 8080. Sem isso a tela
abria antes de a API existir e o primeiro login falhava, o que parece defeito do
sistema e é só ordem de subida.

Os serviços têm `restart: unless-stopped`, então voltam sozinhos depois de reiniciar
a máquina — contanto que o Docker Desktop esteja aberto.

## Login

| Campo | Valor |
|---|---|
| E-mail | `teste@routewise.com` |
| Senha | `12345678` |

> É uma credencial de demonstração, fraca e pública de propósito: o repositório é
> aberto. A autenticação em si é real — senha com BCrypt na tabela `panel_users`,
> criada pela migration `V23`. Dá para trocar a senha e criar outros acessos em
> **Usuários**, e o sistema impede bloquear ou excluir o último acesso ativo.

## O que já vem pronto (sem precisar cadastrar nada)

O banco sobe zerado e o próprio backend aplica as migrations automaticamente
(Flyway) na primeira subida, incluindo os dados de exemplo:

- Motoristas, veículos, formas/condições de pagamento, unidades de medida
- **9 clientes em Toledo/PR** (e região), já com coordenada geográfica real
- Produtos e **20 pedidos**, a maioria já **faturada** — prontos pra virar rota
- Configuração inicial do depósito (pode editar em **Configurações**)

Fluxo rápido pra testar o ponto principal do sistema:

1. **Pedidos** → confere os pedidos faturados (focados em Toledo/PR)
2. **Rotas → Gerar rota** → seleciona alguns pedidos faturados → **Gerar rota**
   (isso chama o algoritmo de otimização de verdade, com mapa real)
3. Escolhe motorista/veículo/horário → **Salvar rota**
4. **Rotas** → o card da rota salva aparece lá, com **Acompanhar** mostrando o mapa

## Parar

```bash
docker compose down
```

Isso mantém os dados salvos (volume do banco). Pra zerar tudo e recomeçar do
zero (reaplica os seeds):

```bash
docker compose down -v
```

## Só o banco, com o backend rodando fora do container

Para desenvolver com `mvn spring-boot:run` e `pnpm dev`, sobe só o Postgres:

```bash
docker compose up -d db
```

O `backend/docker-compose.yml` faz a mesma coisa e aponta para o **mesmo** banco —
mesmo container, mesma porta, mesmo volume. Eram dois bancos distintos com o mesmo
nome de serviço na 5434, e subir pelo arquivo errado dava um sistema que ligava,
logava e aparecia vazio, sem nenhum erro indicando que o banco não era aquele.

## Portas usadas

| Serviço | Porta |
|---|---|
| Frontend (site) | 3000 |
| Backend (API) | 8080 |
| Postgres (só se quiser inspecionar o banco) | 5434 |

Se alguma dessas portas já estiver em uso na máquina, dá pra trocar no
`docker-compose.yml` (ex: `"3001:3000"` pro frontend).
