-- Motoristas da frota.
CREATE TABLE drivers (
  id           UUID         PRIMARY KEY,
  full_name    VARCHAR(150) NOT NULL,
  cpf          VARCHAR(14)  NOT NULL,
  phone        VARCHAR(20)  NOT NULL,
  cnh_number   VARCHAR(11)  NOT NULL,
  cnh_category VARCHAR(2)   NOT NULL,
  cnh_validity DATE         NOT NULL,
  status       VARCHAR(10)  NOT NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_drivers_cpf UNIQUE (cpf),
  CONSTRAINT uq_drivers_cnh_number UNIQUE (cnh_number)
);

INSERT INTO drivers (id, full_name, cpf, phone, cnh_number, cnh_category, cnh_validity, status) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', 'Carlos Eduardo Santos',    '123.456.789-09', '(45) 99911-2233', '12345678901', 'E', '2027-03-15', 'ativo'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', 'Marcos Vinícius Oliveira', '987.654.321-00', '(45) 98877-6655', '10987654321', 'D', '2025-11-02', 'ativo'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a3003', 'Roberto da Silva',         '111.222.333-92', '(45) 99123-4567', '11223344556', 'C', '2026-06-30', 'inativo');
