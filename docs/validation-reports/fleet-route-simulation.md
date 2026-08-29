# Simulação de frota multi-veículo — Cenário 1 vs Cenário 2

Gerado em: 2026-08-20 21:48:19

## 1. Metodologia

Os 9 pontos de entrega são divididos em 3 clusters geográficos por varredura angular em torno do depósito (P0) — heurística padrão de VRP, determinística. A composição de cada cluster não muda entre os cenários. Para cada cluster, `AStarWaypointOptimizer` (código de produção, reaproveitado sem alteração) resolve o TSP local — depósito + 3 paradas, `ROUND_TRIP` — minimizando distância; como cada cluster é sempre percorrido por um único veículo a uma única velocidade, minimizar distância equivale a minimizar duração, então a ordem de visita não muda entre os cenários, só o custo de percorrê-la. A atribuição dos 3 veículos aos 3 clusters é resolvida por busca exaustiva sobre as 3! = 6 permutações possíveis, escolhendo a de menor custo total — pequeno o bastante para busca exata, sem necessidade de heurística. Distâncias são Haversine (linha reta) escaladas por um fator de sinuosidade de 1.3 para aproximar distância real de rua — nenhuma chamada real ao OSRM é feita por este script; as URLs na seção 4 servem para validar manualmente contra o roteamento real.

## 2. Clusters e ordem de visita (A*)

| Cluster | Pontos | Ordem de visita | Distância (km) |
|---|---|---|---|
| Cluster A | P4, P5, P6 | P0 → P6 → P5 → P4 → P0 | 2.01 |
| Cluster B | P7, P8, P2 | P0 → P7 → P8 → P2 → P0 | 2.09 |
| Cluster C | P1, P9, P3 | P0 → P3 → P9 → P1 → P0 | 1.94 |

## 3. Cenário 1 — velocidade uniforme (60 km/h)

| Veículo | Cluster | Ordem | Distância (km) | Velocidade (km/h) | Tempo (h) | Combustível (R$) | Motorista (R$) | Total (R$) |
|---|---|---|---|---|---|---|---|---|
| VUC (Médio) | Cluster A | P0 → P6 → P5 → P4 → P0 | 2.01 | 60 | 0.03 | R$ 2.41 | R$ 1.00 | R$ 3.42 |
| Van (Leve) | Cluster B | P0 → P7 → P8 → P2 → P0 | 2.09 | 60 | 0.03 | R$ 1.67 | R$ 0.87 | R$ 2.55 |
| Truck (Grande) | Cluster C | P0 → P3 → P9 → P1 → P0 | 1.94 | 60 | 0.03 | R$ 3.50 | R$ 1.29 | R$ 4.79 |

**Custo total da frota: R$ 10.75**

## 4. Cenário 2 — com Vl_Max por veículo

| Veículo | Cluster | Ordem | Distância (km) | Velocidade (km/h) | Tempo (h) | Combustível (R$) | Motorista (R$) | Total (R$) |
|---|---|---|---|---|---|---|---|---|
| VUC (Médio) | Cluster A | P0 → P6 → P5 → P4 → P0 | 2.01 | 60 | 0.03 | R$ 2.41 | R$ 1.00 | R$ 3.42 |
| Van (Leve) | Cluster B | P0 → P7 → P8 → P2 → P0 | 2.09 | 60 | 0.03 | R$ 1.67 | R$ 0.87 | R$ 2.55 |
| Truck (Grande) | Cluster C | P0 → P3 → P9 → P1 → P0 | 1.94 | 50 | 0.04 | R$ 3.50 | R$ 1.55 | R$ 5.05 |

**Custo total da frota: R$ 11.01**

## 5. Comparação

| Métrica | Cenário 1 (sem Vl_Max) | Cenário 2 (com Vl_Max) |
|---|---|---|
| Custo total da frota | R$ 10.75 | R$ 11.01 |
| veículo 1 serve | VUC (Médio) → Cluster A | VUC (Médio) → Cluster A |
| veículo 2 serve | Van (Leve) → Cluster B | Van (Leve) → Cluster B |
| veículo 3 serve | Truck (Grande) → Cluster C | Truck (Grande) → Cluster C |

A atribuição veículo→cluster não muda entre os cenários. Diferença de custo total: R$ 0.26 (2.41%).

## 6. Validação OSRM (URLs reais, clicáveis)

Cenário 1:

- **VUC (Médio)** (Cluster A): http://router.project-osrm.org/route/v1/driving/-46.6333,-23.5505;-46.6380,-23.5500;-46.6360,-23.5485;-46.6310,-23.5490;-46.6333,-23.5505?geometries=geojson&annotations=true
- **Van (Leve)** (Cluster B): http://router.project-osrm.org/route/v1/driving/-46.6333,-23.5505;-46.6400,-23.5515;-46.6390,-23.5535;-46.6355,-23.5520;-46.6333,-23.5505?geometries=geojson&annotations=true
- **Truck (Grande)** (Cluster C): http://router.project-osrm.org/route/v1/driving/-46.6333,-23.5505;-46.6320,-23.5530;-46.6370,-23.5550;-46.6340,-23.5510;-46.6333,-23.5505?geometries=geojson&annotations=true

Cenário 2:

- **VUC (Médio)** (Cluster A): http://router.project-osrm.org/route/v1/driving/-46.6333,-23.5505;-46.6380,-23.5500;-46.6360,-23.5485;-46.6310,-23.5490;-46.6333,-23.5505?geometries=geojson&annotations=true
- **Van (Leve)** (Cluster B): http://router.project-osrm.org/route/v1/driving/-46.6333,-23.5505;-46.6400,-23.5515;-46.6390,-23.5535;-46.6355,-23.5520;-46.6333,-23.5505?geometries=geojson&annotations=true
- **Truck (Grande)** (Cluster C): http://router.project-osrm.org/route/v1/driving/-46.6333,-23.5505;-46.6320,-23.5530;-46.6370,-23.5550;-46.6340,-23.5510;-46.6333,-23.5505?geometries=geojson&annotations=true

## 7. GeoJSON simulado — Cenário 1

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "vehicle": "VUC (Médio)",
        "cluster": "Cluster A",
        "stroke": "#00ff00",
        "stroke-width": 4,
        "distance_km": 2.01,
        "total_cost_reais": 3.42
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [-46.6333, -23.5505],
          [-46.6380, -23.5500],
          [-46.6360, -23.5485],
          [-46.6310, -23.5490],
          [-46.6333, -23.5505]
        ]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "vehicle": "Van (Leve)",
        "cluster": "Cluster B",
        "stroke": "#ff0000",
        "stroke-width": 4,
        "distance_km": 2.09,
        "total_cost_reais": 2.55
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [-46.6333, -23.5505],
          [-46.6400, -23.5515],
          [-46.6390, -23.5535],
          [-46.6355, -23.5520],
          [-46.6333, -23.5505]
        ]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "vehicle": "Truck (Grande)",
        "cluster": "Cluster C",
        "stroke": "#0000ff",
        "stroke-width": 4,
        "distance_km": 1.94,
        "total_cost_reais": 4.79
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [-46.6333, -23.5505],
          [-46.6320, -23.5530],
          [-46.6370, -23.5550],
          [-46.6340, -23.5510],
          [-46.6333, -23.5505]
        ]
      }
    }
  ]
}
```

## 8. GeoJSON simulado — Cenário 2

```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "vehicle": "VUC (Médio)",
        "cluster": "Cluster A",
        "stroke": "#00ff00",
        "stroke-width": 4,
        "distance_km": 2.01,
        "total_cost_reais": 3.42
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [-46.6333, -23.5505],
          [-46.6380, -23.5500],
          [-46.6360, -23.5485],
          [-46.6310, -23.5490],
          [-46.6333, -23.5505]
        ]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "vehicle": "Van (Leve)",
        "cluster": "Cluster B",
        "stroke": "#ff0000",
        "stroke-width": 4,
        "distance_km": 2.09,
        "total_cost_reais": 2.55
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [-46.6333, -23.5505],
          [-46.6400, -23.5515],
          [-46.6390, -23.5535],
          [-46.6355, -23.5520],
          [-46.6333, -23.5505]
        ]
      }
    },
    {
      "type": "Feature",
      "properties": {
        "vehicle": "Truck (Grande)",
        "cluster": "Cluster C",
        "stroke": "#0000ff",
        "stroke-width": 4,
        "distance_km": 1.94,
        "total_cost_reais": 5.05
      },
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [-46.6333, -23.5505],
          [-46.6320, -23.5530],
          [-46.6370, -23.5550],
          [-46.6340, -23.5510],
          [-46.6333, -23.5505]
        ]
      }
    }
  ]
}
```
