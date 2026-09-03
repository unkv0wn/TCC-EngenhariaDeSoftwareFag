-- Abastecimentos. Primeira tabela com FK de verdade pra outras entidades já migradas
-- (vehicles, drivers) — km_since_previous NÃO existe aqui de propósito: é sempre derivado
-- no front a partir do histórico de odômetro do veículo, nunca armazenado (evita ficar
-- desatualizado se um registro anterior for editado depois).
CREATE TABLE refuelings (
  id                UUID           PRIMARY KEY,
  vehicle_id        UUID           NOT NULL REFERENCES vehicles(id),
  driver_id         UUID           NOT NULL REFERENCES drivers(id),
  date              DATE           NOT NULL,
  odometer_km       DOUBLE PRECISION NOT NULL,
  liters_refueled   DOUBLE PRECISION NOT NULL,
  price_per_liter   DOUBLE PRECISION NOT NULL,
  created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

INSERT INTO refuelings (id, vehicle_id, driver_id, date, odometer_km, liters_refueled, price_per_liter) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a6001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '2026-07-01', 15000, 45, 6.10),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a6002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '2026-07-20', 15650, 48, 6.15),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a6003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '2026-07-10', 8200,  38, 5.95),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a6004', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3003', '2026-07-25', 22300, 52, 6.10);
