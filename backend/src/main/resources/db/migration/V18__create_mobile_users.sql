-- Credencial do app do motorista, com o aparelho autorizado a usá-la.
--
-- Separada de `drivers` porque são coisas de ciclo de vida diferente: o motorista é
-- cadastro da frota e existe mesmo sem app; o acesso é concedido, bloqueado e revogado
-- sem mexer no cadastro dele.
--
-- `device_id` é o Settings.Secure.ANDROID_ID do celular, cadastrado pelo gestor a
-- partir do que o app mostra na tela de login. NÃO é o IMEI: desde o Android 10 ler o
-- IMEI exige permissão privilegiada, inacessível a app de terceiro. Fica nulo enquanto
-- o gestor não tiver o identificador em mãos, e login com ele nulo é recusado — assim
-- nenhum aparelho entra antes de ser cadastrado.
CREATE TABLE mobile_users (
  id            UUID         PRIMARY KEY,
  driver_id     UUID         NOT NULL,
  username      VARCHAR(60)  NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  device_id     VARCHAR(64),
  device_label  VARCHAR(80),
  status        VARCHAR(10)  NOT NULL,
  last_login_at TIMESTAMPTZ,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_mobile_users_username  UNIQUE (username),
  CONSTRAINT uq_mobile_users_driver    UNIQUE (driver_id),
  -- Um aparelho serve a um acesso só: sem isto, dois motoristas poderiam vincular o
  -- mesmo celular e o registro de campo deixaria de dizer quem esteve lá.
  CONSTRAINT uq_mobile_users_device_id UNIQUE (device_id),
  CONSTRAINT fk_mobile_users_driver FOREIGN KEY (driver_id) REFERENCES drivers (id)
);
