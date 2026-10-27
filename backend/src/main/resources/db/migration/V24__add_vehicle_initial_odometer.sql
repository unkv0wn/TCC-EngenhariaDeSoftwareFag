-- Leitura do odômetro no dia em que o veículo foi cadastrado.
--
-- É um dado que o sistema não tem como derivar: entre a fabricação do caminhão e o
-- cadastro dele aqui existe uma quilometragem que nenhum abastecimento registrou.
--
-- NULL é permitido de propósito. Os veículos já cadastrados não têm essa leitura, e
-- inventar uma (zero, ou a do primeiro abastecimento) seria fabricar medição — o
-- consumo do modelo de custo sai daqui, então um número inventado vira R$ inventado.
--
-- Note o que esta migração **não** cria: uma coluna de "odômetro atual". Esse valor é a
-- maior leitura conhecida do veículo — entre esta e a dos abastecimentos — e é derivado
-- em leitura, nunca armazenado. É a mesma decisão da V7 sobre km_since_previous, pelo
-- mesmo motivo: uma coluna que o abastecimento sobrescreve fica errada assim que alguém
-- edita um registro antigo, apaga o mais recente ou lança fora de ordem, e nada avisa.
ALTER TABLE vehicles ADD COLUMN initial_odometer_km DOUBLE PRECISION;

COMMENT ON COLUMN vehicles.initial_odometer_km IS
  'Odômetro no cadastro. NULL = não informado. O odômetro atual é derivado, não armazenado.';
