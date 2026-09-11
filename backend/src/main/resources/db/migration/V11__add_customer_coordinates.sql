-- Coordenadas geográficas do cliente, usadas como waypoint na otimização de rota.
-- Nullable: clientes antigos e novos cadastros sem geocoding ficam sem coordenada
-- (a Gerar Rota cai pro fallback cidade + jitter nesse caso).
ALTER TABLE customers
  ADD COLUMN address_latitude  DOUBLE PRECISION,
  ADD COLUMN address_longitude DOUBLE PRECISION;

-- Backfill dos clientes seed com a coordenada do CEP (BrasilAPI /cep/v2).
-- Precisão de logradouro/quadra — suficiente pra roteirização.
UPDATE customers SET address_latitude = -23.5617698, address_longitude = -46.6553299
  WHERE id = '8f14e45f-ceea-467e-b3a1-9d2e5c0a5001'; -- Maria Souza / São Paulo SP

UPDATE customers SET address_latitude = -25.42778, address_longitude = -49.27306
  WHERE id = '8f14e45f-ceea-467e-b3a1-9d2e5c0a5002'; -- Distribuidora ABC / Curitiba PR

UPDATE customers SET address_latitude = -19.92083, address_longitude = -43.93778
  WHERE id = '8f14e45f-ceea-467e-b3a1-9d2e5c0a5003'; -- João Pereira / Belo Horizonte MG

UPDATE customers SET address_latitude = -30.03283, address_longitude = -51.23019
  WHERE id = '8f14e45f-ceea-467e-b3a1-9d2e5c0a5004'; -- Fornecedora Central / Porto Alegre RS

UPDATE customers SET address_latitude = -22.9012347, address_longitude = -43.177436
  WHERE id = '8f14e45f-ceea-467e-b3a1-9d2e5c0a5005'; -- Comércio e Distribuição Real / Rio de Janeiro RJ

UPDATE customers SET address_latitude = -27.59667, address_longitude = -48.54917
  WHERE id = '8f14e45f-ceea-467e-b3a1-9d2e5c0a5006'; -- Ana Lima / Florianópolis SC
