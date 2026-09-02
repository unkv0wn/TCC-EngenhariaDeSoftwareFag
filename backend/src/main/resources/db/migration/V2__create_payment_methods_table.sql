-- Formas de pagamento (catálogo referenciado pelos pedidos).
CREATE TABLE payment_methods (
  id         UUID         PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_payment_methods_name UNIQUE (name)
);

-- ids fixos (não gerados aleatoriamente aqui) só pra essa seed poder ser referenciada
-- de forma estável pelo seed mock do front-end (useOrders.ts) enquanto Pedidos ainda
-- não foi migrado pro backend real. Registros criados pela aplicação usam
-- UUID.randomUUID() de verdade (ver PaymentMethodServiceImpl).
INSERT INTO payment_methods (id, name) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a1001', 'Dinheiro'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', 'Pix'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a1003', 'Cartão de Crédito'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a1004', 'Cartão de Débito'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a1005', 'Boleto'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a1006', 'Transferência Bancária');
