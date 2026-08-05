# Fórmulas — A* (tempo) e Cálculo de Custo Atual

Referência de código:
- `backend/src/main/java/com/routewise/algorithm/AStarWaypointOptimizer.java`
- `backend/src/main/java/com/routewise/algorithm/HaversineUtil.java`
- `backend/src/main/java/com/routewise/validation/CostMatrixBuilder.java`

**Notação comum**

Seja $N$ o número de waypoints, indexados $0, 1, \dots, N-1$ (o índice $0$ é a origem).
Seja $c_{ij}$ o custo (duração de viagem OSRM, em segundos) da aresta direcionada do
waypoint $i$ para o waypoint $j$, dado pela matriz de custo $N \times N$.

Um estado do A* é o par $(k, V)$, onde $k$ é o waypoint atual e $V \subseteq \{0,\dots,N-1\}$
é o conjunto de waypoints já visitados (representado como bitmask no código).

---

## 1. A* — otimização de rota (tempo)

### 1.1 Função de avaliação

$$
f(k, V) = g(k, V) + h(k, V)
$$

onde $g$ é o custo real acumulado desde a origem até o estado $(k,V)$ e $h$ é a
heurística admissível do custo restante.

### 1.2 Custo acumulado $g$

$$
g(0, \{0\}) = 0
$$

$$
g(j, V \cup \{j\}) = \min\Big( g(j, V \cup \{j\}),\; g(k, V) + c_{kj} \Big), \qquad j \notin V
$$

(relaxação de aresta padrão do A*: mantém-se o menor custo já encontrado para cada estado)

### 1.3 Heurística $h$ — admissível

Para cada waypoint $j$ ainda não visitado, define-se sua **menor aresta de entrada
global**:

$$
m_j = \min_{i \neq j} \, c_{ij}
$$

A heurística soma esse mínimo sobre todos os waypoints não visitados:

$$
h_{\text{base}}(k, V) = \sum_{j \notin V} m_j
$$

Para o modo `ROUND_TRIP`, soma-se ainda uma cota inferior otimista do custo de retorno
à origem ($0$):

$$
h_{\text{retorno}}(k, V) =
\begin{cases}
c_{k0} & \text{se } V = \{0,\dots,N-1\} \\[4pt]
\min\left( c_{k0},\; \min\limits_{i \notin V} c_{i0} \right) & \text{caso contrário}
\end{cases}
$$

$$
h(k, V) = h_{\text{base}}(k, V) + h_{\text{retorno}}(k, V) \qquad \text{(apenas quando o modo é ROUND\_TRIP)}
$$

**Admissibilidade:** $h$ nunca superestima o custo restante real, pois todo waypoint
não visitado precisa ser alcançado por *alguma* aresta de entrada, e $m_j$ é o piso
teórico (menor valor possível) dessa aresta.

### 1.4 Custo total da rota (estado objetivo)

Quando $V = \{0, \dots, N-1\}$ (todos visitados):

$$
\text{custoTotal} =
\begin{cases}
g(k, V) + c_{k0} & \text{se ROUND\_TRIP} \\
g(k, V) & \text{se OPEN\_ROUTE}
\end{cases}
$$

> **Nota de divergência:** o Javadoc da classe descreve a heurística como "distância de
> Haversine até o waypoint mais distante", mas a implementação real usa $\sum m_j$
> (seção 1.3). `HaversineUtil` é usada em outras partes do sistema (ex.: construção do
> grafo OSM em `OsmGraphService`), não dentro do heurístico do A*.

### 1.5 Complexidade

$$
O(N^2 \cdot 2^N)
$$

Espaço de estados $= N \times 2^N$ (máx. $10 \times 1024 = 10\,240$ para $N=10$, limite da UI).

---

## 2. Cálculo de custo atual (validação empírica — Cenário A vs. Cenário B)

Implementado em `CostMatrixBuilder`, usado nos experimentos de `com.routewise.validation`
para comparar o custo **só-tempo** (o que o A* de produção efetivamente minimiza) contra
um custo **operacional em R$** (combustível + pneus + tempo de motorista), por aresta
$(i,j)$. Seja $d_{ij}$ a distância Haversine (km) e $v_{ij}$ a velocidade média (km/h) do
tipo de via da aresta $(i,j)$.

### 2.1 Duração base (segundos)

$$
t_{ij} = \frac{d_{ij}}{v_{ij}} \times 3600
$$

### 2.2 Fator de carga (ocupação do veículo)

$$
\lambda = \min\left( \max\left( \frac{w_{\text{carga}}}{w_{\text{cap}}},\; \frac{vol_{\text{carga}}}{vol_{\text{cap}}} \right),\; 1 \right)
$$

isto é, o maior entre ocupação por peso e por volume, limitado ao intervalo $[0,1]$.

### 2.3 Combustível

$$
\text{consumo}_{ij} = C_{\text{base}} \times (1 + 0{,}30\,\lambda) \times \mu^{\text{fuel}}_{ij}
$$

$$
L_{ij} = d_{ij} \times \frac{\text{consumo}_{ij}}{100}
$$

onde $C_{\text{base}}$ é o consumo base do veículo (L/100km) e $\mu^{\text{fuel}}_{ij}$ o
multiplicador de consumo do tipo de via.

### 2.4 Desgaste de pneus

$$
\phi_{ij} = \frac{1}{K_{\text{pneu}}} \times n_{\text{eixos}} \times (1 + 0{,}50\,\lambda) \times \mu^{\text{wear}}_{ij}
$$

$$
W_{ij} = d_{ij} \times \phi_{ij} \times P_{\text{pneu}}
$$

onde $K_{\text{pneu}}$ é a vida útil do pneu (km), $n_{\text{eixos}}$ o número de eixos e
$P_{\text{pneu}}$ o custo de reposição por pneu (R\$).

### 2.5 Custo de tempo (motorista)

$$
T_{ij} = \frac{t_{ij}}{3600} \times S_{\text{motorista}}
$$

onde $S_{\text{motorista}}$ é o custo por hora do motorista (R\$/h).

### 2.6 Custo A — só tempo

$$
\text{custoA}_{ij} = t_{ij}
$$

(o que o A* de produção efetivamente minimiza — unidade: segundos)

### 2.7 Custo B — operacional total

$$
F_{ij} = L_{ij} \times P_{\text{combustível}}
$$

$$
\text{custoB}_{ij} = T_{ij} + F_{ij} + W_{ij}
$$

(unidade: R\$)

### 2.8 Constantes

| Símbolo | Constante no código | Valor |
|---|---|---|
| $0{,}30$ | `LOAD_FUEL_FACTOR` | 0.30 |
| $0{,}50$ | `LOAD_WEAR_FACTOR` | 0.50 |

---

## 3. Resumo comparativo

| | A* de produção | Validação — custoA | Validação — custoB |
|---|---|---|---|
| Minimiza | $c_{ij}$ = duração OSRM (s) | $t_{ij}$ (s) | R\$ (tempo + combustível + pneu) |
| Sensível à carga/veículo? | Não | Não | Sim ($\lambda$, perfil do veículo) |
| Uso | Pipeline real (`RouteController` → A*) | Baseline em `docs/validation-reports/` | Custo "realista" para validar se o A* só-tempo é boa proxy de custo operacional |
