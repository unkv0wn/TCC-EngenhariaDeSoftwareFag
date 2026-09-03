-- Pedidos — referencia todos os outros cadastros já migrados.
CREATE TABLE orders (
  id                    UUID             PRIMARY KEY,
  customer_id           UUID             NOT NULL REFERENCES customers(id),
  vehicle_id            UUID             NOT NULL REFERENCES vehicles(id),
  driver_id             UUID             NOT NULL REFERENCES drivers(id),
  payment_method_id     UUID             NOT NULL REFERENCES payment_methods(id),
  payment_condition_id  UUID             NOT NULL REFERENCES payment_conditions(id),
  date                  DATE             NOT NULL,
  status                VARCHAR(20)      NOT NULL,
  discount              DOUBLE PRECISION NOT NULL DEFAULT 0,
  shipping_cost         DOUBLE PRECISION NOT NULL DEFAULT 0,
  notes                 VARCHAR(500),
  created_at            TIMESTAMPTZ      NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ      NOT NULL DEFAULT now()
);

-- Itens do pedido — item_order preserva a ordem em que foram cadastrados no formulário.
CREATE TABLE order_items (
  order_id    UUID             NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  item_order  INTEGER          NOT NULL,
  product_id  UUID             NOT NULL REFERENCES products(id),
  quantity    INTEGER          NOT NULL,
  unit_price  DOUBLE PRECISION NOT NULL,
  PRIMARY KEY (order_id, item_order)
);

-- Linha do tempo de status do pedido (ver comentário em Order.java / useOrders.ts sobre a
-- máquina de estados). Sem chave natural — cada mudança de status é só um fato imutável.
CREATE TABLE order_status_history (
  id          BIGSERIAL   PRIMARY KEY,
  order_id    UUID        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  status      VARCHAR(20) NOT NULL,
  changed_at  TIMESTAMPTZ NOT NULL
);

-- ids fixos (não gerados aleatoriamente aqui) só pra essa seed reproduzir os pedidos que
-- existiam no mock do front-end (useOrders.ts) antes da migração. Registros criados pela
-- aplicação usam UUID.randomUUID() de verdade (ver OrderServiceImpl).
INSERT INTO orders (id, customer_id, vehicle_id, driver_id, payment_method_id, payment_condition_id, date, status, discount, shipping_cost, notes) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-08-20', 'entregue', 10, 25, 'Entregar na portaria dos fundos.'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1005', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7003', '2026-08-24', 'em_rota', 0, 0, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7005', '2026-08-27', 'aguardando', 0, 15, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9004', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-08-28', 'faturado', 0, 20, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9005', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1005', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7003', '2026-08-28', 'faturado', 5, 18, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9006', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7005', '2026-08-29', 'faturado', 0, 0, NULL);

INSERT INTO order_items (order_id, item_order, product_id, quantity, unit_price) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9001', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8001', 10, 18.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9001', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8003', 5, 24.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9002', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8002', 20, 42.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9003', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8004', 8, 65.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9003', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8005', 15, 2.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9004', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8001', 12, 18.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9005', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8003', 6, 24.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9006', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8002', 10, 42.50);

INSERT INTO order_status_history (order_id, status, changed_at) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9001', 'aguardando', '2026-08-18T09:12:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9001', 'faturado',   '2026-08-19T13:40:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9001', 'em_rota',    '2026-08-20T08:03:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9001', 'entregue',   '2026-08-20T14:47:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9002', 'aguardando', '2026-08-22T11:30:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9002', 'faturado',   '2026-08-23T10:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9002', 'em_rota',    '2026-08-24T07:55:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9003', 'aguardando', '2026-08-26T16:20:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9004', 'aguardando', '2026-08-27T10:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9004', 'faturado',   '2026-08-28T09:15:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9005', 'aguardando', '2026-08-27T14:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9005', 'faturado',   '2026-08-28T11:30:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9006', 'aguardando', '2026-08-28T08:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9006', 'faturado',   '2026-08-29T08:45:00Z');
