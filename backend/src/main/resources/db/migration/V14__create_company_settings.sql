-- Configurações da empresa (linha única). Guarda o ponto de partida das rotas
-- (depósito/matriz) — antes era uma constante chumbada no front.
CREATE TABLE company_settings (
  id                UUID             PRIMARY KEY,
  company_name      VARCHAR(150)     NOT NULL,
  address_zip_code  VARCHAR(10),
  address_street    VARCHAR(150),
  address_number    VARCHAR(20),
  address_district  VARCHAR(100),
  address_city      VARCHAR(100),
  address_state     VARCHAR(2),
  latitude          DOUBLE PRECISION NOT NULL,
  longitude         DOUBLE PRECISION NOT NULL,
  updated_at        TIMESTAMPTZ      NOT NULL DEFAULT now()
);

-- Linha única — id fixo. Começa com a matriz que estava chumbada (São Paulo).
INSERT INTO company_settings (id, company_name, address_city, address_state, latitude, longitude)
VALUES ('00000000-0000-0000-0000-0000000c0001', 'Minha Empresa', 'São Paulo', 'SP', -23.5505, -46.6333);
