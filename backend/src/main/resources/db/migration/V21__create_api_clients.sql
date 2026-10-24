-- Credenciais de integração: quem, de fora, pode ler dados do RouteWise.
--
-- Separada de `mobile_users` porque são credenciais de naturezas diferentes: aquela é de
-- uma pessoa num aparelho conhecido, esta é de um sistema. O que elas compartilham é o
-- desenho — segredo guardado só como hash BCrypt, status que o gestor revoga, e registro
-- do último uso.
--
-- `client_secret` NÃO é guardado: só o hash. O valor em claro aparece uma vez, na tela
-- que o cria, e depois não existe mais em lugar nenhum — nem no banco, nem em log. Se o
-- integrador perder, o gestor gera outro.
CREATE TABLE api_clients (
  id           UUID         PRIMARY KEY,
  name         VARCHAR(120) NOT NULL,
  client_id    VARCHAR(40)  NOT NULL,
  secret_hash  VARCHAR(100) NOT NULL,
  -- Escopos separados por vírgula (ex.: "orders:read,routes:read"). Lista curta e
  -- fechada; uma tabela de junção aqui seria infraestrutura sem ganho.
  scopes       VARCHAR(255) NOT NULL,
  -- "ativo" | "revogado". Revogar não apaga: o histórico de acesso precisa continuar
  -- apontando para um cliente que existe.
  status       VARCHAR(10)  NOT NULL,
  last_used_at TIMESTAMPTZ,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_api_clients_client_id UNIQUE (client_id)
);
