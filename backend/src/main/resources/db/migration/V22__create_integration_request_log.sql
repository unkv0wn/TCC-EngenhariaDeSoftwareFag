-- Registro de acesso da superfície de integração.
--
-- Existe pelo mesmo motivo que a tela de tentativas de login do app: um controle de
-- acesso que não mostra o que recusou é meio controle. Sem isto, uma credencial vazada
-- sendo usada por terceiros, ou um integrador batendo em escopo que não tem, não
-- apareceria em lugar nenhum que o gestor alcance — só no log de aplicação.
--
-- `api_client_id` é anulável e não tem ON DELETE CASCADE: uma tentativa com client_id
-- inexistente não tem dono, e o histórico precisa sobreviver à exclusão do cliente.
-- `client_id_tentado` guarda o texto enviado, que é o que identifica quem tentou quando
-- não há dono.
CREATE TABLE integration_request_log (
  id                UUID         PRIMARY KEY,
  api_client_id     UUID,
  client_id_tentado VARCHAR(40),
  method            VARCHAR(10)  NOT NULL,
  path              VARCHAR(200) NOT NULL,
  -- "sucesso" | "credencial_invalida" | "cliente_revogado" | "token_invalido"
  -- | "escopo_insuficiente"
  outcome           VARCHAR(30)  NOT NULL,
  status_code       INT          NOT NULL,
  occurred_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT fk_integration_log_client FOREIGN KEY (api_client_id)
    REFERENCES api_clients (id) ON DELETE SET NULL
);

-- A tela do gestor lê sempre "os mais recentes", e esta tabela é a única do projeto que
-- cresce por requisição.
CREATE INDEX idx_integration_log_occurred_at ON integration_request_log (occurred_at DESC);
