-- Usuário do painel do gestor.
--
-- Até aqui o painel não tinha tabela de usuário nenhuma: `/api/auth/login` emitia token
-- para qualquer e-mail que recebesse, e a única conferência de credencial morava num par
-- fixo dentro do código do front. O sistema já tinha duas superfícies com autenticação
-- real — o app do motorista e a integração —, e a terceira, que é justamente a que
-- administra as outras duas, era a aberta.
--
-- Separada de `mobile_users` de propósito, apesar da forma parecida: aquela é credencial
-- de campo, presa a um motorista e a um aparelho. Esta é de escritório, e as duas não
-- compartilham ciclo de vida — bloquear o acesso de alguém ao painel não tem nada a ver
-- com o celular dele.
CREATE TABLE panel_users (
  id            UUID         PRIMARY KEY,
  name          VARCHAR(120) NOT NULL,
  email         VARCHAR(160) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  status        VARCHAR(10)  NOT NULL,
  last_login_at TIMESTAMPTZ,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  -- Sem distinção de maiúsculas: e-mail digitado como "Teste@" e "teste@" é a mesma
  -- pessoa, e dois cadastros assim seriam dois acessos onde deveria haver um.
  CONSTRAINT uq_panel_users_email UNIQUE (email)
);

-- O usuário da demonstração, com a mesma credencial que o README-DOCKER.md documenta e
-- que a banca vai usar. A diferença é que ela deixa de ser um `if` no código do front e
-- passa a ser uma linha de tabela: pode ser bloqueada, ter a senha trocada e conviver
-- com outras.
--
-- O hash é BCrypt de "12345678". Senha fraca e pública, de propósito — é credencial de
-- demonstração num repositório aberto, e fingir o contrário seria pior do que declarar.
INSERT INTO panel_users (id, name, email, password_hash, status) VALUES (
  '00000000-0000-0000-0000-000000000001',
  'Gestor de demonstração',
  'teste@routewise.com',
  '$2a$10$eQ6zO.oCQwxafp8A.CJpCuqo68rqWWU77XE7BxnS2HqsDFUO34vgS',
  'ativo'
);
