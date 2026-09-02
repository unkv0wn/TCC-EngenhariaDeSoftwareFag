-- Formas de pagamento (catálogo referenciado pelos pedidos).
--
-- id é VARCHAR (não UUID gerado pelo banco) de propósito: as linhas de seed abaixo
-- usam os mesmos ids "legíveis" que o mock do front-end já usa (pix, boleto, ...),
-- então o cadastro de Pedidos — que ainda não foi migrado — continua funcionando
-- sem quebrar a referência enquanto a migração avança módulo a módulo.
CREATE TABLE payment_methods (
  id         VARCHAR(40)  PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_payment_methods_name UNIQUE (name)
);

INSERT INTO payment_methods (id, name) VALUES
  ('dinheiro',        'Dinheiro'),
  ('pix',              'Pix'),
  ('cartao-credito',   'Cartão de Crédito'),
  ('cartao-debito',    'Cartão de Débito'),
  ('boleto',           'Boleto'),
  ('transferencia',    'Transferência Bancária');
