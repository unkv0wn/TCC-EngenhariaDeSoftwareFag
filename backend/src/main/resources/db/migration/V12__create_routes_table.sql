-- Rotas geradas e salvas. Substitui o store em memória do front (useRoutes.ts).
--
-- `result_json` guarda o RouteResultDto inteiro (waypoints ordenados, segmentos,
-- geometria OSRM) como snapshot — é o que o mapa redesenha. `stops_json` guarda
-- os rótulos + coordenada das paradas de cliente, na ordem enviada ao algoritmo.
-- Os campos escalares (distância/tempo/valor) são desnormalizados do result_json
-- pra o dashboard poder somar/filtrar sem abrir o JSON.
CREATE TABLE routes (
  id                  UUID             PRIMARY KEY,
  driver_id           UUID             NOT NULL REFERENCES drivers(id),
  vehicle_id          UUID             NOT NULL REFERENCES vehicles(id),
  departure_time      VARCHAR(5)       NOT NULL,
  status              VARCHAR(20)      NOT NULL,
  orders_count        INTEGER          NOT NULL,
  total_value         DOUBLE PRECISION NOT NULL,
  total_distance_km   DOUBLE PRECISION NOT NULL,
  total_duration_min  DOUBLE PRECISION NOT NULL,
  completed_stops     INTEGER          NOT NULL DEFAULT 0,
  result_json         TEXT             NOT NULL,
  stops_json          TEXT             NOT NULL,
  created_at          TIMESTAMPTZ      NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE INDEX idx_routes_created_at ON routes (created_at DESC);
