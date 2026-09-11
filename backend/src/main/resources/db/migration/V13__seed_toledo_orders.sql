-- Seed de dados focado em Toledo/PR — clientes, produtos e ~20 pedidos faturados
-- prontos pra roteirização, mais o fechamento dos pedidos antigos como "entregue".
--
-- ids fixos no mesmo padrão das seeds anteriores (V6/V9/V10). Registros criados
-- pela aplicação continuam usando UUID.randomUUID().

-- ─────────────────────────────────────────────────────────────────────────────
-- PRODUTOS (catálogo — complementa os 5 da V9)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO products (id, sku, name, description, unit_id, unit_price, weight_kg) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8006', 'FAR-006', 'Farinha de Trigo 5kg',
    'Saco de farinha de trigo tipo 1.',                       '8f14e45f-ceea-467e-b3a1-9d2e5c0a2001', 21.50, 5),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8007', 'ACU-007', 'Açúcar Refinado 5kg',
    'Saco de açúcar refinado.',                               '8f14e45f-ceea-467e-b3a1-9d2e5c0a2001', 22.90, 5),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8008', 'CAF-008', 'Café Torrado e Moído 500g (fardo c/ 10)',
    'Fardo com 10 pacotes de café torrado e moído.',          '8f14e45f-ceea-467e-b3a1-9d2e5c0a2003', 89.00, 5.5),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8009', 'LEI-009', 'Leite UHT Integral 1L (caixa c/ 12)',
    'Caixa com 12 litros de leite UHT integral.',             '8f14e45f-ceea-467e-b3a1-9d2e5c0a2003', 62.40, 12.5),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8010', 'MAC-010', 'Macarrão Espaguete 500g (fardo c/ 20)',
    'Fardo com 20 pacotes de macarrão espaguete.',            '8f14e45f-ceea-467e-b3a1-9d2e5c0a2003', 74.00, 10),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a8011', 'SAB-011', 'Sabão em Pó 1kg (caixa c/ 12)',
    'Caixa com 12 pacotes de sabão em pó.',                   '8f14e45f-ceea-467e-b3a1-9d2e5c0a2003', 108.00, 12.5);

-- ─────────────────────────────────────────────────────────────────────────────
-- CLIENTES em Toledo/PR e região (coordenada já preenchida — Fase B)
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO customers (
  id, person_type, document, name, trade_name, type, email, phone,
  address_zip_code, address_street, address_number, address_complement, address_district, address_city, address_state,
  address_latitude, address_longitude
) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5007', 'fisica',   '100.200.300-88',     'Antônio Ferreira',                     NULL,            'cliente',   'antonio.ferreira@email.com',       '(45) 99811-2020', '85900-030', 'Rua Barão do Rio Branco',   '1250', NULL,        'Centro',              'Toledo', 'PR', -24.72460, -53.74120),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5008', 'juridica', '22.333.444/0001-81', 'Supermercado Bom Preço Ltda',          'Bom Preço',     'cliente',   'compras@bompreco.com.br',          '(45) 3252-1010',  '85903-000', 'Avenida Parigot de Souza',  '3400', NULL,        'Jardim La Salle',     'Toledo', 'PR', -24.71000, -53.73000),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5009', 'fisica',   '111.222.333-96',     'Juliana Marques',                      NULL,            'cliente',   'juliana.marques@email.com',        '(45) 99722-3131', '85900-000', 'Rua Sete de Setembro',      '890',  'Apto 12',   'Centro',              'Toledo', 'PR', -24.72200, -53.74400),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5010', 'juridica', '33.444.555/0001-81', 'Distribuidora Oeste Ltda',             'Oeste Distrib', 'ambos',     'vendas@oestedistrib.com.br',       '(45) 3277-4545',  '85905-000', 'Rua Raposo Tavares',        '220',  'Galpão 3',  'Vila Industrial',     'Toledo', 'PR', -24.73500, -53.72500),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5011', 'fisica',   '222.333.444-05',     'Pedro Henrique Alves',                 NULL,            'cliente',   'pedro.alves@email.com',            '(45) 99633-4242', '85904-100', 'Rua Almirante Barroso',     '145',  NULL,        'Vila Pioneiro',       'Toledo', 'PR', -24.71800, -53.75000),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5012', 'fisica',   '333.444.555-08',     'Camila Rodrigues',                     NULL,            'cliente',   'camila.rodrigues@email.com',       '(45) 99544-5353', '85906-000', 'Avenida Ministro Cirne Lima','2100', NULL,        'Jardim Coopagro',     'Toledo', 'PR', -24.74200, -53.73500),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5013', 'juridica', '44.555.666/0001-81', 'Padaria e Confeitaria Trigo Dourado Ltda', 'Trigo Dourado', 'cliente', 'contato@trigodourado.com.br',      '(45) 3255-6767',  '85900-110', 'Rua Guaíra',                '560',  NULL,        'Centro',              'Toledo', 'PR', -24.72650, -53.73850),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5014', 'fisica',   '444.555.666-19',     'Fernanda Lima Souza',                  NULL,            'cliente',   'fernanda.souza@email.com',         '(45) 99455-6464', '85907-000', 'Rua Presidente Kennedy',    '780',  NULL,        'Jardim Porto Alegre', 'Toledo', 'PR', -24.70500, -53.74200),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a5015', 'fisica',   '555.666.777-20',     'Ricardo Gomes',                        NULL,            'cliente',   'ricardo.gomes@email.com',          '(45) 99366-7575', '85919-000', 'Rua Osvaldo Cruz',          '300',  NULL,        'Vila Nova',           'Toledo', 'PR', -24.67000, -53.79000);

-- ─────────────────────────────────────────────────────────────────────────────
-- PEDIDOS — 18 faturados (prontos pra rota) + 2 aguardando, todos em Toledo/PR
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO orders (id, customer_id, vehicle_id, driver_id, payment_method_id, payment_condition_id, date, status, discount, shipping_cost, notes) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9007', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5007', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-09-01', 'faturado', 0,  20, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9008', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5008', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1005', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7003', '2026-09-01', 'faturado', 15, 0,  'Entregar até 11h.'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9009', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5009', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-09-02', 'faturado', 0,  15, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9010', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5010', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1006', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7004', '2026-09-02', 'faturado', 30, 0,  NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9011', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5011', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7002', '2026-09-03', 'faturado', 0,  18, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9012', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5012', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7005', '2026-09-03', 'faturado', 0,  25, 'Cliente pede nota junto.'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9013', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5013', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-09-04', 'faturado', 10, 12, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9014', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5014', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1005', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7003', '2026-09-04', 'faturado', 0,  22, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9015', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5015', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1006', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7004', '2026-09-05', 'faturado', 0,  40, 'Zona rural — combinar acesso por telefone.'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9016', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5007', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-09-05', 'faturado', 5,  15, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9017', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5008', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1005', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7003', '2026-09-08', 'faturado', 20, 0,  NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9018', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5009', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-09-08', 'faturado', 0,  15, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9019', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5010', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1006', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7004', '2026-09-09', 'faturado', 25, 0,  NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9020', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5011', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7002', '2026-09-09', 'faturado', 0,  18, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9021', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5012', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7005', '2026-09-09', 'faturado', 0,  20, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9022', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5013', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-09-10', 'faturado', 10, 12, 'Retirar vasilhame antigo.'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9023', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5014', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1005', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7003', '2026-09-10', 'faturado', 0,  22, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9024', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5015', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1006', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7004', '2026-09-10', 'faturado', 0,  35, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9025', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5007', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-09-10', 'aguardando', 0, 20, NULL),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9026', '8f14e45f-ceea-467e-b3a1-9d2e5c0a5009', '8f14e45f-ceea-467e-b3a1-9d2e5c0a4003', '8f14e45f-ceea-467e-b3a1-9d2e5c0a3002', '8f14e45f-ceea-467e-b3a1-9d2e5c0a1001', '8f14e45f-ceea-467e-b3a1-9d2e5c0a7001', '2026-09-10', 'aguardando', 0, 15, NULL);

INSERT INTO order_items (order_id, item_order, product_id, quantity, unit_price) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9007', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8009', 8,  62.40),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9007', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8001', 10, 18.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9008', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8002', 15, 42.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9009', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8003', 6,  24.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9009', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8007', 4,  22.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9010', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8004', 5,  65.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9010', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8010', 8,  74.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9011', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8006', 12, 21.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9012', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8011', 6,  108.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9012', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8005', 20, 2.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9013', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8001', 20, 18.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9014', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8009', 10, 62.40),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9014', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8008', 3,  89.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9015', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8010', 12, 74.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9015', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8003', 10, 24.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9016', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8002', 6,  42.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9016', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8007', 5,  22.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9017', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8008', 6,  89.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9018', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8006', 8,  21.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9018', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8007', 8,  22.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9019', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8011', 4,  108.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9020', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8001', 15, 18.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9020', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8009', 6,  62.40),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9021', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8004', 4,  65.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9022', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8002', 10, 42.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9022', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8005', 24, 2.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9023', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8010', 20, 74.00),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9024', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8003', 16, 24.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9024', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8006', 10, 21.50),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9025', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8009', 12, 62.40),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9026', 0, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8001', 10, 18.90),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9026', 1, '8f14e45f-ceea-467e-b3a1-9d2e5c0a8007', 6,  22.90);

-- Linha do tempo dos pedidos novos.
INSERT INTO order_status_history (order_id, status, changed_at)
SELECT id, 'aguardando', ((date - 1) + TIME '09:00')::timestamptz
FROM orders WHERE id BETWEEN '8f14e45f-ceea-467e-b3a1-9d2e5c0a9007' AND '8f14e45f-ceea-467e-b3a1-9d2e5c0a9026';

INSERT INTO order_status_history (order_id, status, changed_at)
SELECT id, 'faturado', (date + TIME '13:30')::timestamptz
FROM orders WHERE id BETWEEN '8f14e45f-ceea-467e-b3a1-9d2e5c0a9007' AND '8f14e45f-ceea-467e-b3a1-9d2e5c0a9024';

-- ─────────────────────────────────────────────────────────────────────────────
-- PEDIDOS ANTIGOS (V10) → entregue
-- ─────────────────────────────────────────────────────────────────────────────
UPDATE orders SET status = 'entregue'
WHERE id IN (
  '8f14e45f-ceea-467e-b3a1-9d2e5c0a9002',
  '8f14e45f-ceea-467e-b3a1-9d2e5c0a9003',
  '8f14e45f-ceea-467e-b3a1-9d2e5c0a9004',
  '8f14e45f-ceea-467e-b3a1-9d2e5c0a9005',
  '8f14e45f-ceea-467e-b3a1-9d2e5c0a9006'
) AND status <> 'entregue';

INSERT INTO order_status_history (order_id, status, changed_at) VALUES
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9003', 'faturado', '2026-08-30T09:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9004', 'em_rota',  '2026-09-01T08:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9005', 'em_rota',  '2026-09-01T08:10:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9006', 'em_rota',  '2026-09-01T08:20:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9003', 'em_rota',  '2026-09-02T08:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9002', 'entregue', '2026-09-02T15:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9003', 'entregue', '2026-09-02T15:30:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9004', 'entregue', '2026-09-02T16:00:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9005', 'entregue', '2026-09-02T16:20:00Z'),
  ('8f14e45f-ceea-467e-b3a1-9d2e5c0a9006', 'entregue', '2026-09-02T16:40:00Z');
