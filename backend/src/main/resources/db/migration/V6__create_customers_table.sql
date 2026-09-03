-- Clientes/fornecedores. Endereço fica achatado em colunas prefixadas (address_*) — o
-- front trabalha com um objeto aninhado, mas a camada de DTO cuida do mapeamento.
CREATE TABLE customers (
  id                  UUID         PRIMARY KEY,
  person_type         VARCHAR(10)  NOT NULL,
  document            VARCHAR(20)  NOT NULL,
  name                VARCHAR(150) NOT NULL,
  trade_name          VARCHAR(150),
  type                VARCHAR(12)  NOT NULL,
  email               VARCHAR(150) NOT NULL,
  phone               VARCHAR(20)  NOT NULL,
  address_zip_code    VARCHAR(10)  NOT NULL,
  address_street      VARCHAR(150) NOT NULL,
  address_number      VARCHAR(20)  NOT NULL,
  address_complement  VARCHAR(100),
  address_district    VARCHAR(100) NOT NULL,
  address_city        VARCHAR(100) NOT NULL,
  address_state       VARCHAR(2)   NOT NULL,
  created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CONSTRAINT uq_customers_document UNIQUE (document)
);

INSERT INTO customers (
  id, person_type, document, name, trade_name, type, email, phone,
  address_zip_code, address_street, address_number, address_complement, address_district, address_city, address_state
) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5001', 'fisica',   '529.982.247-25',      'Maria Souza',                              NULL,            'cliente',   'maria.souza@email.com',    '(11) 98765-4321', '01310-100', 'Avenida Paulista',        '1000', NULL,        'Bela Vista',      'São Paulo',       'SP'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5002', 'juridica', '11.222.333/0001-81',  'Distribuidora ABC Ltda',                   'ABC Bebidas',   'cliente',   'contato@abcbebidas.com.br','(41) 3025-4477',  '80010-000', 'Rua XV de Novembro',      '500',  NULL,        'Centro',          'Curitiba',        'PR'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5003', 'fisica',   '123.456.789-09',      'João Pereira',                             NULL,            'cliente',   'joao.pereira@email.com',   '(31) 99876-5432', '30130-010', 'Avenida Afonso Pena',     '200',  'Apto 302',  'Centro',          'Belo Horizonte',  'MG'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5004', 'juridica', '12.345.678/0001-95',  'Fornecedora Central de Alimentos Ltda',    'Central Alimentos', 'fornecedor', 'vendas@centralalimentos.com.br', '(51) 3212-8899', '90010-150', 'Rua dos Andradas', '1200', NULL, 'Centro Histórico', 'Porto Alegre',    'RS'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5005', 'juridica', '98.765.432/0001-98',  'Comércio e Distribuição Real Ltda',        'Real Distribuidora','ambos',    'contato@realdistribuidora.com.br', '(21) 2536-7788', '20040-020', 'Rua da Assembleia', '77',   NULL,   'Centro',          'Rio de Janeiro',  'RJ'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5006', 'fisica',   '987.654.321-00',      'Ana Lima',                                  NULL,            'cliente',   'ana.lima@email.com',       '(48) 99654-1122', '88010-400', 'Rua Felipe Schmidt',      '50',   NULL,        'Centro',          'Florianópolis',   'SC');
