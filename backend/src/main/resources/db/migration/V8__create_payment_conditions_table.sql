-- Condições de pagamento (catálogo referenciado pelos pedidos).
CREATE TABLE payment_conditions (
  id             UUID         PRIMARY KEY,
  name           VARCHAR(100) NOT NULL,
  installments   INTEGER      NOT NULL,
  interval_days  INTEGER      NOT NULL,
  created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_payment_conditions_name UNIQUE (name)
);

-- ids fixos (não gerados aleatoriamente aqui) só pra essa seed poder ser referenciada
-- de forma estável pelo seed mock do front-end (useOrders.ts) enquanto Pedidos ainda
-- não foi migrado pro backend real. Registros criados pela aplicação usam
-- UUID.randomUUID() de verdade (ver PaymentConditionServiceImpl).
INSERT INTO payment_conditions (id, name, installments, interval_days) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', 'À vista', 1, 0),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a7002', '30 dias', 1, 30),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a7003', '30/60 dias', 2, 30),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a7004', '30/60/90 dias', 3, 30),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a7005', '3x sem juros', 3, 30);
