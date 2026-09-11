# RouteWise — rodando com Docker

Sobe o sistema inteiro (banco + backend + frontend) com um comando só, sem precisar
instalar Java, Node ou Postgres na máquina.

## Pré-requisito

[Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e aberto.

## Como rodar

Na raiz do projeto (onde está este arquivo):

```bash
docker compose up --build
```

A primeira vez demora alguns minutos (baixa as imagens e compila tudo). Da segunda
vez em diante é bem mais rápido — pode rodar só `docker compose up`.

Quando aparecer o backend subindo (`Started RouteWiseApplication`), acessa:

**http://localhost:3000**

## Login

| Campo | Valor |
|---|---|
| E-mail | `teste@routewise.com` |
| Senha | `12345678` |

> É um login fixo de demonstração — o sistema ainda não tem cadastro de
> usuário/senha "de verdade" (isso não é um bug do Docker, é assim mesmo na versão
> atual do projeto).

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

## Portas usadas

| Serviço | Porta |
|---|---|
| Frontend (site) | 3000 |
| Backend (API) | 8080 |
| Postgres (só se quiser inspecionar o banco) | 5434 |

Se alguma dessas portas já estiver em uso na máquina, dá pra trocar no
`docker-compose.yml` (ex: `"3001:3000"` pro frontend).
