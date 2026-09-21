-- Registro de eventos de uma rota em andamento: saída, chegada, entrega, ocorrência,
-- abastecimento, retorno. Substitui o `completed_stops` congelado da tabela `routes`
-- como fonte do progresso — uma parada está concluída quando existe um evento do tipo
-- 'entrega' apontando pra ela.
--
-- Diferente dos outros módulos, aqui a FK é real e com CASCADE: um evento não existe
-- fora da sua rota (composição, não referência), então apagar a rota apaga o log.
--
-- `stop_index` é nulo pros eventos que não pertencem a uma parada específica (saída,
-- abastecimento, ocorrência no caminho). Quando preenchido, é o índice da parada na
-- ordem otimizada — o mesmo número que o marcador mostra no mapa.
CREATE TABLE route_events (
  id           UUID         PRIMARY KEY,
  route_id     UUID         NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
  stop_index   INTEGER,
  type         VARCHAR(20)  NOT NULL,
  author_role  VARCHAR(20)  NOT NULL,
  author_name  VARCHAR(120) NOT NULL,
  notes        TEXT,
  occurred_at  TIMESTAMPTZ  NOT NULL,
  created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_route_events_route ON route_events (route_id, occurred_at);
