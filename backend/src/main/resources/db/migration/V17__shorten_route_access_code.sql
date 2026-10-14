-- Troca o token de 43 caracteres (V16) por um código de 12, em base32 de Crockford.
--
-- Motivo: o token longo servia bem a link e QR Code, mas o plano B — alguém ditando o
-- código por telefone, ou o motorista digitando porque a câmera não leu — era inviável com
-- 43 caracteres. 12 caracteres dão 60 bits, que continuam inatingíveis por força bruta e
-- cabem numa conversa. Ver RouteAccessCodeGenerator.
--
-- As rotas existentes ficam com código nulo: o valor antigo não estava em uso por ninguém
-- (o front nunca chegou a exibi-lo) e gerar aleatoriedade com alfabeto restrito em SQL
-- renderia um código de qualidade pior que o do gerador. Elas recebem um na primeira vez
-- que forem compartilhadas, via POST /api/routes/{id}/access-code.
ALTER TABLE routes DROP COLUMN access_token;
ALTER TABLE routes ADD COLUMN access_code VARCHAR(12);

CREATE UNIQUE INDEX idx_routes_access_code ON routes (access_code);
