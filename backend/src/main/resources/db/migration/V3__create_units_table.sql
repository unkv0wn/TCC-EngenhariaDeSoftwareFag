-- Unidades de medida (catálogo referenciado pelos produtos).
-- Mesmo raciocínio de id "legível" da V2 — ver comentário lá.
CREATE TABLE units (
  id         VARCHAR(40) PRIMARY KEY,
  code       VARCHAR(10) NOT NULL,
  name       VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_units_code UNIQUE (code),
  CONSTRAINT uq_units_name UNIQUE (name)
);

INSERT INTO units (id, code, name) VALUES
  ('un', 'UN', 'Unidade'),
  ('kg', 'KG', 'Quilo'),
  ('cx', 'CX', 'Caixa'),
  ('l',  'L',  'Litro'),
  ('pl', 'PL', 'Paletes');
