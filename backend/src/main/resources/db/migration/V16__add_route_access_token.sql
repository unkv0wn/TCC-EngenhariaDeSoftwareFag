-- Acesso do app do motorista (ver docs/superpowers/specs/2026-10-05-driver-mobile-app-design.md).
--
-- `access_token`: 32 bytes aleatórios em base64url (43 caracteres), gerados com
-- SecureRandom quando a rota é salva. É um SEGREDO PORTADOR, não autenticação —
-- quem tem o link tem a rota. Aceitável porque o link vai pro WhatsApp do motorista
-- escalado e expõe uma rota, não o sistema. Nulo nas rotas criadas antes desta
-- migration: elas ganham token na primeira vez que forem compartilhadas.
ALTER TABLE routes ADD COLUMN access_token VARCHAR(43);
CREATE UNIQUE INDEX idx_routes_access_token ON routes (access_token);

-- `client_event_id`: id gerado no celular, pra fila offline ser idempotente. Sem ele,
-- perder a resposta de rede num túnel faz o motorista reenviar e duplicar a entrega —
-- e aí o progresso da rota passa a mentir. Nulo nos eventos criados pelo gestor na web,
-- que não passam por fila nenhuma; o índice único ignora nulos no Postgres.
ALTER TABLE route_events ADD COLUMN client_event_id UUID;
CREATE UNIQUE INDEX idx_route_events_client_id ON route_events (client_event_id);
