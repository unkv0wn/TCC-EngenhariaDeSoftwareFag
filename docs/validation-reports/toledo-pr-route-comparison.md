# Comparação de Rotas em Toledo, PR — Cenário A vs Cenário B

Gerado em: 2026-08-05 00:59:26

## 1. Contexto e metodologia

Exemplo trabalhado com um cenário fixo — não faz parte da amostragem estatística de 500 trials dos outros relatórios — situado inteiramente dentro do município de Toledo, PR. Os sete pontos abaixo têm nomes de marcos públicos reais e conhecidos da cidade; suas coordenadas são referências aproximadas para esses locais, não valores obtidos de uma API de geocodificação nem levantamento em campo. As distâncias entre eles continuam sendo calculadas por linha reta (fórmula de Haversine, mesma simplificação usada nos demais experimentos deste pacote — nenhuma chamada real ao OSRM é feita), e o tipo de via por trecho é modelado, não medido: trechos com mais de 3 km em linha reta têm maior probabilidade de ser tratados como ARTERIAL (avenida); os demais, maior probabilidade de ser URBANA (rua local) — sorteado independentemente por sentido, já que mão-única e trânsito assimétrico fazem o caminho de ida nem sempre ser igual ao de volta. Sem RODOVIA, propositalmente, já que o objetivo aqui é uma rota inteiramente intraurbana.

Os sete pontos (posição real) e o veículo/carga (seção 3) são fixos; a única coisa sorteada é o tipo de via por sentido. A seed usada aqui (**20260732**) foi escolhida testando até 200 seeds sequenciais e ficando com a primeira em que a rota do Cenário B realmente diverge da do Cenário A — mesmo critério que `EmpiricalCostComparisonExample` usa para seu exemplo sintético: um caso onde as duas rotas são iguais não ilustra nada sobre o efeito do custo empírico, mesmo sendo um resultado válido.

`AStarWaypointOptimizer` e `CostMatrixBuilder` são reaproveitados sem alteração — o mesmo algoritmo e as mesmas fórmulas de custo (tempo + combustível + desgaste de pneu) usados nos outros relatórios deste pacote.

## 2. Pontos avaliados

| # | Ponto | Latitude | Longitude |
|---|---|---|---|
| 0 | Prefeitura Municipal de Toledo | -24.7167 | -53.7333 |
| 1 | Catedral Sagrada Família | -24.7180 | -53.7365 |
| 2 | Terminal Rodoviário de Toledo | -24.7145 | -53.7290 |
| 3 | UNIOESTE — Campus Toledo | -24.6980 | -53.7410 |
| 4 | Shopping Toledo | -24.7300 | -53.7180 |
| 5 | Parque Ecológico de Toledo | -24.7050 | -53.7550 |
| 6 | C.Vale — Sede | -24.7400 | -53.7450 |

Ponto 0 (Prefeitura Municipal de Toledo) é o depósito/origem — ponto de partida e retorno, já que o modo de rota é ROUND_TRIP.

## 3. Perfil de veículo e carga

| Parâmetro | Valor |
|---|---|
| Veículo | Médio (3 eixos) |
| Eixos | 3 |
| Capacidade | 4000 kg / 25.0 m³ |
| Carga deste cenário | 2800 kg (70% peso) · 14.0 m³ (56% volume) |
| Restrição | Peso |
| Consumo base | 15.0 L/100km |
| Preço do litro | R$ 6.10 |
| Custo-hora do motorista | R$ 35.00 |

## 4. Rotas escolhidas

- **Cenário A (só duração, produção atual):** Prefeitura Municipal de Toledo → Terminal Rodoviário de Toledo → C.Vale — Sede → Parque Ecológico de Toledo → UNIOESTE — Campus Toledo → Shopping Toledo → Catedral Sagrada Família → Prefeitura Municipal de Toledo
- **Cenário B (tempo + combustível + desgaste de pneu):** Prefeitura Municipal de Toledo → Catedral Sagrada Família → Terminal Rodoviário de Toledo → Shopping Toledo → UNIOESTE — Campus Toledo → Parque Ecológico de Toledo → C.Vale — Sede → Prefeitura Municipal de Toledo
- Rotas diferentes? **Sim**

## 5. Métricas agregadas de cada rota

| Métrica | Rota do Cenário A | Rota do Cenário B |
|---|---|---|
| Distância total | 16.30 km | 15.99 km |
| Duração total | 24.8 min | 25.0 min |
| Combustível total | 3.20 L | 3.17 L |
| Desgaste de pneu | R$ 7.57 | R$ 7.52 |
| Custo total (fórmula combinada) | R$ 41.57 | R$ 41.44 |

**Gap:** seguir a rota do Cenário A custaria R$ 0.13 a mais (0.32%) do que a rota do Cenário B, avaliadas as duas sob a mesma fórmula de custo combinada.

## 6. Detalhamento por trecho — Cenário A

| De | Para | Tipo de via | Distância (km) | Duração (min) | Combustível (L) | Desgaste (R$) | Custo do trecho (R$) |
|---|---|---|---|---|---|---|---|
| Prefeitura Municipal de Toledo | Terminal Rodoviário de Toledo | URBANA | 0.50 | 1.2 | 0.12 | 0.29 | 1.71 |
| Terminal Rodoviário de Toledo | C.Vale — Sede | ARTERIAL | 3.26 | 3.9 | 0.59 | 1.37 | 7.27 |
| C.Vale — Sede | Parque Ecológico de Toledo | ARTERIAL | 4.02 | 4.8 | 0.73 | 1.69 | 8.95 |
| Parque Ecológico de Toledo | UNIOESTE — Campus Toledo | URBANA | 1.61 | 3.9 | 0.38 | 0.95 | 5.53 |
| UNIOESTE — Campus Toledo | Shopping Toledo | ARTERIAL | 4.25 | 5.1 | 0.77 | 1.78 | 9.46 |
| Shopping Toledo | Catedral Sagrada Família | URBANA | 2.30 | 5.5 | 0.54 | 1.35 | 7.87 |
| Catedral Sagrada Família | Prefeitura Municipal de Toledo | ARTERIAL | 0.35 | 0.4 | 0.06 | 0.15 | 0.79 |

## 7. Detalhamento por trecho — Cenário B

| De | Para | Tipo de via | Distância (km) | Duração (min) | Combustível (L) | Desgaste (R$) | Custo do trecho (R$) |
|---|---|---|---|---|---|---|---|
| Prefeitura Municipal de Toledo | Catedral Sagrada Família | URBANA | 0.35 | 0.8 | 0.08 | 0.21 | 1.21 |
| Catedral Sagrada Família | Terminal Rodoviário de Toledo | URBANA | 0.85 | 2.0 | 0.20 | 0.50 | 2.92 |
| Terminal Rodoviário de Toledo | Shopping Toledo | URBANA | 2.05 | 4.9 | 0.48 | 1.20 | 7.03 |
| Shopping Toledo | UNIOESTE — Campus Toledo | ARTERIAL | 4.25 | 5.1 | 0.77 | 1.78 | 9.46 |
| UNIOESTE — Campus Toledo | Parque Ecológico de Toledo | URBANA | 1.61 | 3.9 | 0.38 | 0.95 | 5.53 |
| Parque Ecológico de Toledo | C.Vale — Sede | ARTERIAL | 4.02 | 4.8 | 0.73 | 1.69 | 8.95 |
| C.Vale — Sede | Prefeitura Municipal de Toledo | ARTERIAL | 2.85 | 3.4 | 0.52 | 1.19 | 6.34 |

## 8. Conclusão

Neste cenário específico de Toledo-PR, considerar combustível e desgaste de pneu muda a ordem de visitação escolhida pelo algoritmo — a rota mais rápida (Cenário A) não é a de menor custo operacional (Cenário B).
Isolado a este exemplo, o gap de 0.32% é pequeno/moderado comparado à distribuição de 500 trials sintéticos dos outros relatórios deste pacote — o valor esperado para uma única rota real depende muito da geometria específica dos pontos e não deve ser generalizado sem repetir o experimento estatístico com geografia real.
