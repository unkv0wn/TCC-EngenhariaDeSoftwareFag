-- Veículos da frota.
CREATE TABLE vehicles (
  id          UUID         PRIMARY KEY,
  plate       VARCHAR(10)  NOT NULL,
  model       VARCHAR(100) NOT NULL,
  brand       VARCHAR(100) NOT NULL,
  year        INTEGER      NOT NULL,
  color       VARCHAR(50)  NOT NULL,
  capacity_kg DOUBLE PRECISION NOT NULL,
  fuel_type   VARCHAR(10)  NOT NULL,
  created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_vehicles_plate UNIQUE (plate)
);

INSERT INTO vehicles (id, plate, model, brand, year, color, capacity_kg, fuel_type) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', 'ABC-1234', 'Sprinter', 'Mercedes-Benz', 2021, 'Branco', 1200, 'diesel'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', 'XYZ9F45',  'HR',       'Hyundai',       2019, 'Prata',  900,  'gasolina'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', 'JJK4C11',  'Daily',    'Iveco',         2022, 'Branco', 1500, 'diesel'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a4004', 'QWE7B88',  'Onix',     'Chevrolet',     2023, 'Prata',  450,  'etanol');
