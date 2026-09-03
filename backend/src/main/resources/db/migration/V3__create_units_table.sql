-- Unidades de medida (catálogo referenciado pelos produtos).
CREATE TABLE units (
  id         UUID        PRIMARY KEY,
  code       VARCHAR(10) NOT NULL,
  name       VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_units_code UNIQUE (code),
  CONSTRAINT uq_units_name UNIQUE (name)
);

-- Mesmo raciocínio de ids fixos da V2 — ver comentário lá.
INSERT INTO units (id, code, name) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a2001', 'UN', 'Unidade'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a2002', 'KG', 'Quilo'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a2003', 'CX', 'Caixa'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a2004', 'L',  'Litro'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a2005', 'PL', 'Paletes');
