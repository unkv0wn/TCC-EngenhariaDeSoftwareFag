# Simulação de frota — 4 veículos em Toledo, PR (malha viária real via OSRM)

## 1. Metodologia

Os 9 pontos de entrega foram divididos em 4 clusters geográficos por varredura angular em
torno do depósito (P0), com tamanhos 3/2/2/2. Diferente do exercício anterior (São Paulo,
distância Haversine), aqui a matriz de distância/duração entre todos os 10 pontos foi obtida
**diretamente da API pública do OSRM** (`/table`, malha viária real de Toledo-PR) — a mesma
fonte de dados que o backend de produção usa. Para cada cluster, a ordem de visita ótima
(depósito + paradas, ida e volta) foi resolvida por busca exata (permutação completa —
viável porque cada cluster tem no máximo 3 paradas). A atribuição dos 4 veículos aos 4
clusters foi resolvida por busca exaustiva sobre as 4! = 24 permutações possíveis.

**Tempo de direção** usa a velocidade efetiva = mínimo entre a velocidade média real
implícita no trecho (distância OSRM ÷ duração OSRM) e o `Vl_Max` do veículo — conforme
pedido, o menor dos dois. **Tempo de serviço** = número de paradas do cluster × tempo de
serviço por parada do veículo.

Fórmula de custo aplicada por veículo/cluster:

```
Custo = KM × (R$Combustível + R$Pneu/Manutenção) + (Tempo_Direção + Tempo_Serviço) × R$/Hora
```

Nenhuma chamada ao OSRM `/route` altera o resultado de custo — ela é usada só para desenhar
a geometria real das rotas no GeoJSON da seção 4.

## 2. Frota

| Veículo | Combustível | Pneu/Manut. | Motorista | Vl_Max | Serviço/parada |
|---|---|---|---|---|---|
| Van Leve Econômica | R$ 0,75/km | R$ 0,05/km | R$ 22,00/h | 100 km/h | 5 min |
| Furgão Leve Turbo | R$ 0,85/km | R$ 0,06/km | R$ 25,00/h | 110 km/h | 4 min |
| VUC Médio Padrão | R$ 1,20/km | R$ 0,10/km | R$ 30,00/h | 80 km/h | 8 min |
| VUC Médio Pesado | R$ 1,35/km | R$ 0,12/km | R$ 32,00/h | 70 km/h | 10 min |

## 3. Resultado da otimização (melhor entre as 24 atribuições possíveis)

| Veículo | Entregas | Ordem de visita | Distância | Tempo direção | Tempo serviço | Combustível+Pneu | Motorista | **Total** |
|---|---|---|---|---|---|---|---|---|
| Furgão Leve Turbo | P8, P7, P4 | P0→P8→P7→P4→P0 | 3.21 km | 6.2 min | 12.0 min | R$ 2.92 | R$ 7.58 | **R$ 10.50** |
| VUC Médio Pesado | P3, P1 | P0→P3→P1→P0 | 2.48 km | 4.4 min | 20.0 min | R$ 3.64 | R$ 13.02 | **R$ 16.66** |
| Van Leve Econômica | P6, P5 | P0→P6→P5→P0 | 3.02 km | 5.2 min | 10.0 min | R$ 2.42 | R$ 5.59 | **R$ 8.01** |
| VUC Médio Padrão | P2, P9 | P0→P2→P9→P0 | 2.88 km | 4.6 min | 16.0 min | R$ 3.75 | R$ 10.28 | **R$ 14.02** |

**Custo total da frota: R$ 49.19**

Note que o tempo de serviço (5–10 min por parada, ×4 veículos com taxas diferentes) domina
o custo mais do que o tempo de direção nesta simulação — os clusters são geograficamente
compactos (2.5–3.2 km de perímetro), então o "custo/hora do motorista parado entregando"
pesa mais que o "custo/km rodando". Isso é o motivo de o VUC Médio Pesado (parada mais
longa, 10 min, e motorista mais caro, R$32/h) custar mais que o Furgão Leve Turbo mesmo
fazendo uma rota parecida em distância.

## 4. Validação OSRM (URLs reais, clicáveis)

- **Furgão Leve Turbo** (P0→P8→P7→P4→P0): http://router.project-osrm.org/route/v1/driving/-53.7380,-24.7170;-53.7400,-24.7100;-53.7330,-24.7110;-53.7310,-24.7150;-53.7380,-24.7170?geometries=geojson&annotations=true
- **VUC Médio Pesado** (P0→P3→P1→P0): http://router.project-osrm.org/route/v1/driving/-53.7380,-24.7170;-53.7450,-24.7190;-53.7420,-24.7140;-53.7380,-24.7170?geometries=geojson&annotations=true
- **Van Leve Econômica** (P0→P6→P5→P0): http://router.project-osrm.org/route/v1/driving/-53.7380,-24.7170;-53.7440,-24.7220;-53.7390,-24.7250;-53.7380,-24.7170?geometries=geojson&annotations=true
- **VUC Médio Padrão** (P0→P2→P9→P0): http://router.project-osrm.org/route/v1/driving/-53.7380,-24.7170;-53.7350,-24.7210;-53.7360,-24.7260;-53.7380,-24.7170?geometries=geojson&annotations=true

## 5. GeoJSON (geometria real, seguindo as ruas de Toledo-PR)

Ver arquivo anexo `toledo-fleet-4vehicles.geojson` — `FeatureCollection` com 4 `LineString`,
uma por veículo, `stroke` distinto por veículo (Van=`#ff0000`, Furgão=`#ff9900`,
VUC Padrão=`#0000ff`, VUC Pesado=`#9900ff`), geometria obtida via OSRM `/route`
(`overview=full`), pronta para colar no geojson.io.
