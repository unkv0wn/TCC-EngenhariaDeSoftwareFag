# Calibração de Consumo com Especificações Reais de Motor

Gerado por `EngineSpecFuelCalibrationExperiment` — 500 cenários sintéticos (seed 42), mesmos para as duas rodadas de cada perfil (só `baseFuelConsumptionLPer100Km` muda).

## 1. Metodologia e ressalva

`VehicleProfile.baseFuelConsumptionLPer100Km` é definido como consumo a vazio, em via ARTERIAL. Fichas técnicas públicas informam cidade/estrada, então a base ARTERIAL é obtida invertendo a própria fórmula do `CostMatrixBuilder` — ver Javadoc de `EngineSpecFuelCalibrationExperiment` para a derivação completa, número por número, de cada um dos três perfis.

**Nenhum desses números é telemetria de frota própria** — são fichas técnicas de fabricante e médias reportadas por fontes do setor (ver Seção 4), a mesma ressalva já registrada em `VehicleProfile` para os valores que estão sendo substituídos aqui. O perfil Pesado tem a maior incerteza dos três: não existe ficha pública "vazio, cidade/estrada" para uma combinação bitrem completa, então sua base foi obtida a partir de um consumo médio já carregado, assumindo `loadFactor ≈ 1`.

## 2. Consumo base: placeholder atual vs. derivado de motor real

| Perfil | Veículo / motor real | Atual (placeholder) | Derivado (fonte real) | Diferença |
|---|---|---|---|---|
| Leve (2 eixos) | Hyundai HR 2.5 CRDi (D4CB 2.5L turbodiesel, 130 cv) | 10.0 L/100km | 10.16 L/100km | +1.6% |
| Médio (3 eixos) | Mercedes-Benz Accelo 815 (OM924 LA 4.8L, 156 cv, 580 Nm) | 15.0 L/100km | 17.81 L/100km | +18.7% |
| Pesado (5 eixos) | Bitrem (cavalo Scania R450) (DC13 12.7L, 450 cv, 2350 Nm) | 32.0 L/100km | 45.25 L/100km | +41.4% |

- **Leve (2 eixos)** (Hyundai HR 2.5 CRDi, D4CB 2.5L turbodiesel, 130 cv): Cidade 8 km/L (12,50 L/100km) e estrada 11 km/L (9,09 L/100km); base = média(12,50/1,3; 9,09/0,85) = média(9,62; 10,70) = 10,16 L/100km. Fonte: motortudo.com/ficha-tecnica-hyundai-hr-hd-2-5-turbo-2021
- **Médio (3 eixos)** (Mercedes-Benz Accelo 815, OM924 LA 4.8L, 156 cv, 580 Nm): Cidade 4,2 km/L (23,81 L/100km) e estrada 6,8 km/L (14,71 L/100km); base = média(23,81/1,3; 14,71/0,85) = média(18,32; 17,31) = 17,81 L/100km. Fonte: consultadeplaca.net/blog/ficha-tecnica-accelo-815
- **Pesado (5 eixos)** (Bitrem (cavalo Scania R450), DC13 12.7L, 450 cv, 2350 Nm): Consumo médio carregado em rodovia 1,8–2,3 km/L (ponto médio 2,0 km/L = 50,0 L/100km); assumindo loadFactor≈1 sobre RODOVIA: base = 50,0 / [(1+0,30·1)·0,85] = 50,0/1,105 = 45,25 L/100km (faixa: 39,3–50,3 L/100km). Fonte: blog.fretebras.com.br/caminhao-bitrem; infleet.com.br/blog/tabela-consumo-combustivel-caminhoes

## 3. Efeito no experimento de 500 cenários (mesma rota A* / mesmo custoB)

| Perfil | Rotas diferentes | Custo médio atual | Custo médio corrigido | Combustível médio atual | Combustível médio corrigido | Gap médio (R$) | Gap médio (%) | Wilcoxon p |
|---|---|---|---|---|---|---|---|---|
| Leve (2 eixos) | 0.2% | R$ 243.94 | R$ 245.98 | R$ 127.67 | R$ 129.72 | R$ 0.00 | 0.00% | N/A |
| Médio (3 eixos) | 1.6% | R$ 352.03 | R$ 385.79 | R$ 180.24 | R$ 213.84 | R$ 0.01 | 0.00% | 0.0142 |
| Pesado (5 eixos) | 4.8% | R$ 543.69 | R$ 682.73 | R$ 336.09 | R$ 474.51 | R$ 0.12 | 0.01% | 0.0000 |

"Rotas diferentes" = quantos dos 500 cenários o A* escolhe uma ordem diferente ao trocar só o consumo-base pelo valor derivado de motor real, mantendo tudo o mais igual. "Gap médio" = quanto, em R$ sob a função de custo corrigida, a rota antiga (otimizada com o placeholder) perde para a rota recalculada com o consumo real — sempre ≥ 0 por construção, mesma lógica do `MarginalContributionAnalyzer`.

## 4. Conclusão

- **Leve (2 eixos)**: 0.2% das rotas mudam — sem significância estatística suficiente nesta amostra (Wilcoxon). Custo médio previsto sobe de R$ 243.94 para R$ 245.98 (+0.8%) só pela troca do consumo-base.
- **Médio (3 eixos)**: 1.6% das rotas mudam e a diferença é estatisticamente significativa (Wilcoxon p < 0,05). Custo médio previsto sobe de R$ 352.03 para R$ 385.79 (+9.6%) só pela troca do consumo-base.
- **Pesado (5 eixos)**: 4.8% das rotas mudam e a diferença é estatisticamente significativa (Wilcoxon p < 0,05). Custo médio previsto sobe de R$ 543.69 para R$ 682.73 (+25.6%) só pela troca do consumo-base.

O perfil **Leve** mudou pouco (o placeholder já estava perto do valor real do Hyundai HR). **Médio** e, principalmente, **Pesado** tinham consumo-base subestimado — o placeholder de 32,0 L/100km para o perfil pesado está bem abaixo da faixa real de um bitrem carregado (39,3–50,3 L/100km), então qualquer conclusão de TCC que dependa da magnitude absoluta do custo de combustível para veículos pesados deveria usar os valores desta calibração, não os placeholders originais de `VehicleProfile`.

## 5. Fontes

- Hyundai HR 2.5 CRDi (D4CB): motortudo.com — ficha técnica HR HD 2.5 Turbo 2021
- Mercedes-Benz Accelo 815 (OM924 LA): consultadeplaca.net/blog — ficha técnica Accelo 815 (dados compilados de testes reais/ABRACAM e frotistas)
- Scania R450 (motor DC13): blog.caminhoesecarretas.com.br — ficha técnica Scania R450
- Consumo de bitrem carregado: blog.fretebras.com.br/caminhao-bitrem; infleet.com.br/blog/tabela-consumo-combustivel-caminhoes

Consultadas em 2026-08-13. Nenhuma é telemetria de frota própria do RouteWise — ver ressalva na Seção 1.
