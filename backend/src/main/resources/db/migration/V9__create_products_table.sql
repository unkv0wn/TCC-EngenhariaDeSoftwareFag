-- Produtos (catálogo referenciado pelos itens de pedido).
CREATE TABLE products (
  id          UUID             PRIMARY KEY,
  sku         VARCHAR(30)      NOT NULL,
  name        VARCHAR(150)     NOT NULL,
  description VARCHAR(200),
  unit_id     UUID             NOT NULL REFERENCES units(id),
  unit_price  DOUBLE PRECISION NOT NULL,
  weight_kg   DOUBLE PRECISION NOT NULL,
  created_at  TIMESTAMPTZ      NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ      NOT NULL DEFAULT now(),
  CONSTRAINT uq_products_sku UNIQUE (sku)
);

-- ids fixos (não gerados aleatoriamente aqui) só pra essa seed poder ser referenciada
-- de forma estável pelo seed mock do front-end (useOrders.ts) enquanto Pedidos ainda
-- não foi migrado pro backend real. Registros criados pela aplicação usam
-- UUID.randomUUID() de verdade (ver ProductServiceImpl).
INSERT INTO products (id, sku, name, description, unit_id, unit_price, weight_kg) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8001', 'CX-001', 'Água Mineral 500ml (caixa c/ 12)',
    'Caixa com 12 garrafas de água mineral sem gás.', '8f14e45f-ceea-467e-b3a1-9d2e5c0a2003', 18.90, 12),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8002', 'REF-002', 'Refrigerante Cola 2L (caixa c/ 6)',
    'Caixa com 6 garrafas de refrigerante sabor cola.', '8f14e45f-ceea-467e-b3a1-9d2e5c0a2003', 42.50, 14.4),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8003', 'ARR-003', 'Arroz Tipo 1 5kg',
    'Saco de arroz branco tipo 1.', '8f14e45f-ceea-467e-b3a1-9d2e5c0a2001', 24.90, 5),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8004', 'OL-004', 'Óleo de Soja 900ml (caixa c/ 12)',
    NULL, '8f14e45f-ceea-467e-b3a1-9d2e5c0a2003', 65.00, 10.8),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8005', 'DET-005', 'Detergente Líquido 500ml',
    NULL, '8f14e45f-ceea-467e-b3a1-9d2e5c0a2004', 2.50, 0.5);
