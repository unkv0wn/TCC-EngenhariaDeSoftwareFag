# RouteWise — Revisão Matemática (para validação externa)

**Propósito deste documento:** reunir, com o máximo de precisão e detalhe, toda a
matemática usada no núcleo de otimização de rotas do RouteWise (TCC), para que um
segundo modelo (Gemini Pro) possa revisar a correção formal — admissibilidade da
heurística, corretude estatística, consistência dimensional das fórmulas de custo
empírico, e quaisquer inconsistências entre o que a documentação do código promete e
o que o código de fato faz.

Todas as fórmulas abaixo foram **transcritas diretamente do código-fonte** (não
parafraseadas) e cada uma cita o arquivo e, quando relevante, o número de linha. Onde
o código e a Javadoc discordam, ou onde uma alegação (ex.: "admissível", "degrada
graciosamente") não é comprovada, isso é sinalizado explicitamente na Seção 7.

Branch: `review-math-a-star`. Repositório: `TCC-EngenhariaDeSoftwareFag`.

---

## Sumário

1. [Visão geral do sistema](#1-visão-geral-do-sistema)
2. [Algoritmo A* de produção](#2-algoritmo-a-de-produção)
3. [Fórmula de Haversine](#3-fórmula-de-haversine)
4. [Integração com o OSRM](#4-integração-com-o-osrm)
5. [Modelo de custo empírico (`com.routewise.validation`)](#5-modelo-de-custo-empírico-comroutewisevalidation)
6. [Unidades usadas em todo o sistema](#6-unidades-usadas-em-todo-o-sistema)
7. [Perguntas específicas para a revisão](#7-perguntas-específicas-para-a-revisão)
8. [Índice de arquivos citados](#8-índice-de-arquivos-citados)

---

## 1. Visão geral do sistema

RouteWise resolve uma variante do Problema do Caixeiro Viajante (TSP) sobre 2–10
waypoints: dado um conjunto de pontos geográficos, encontrar a ordem de visita que
minimiza o custo total de deslocamento, com três modos de restrição de início/fim.

O pipeline de produção (`RouteOptimizerServiceImpl.compute`) é:

$$
\text{waypoints} \xrightarrow{\text{OSRM /table}} \text{matriz } N\times N \text{ de durações (s)} \xrightarrow{\text{A*}} \text{ordem ótima} \xrightarrow{\text{OSRM /route}} \text{geometria}
$$

Apenas a **matriz de duração** (não distância) é usada como custo de aresta em
produção — isto é: **o sistema hoje minimiza apenas tempo de viagem**, não uma
combinação de tempo/combustível/pedágio/desgaste. Um modelo de custo combinado em R$
existe, mas apenas dentro do pacote `com.routewise.validation`, isolado da produção
(ver Seção 5 e Seção 7).

---

## 2. Algoritmo A* de produção

**Arquivo:** `backend/src/main/java/com/routewise/algorithm/AStarWaypointOptimizer.java`
**Classe:** `AStarWaypointOptimizer` (`@Component`)

### 2.1 Espaço de estados

Estado = `(currentWaypointIndex, visitedBitmask)`, onde `visitedBitmask` é uma
máscara de bits de $n$ posições (um bit por waypoint já visitado).

$$
\text{Estado} = (i,\ V), \quad i \in \{0,\dots,n-1\},\ V \in \{0,\dots,2^n-1\}
$$

$$
|\text{Estados}| = n \cdot 2^n \quad \xrightarrow{n=10} \quad 10 \cdot 1024 = 10{,}240
$$

$n \le 10$ é imposto na borda da API (`RouteRequestDto.java` linha 19:
`@Size(min = 2, max = 10)`). Complexidade de tempo/espaço: $O(n^2 \cdot 2^n)$.

Caso especial: $n=1$ retorna `[0]` imediatamente, sem rodar o A* (linhas 58–60).

### 2.2 Custo acumulado $g$

$$
g(\text{next}, V \cup \{\text{next}\}) = g(\text{current}, V) + \text{costMatrix}[\text{current}][\text{next}]
$$

`costMatrix[i][j]` é, em produção, **a duração de viagem em segundos** retornada
pelo OSRM `/table` (ver Seção 4.1) — não distância, não uma combinação ponderada.
Código (linha 134): `double nextG = gCost + costMatrix[current][next];`

### 2.3 Heurística $h$ (transcrição literal, linhas 167–198)

```java
private double heuristic(
  int current, int visited, double[][] costs,
  double[] minIncoming, int n, int allVisited, boolean roundTrip
) {
  double h = 0;

  // Add min incoming edge for every unvisited node
  for (int i = 0; i < n; i++) {
    if ((visited & (1 << i)) == 0) {
      h += minIncoming[i];
    }
  }

  // For ROUND_TRIP: add a lower bound for the eventual return to origin
  if (roundTrip) {
    if (visited == allVisited) {
      h += costs[current][0];
    } else {
      double minReturn = costs[current][0];
      for (int i = 0; i < n; i++) {
        if ((visited & (1 << i)) == 0 && costs[i][0] < minReturn) {
          minReturn = costs[i][0];
        }
      }
      h += minReturn;
    }
  }

  return h;
}
```

Em notação fechada:

$$
h(\text{current}, V) = \underbrace{\sum_{k \notin V} \text{minIncoming}[k]}_{\text{soma sobre não-visitados}} \;+\; \mathbb{1}_{\text{roundTrip}} \cdot r(\text{current}, V)
$$

onde

$$
\text{minIncoming}[j] = \min_{i \neq j} \text{costs}[i][j]
$$

(mínimo global de aresta **entrando** em $j$, pré-computado uma vez por chamada em
`computeMinIncoming`, linhas 200–217; se $j$ não tem nenhuma aresta de entrada,
`minIncoming[j] := 0` como guarda — linha 212–214), e o termo de retorno (somente
quando `roundTrip = true`, i.e. `RouteMode.ROUND_TRIP`):

$$
r(\text{current}, V) =
\begin{cases}
\text{costs}[\text{current}][0] & \text{se } V = \text{allVisited} \\[4pt]
\min\Big(\text{costs}[\text{current}][0],\ \min\limits_{k \notin V} \text{costs}[k][0]\Big) & \text{caso contrário}
\end{cases}
$$

**Alegação de admissibilidade, transcrita da Javadoc da classe (linhas 29–35):**

> "For each unvisited waypoint, add the minimum global incoming edge weight from
> the cost matrix. This never overestimates the remaining cost because: (1) Each
> unvisited node must be entered at some point. (2) The cheapest possible entry is
> its global minimum incoming edge."

Esta é a relaxação clássica "soma das menores arestas de entrada" para TSP
assimétrico. **Não há nenhuma prova de consistência** (só de admissibilidade) em
lugar nenhum do código ou da documentação — ver pergunta 7.1.

### 2.4 $f(n) = g(n) + h(n)$ e a fila de prioridade

```java
PriorityQueue<double[]> openSet = new PriorityQueue<>(
  Comparator.comparingDouble(s -> s[2] + s[3])   // s[2]=g, s[3]=h
);
```

Prioridade = $f = g+h$ padrão. **Sem regra de desempate explícita** quando dois
estados têm o mesmo $f$ — `PriorityQueue` do Java usa apenas a ordem heap-interna,
não determinística entre elementos de prioridade igual.

**Deleção preguiçosa (lazy deletion), não um closed-set verdadeiro:**

```java
if (gCost > bestG[current][visited] + 1e-9) {
  continue;
}
```

`bestG[i][V]` mantém o melhor $g$ conhecido por estado; entradas obsoletas na fila
são simplesmente ignoradas ao serem retiradas, com tolerância de ponto flutuante de
$10^{-9}$.

### 2.5 Teste de objetivo

$$
\text{objetivo atingido} \iff V = \text{allVisited} = 2^n - 1
$$

idêntico nos três modos. Ao atingir o objetivo:

$$
\text{total} = g(\text{current}, V) + \mathbb{1}_{\text{roundTrip}} \cdot \text{costMatrix}[\text{current}][0]
$$

(linha 114 — usado apenas para log, o caminho retornado vem de `reconstructPath`).

### 2.6 Semântica dos três `RouteMode` (`backend/src/main/java/com/routewise/model/RouteMode.java`)

| Modo | `roundTrip` (interno) | Teste de objetivo | Heurística | Restrição na expansão |
|---|---|---|---|---|
| `ROUND_TRIP` | `true` | $V=$ allVisited | soma + termo de retorno $r$ | nenhuma |
| `OPEN_ROUTE` | `false` | $V=$ allVisited | só a soma | nenhuma |
| `FIXED_START_END` | `false` | $V=$ allVisited | **igual a OPEN_ROUTE** (sem termo específico) | ver abaixo |

`FIXED_START_END` não altera a heurística nem o teste de objetivo — só adiciona uma
guarda na expansão de vizinhos (linhas 128–131):

```java
// For FIXED_START_END, do not visit the destination (n-1) until all other nodes are visited
if (mode == RouteMode.FIXED_START_END && next == n - 1 && visited != (allVisited ^ (1 << (n - 1)))) {
  continue;
}
```

Ou seja: o waypoint de índice $n-1$ só pode ser expandido quando ele é o **único**
nó não-visitado restante ($V = \text{allVisited} \oplus 2^{n-1}$). Isso garante que
$n-1$ seja sempre o último parado; o primeiro é sempre $0$ por construção do estado
inicial (`startVisited = 1`, i.e. bit 0 setado). Não existe nenhuma outra checagem
de "deve terminar exatamente em $n-1$" — a garantia vem inteiramente dessa poda na
expansão, combinada com o teste de objetivo padrão.

Reconstrução de caminho (`reconstructPath`, linhas 220–245): segue ponteiros de pai
de trás para frente; para `ROUND_TRIP`, acrescenta `0` ao final da lista antes de
retornar (fecha o laço).

### 2.7 Fallback (não deveria ocorrer nunca)

Se a fila de prioridade esvaziar sem atingir o objetivo (matriz de custo completa e
finita — teoricamente impossível), retorna a ordem sequencial trivial `[0,1,...,n-1]`
(+ `0` se `roundTrip`), com um log de aviso (linhas 147–156).

---

## 3. Fórmula de Haversine

**Arquivo:** `backend/src/main/java/com/routewise/algorithm/HaversineUtil.java`

$$
R = 6371.0 \text{ km (raio da Terra, constante)}
$$

$$
\Delta\varphi = \text{rad}(\varphi_2-\varphi_1), \qquad \Delta\lambda = \text{rad}(\lambda_2-\lambda_1)
$$

$$
a = \sin^2\!\left(\frac{\Delta\varphi}{2}\right) + \cos(\text{rad}(\varphi_1)) \cdot \cos(\text{rad}(\varphi_2)) \cdot \sin^2\!\left(\frac{\Delta\lambda}{2}\right)
$$

$$
c = 2 \cdot \operatorname{atan2}\!\left(\sqrt{a},\ \sqrt{1-a}\right)
$$

$$
d_{\text{km}} = R \cdot c
$$

Fórmula de Haversine padrão, sem simplificações. Retorna quilômetros;
`toMetres(km) = km * 1000.0` converte quando necessário.

**Achado importante:** apesar da Javadoc da classe afirmar
*"The Haversine distance is an admissible heuristic for A* on road graphs because
road distance is always ≥ straight-line distance"*, **`HaversineUtil` não é usado
em nenhum lugar dentro de `AStarWaypointOptimizer`** — a heurística de produção é
inteiramente baseada na matriz de custo (`minIncoming`, Seção 2.3), não em
geografia. `HaversineUtil` é de fato usado em: (a) `OsmGraphService` (snapping de
nó mais próximo no grafo OSM — recurso desacoplado da matriz de custo do A*), e (b)
extensivamente dentro de `com.routewise.validation`, para sintetizar matrizes de
distância em linha reta nos experimentos (não há chamadas OSRM reais nos
experimentos). Ver pergunta 7.2.

---

## 4. Integração com o OSRM

**Interface:** `backend/src/main/java/com/routewise/service/IOsrmClient.java`
**Implementação:** `backend/src/main/java/com/routewise/service/impl/OsrmClientServiceImpl.java`

### 4.1 `/table` — matriz de duração (alimenta o A*)

```
GET {osrmBaseUrl}/table/v1/driving/{lng,lat;lng,lat;...}?annotations=duration
```

Retorna `OsrmTableResponse.durations()`, uma matriz `double[N][N]` em **segundos**.
Só `annotations=duration` é pedido — **nenhuma matriz de distância é buscada do
OSRM em produção**. `RouteOptimizerServiceImpl` passa
`tableResponse.durations()` direto como `costMatrix` para
`AStarWaypointOptimizer.optimize()` (linha 90 e 95 de
`RouteOptimizerServiceImpl.java`), sem nenhuma transformação.

### 4.2 `/route` — geometria final (não influencia a otimização)

```
GET {osrmBaseUrl}/route/v1/driving/{lng,lat;...}?overview=full&geometries=geojson&steps=true
```

Chamado **depois** que o A* já decidiu a ordem — usado só para desenhar a rota e
quebrar em segmentos por trecho (`RouteResultDto.segments`). Coordenadas enviadas
ao OSRM em ordem `lng,lat` (invertida em relação ao `WaypointDto` interno, que é
`lat,lng`), formatadas com `Locale.US` (`"%.6f,%.6f"`) para evitar que a vírgula
decimal do `pt_BR` corrompa a URL.

### 4.3 Degradação "graciosa" — e a lacuna real

Javadoc da classe `RouteOptimizerServiceImpl` (linhas 39–41):

> "If the OSRM validation call (step 5) fails, the system degrades gracefully: the
> result still contains the A*-optimised order but no route geometry from OSRM and
> the `osrmValidation` field is set to `UNAVAILABLE`."

Isso é **verdade apenas para a chamada `/route` (passo 5)**. Confirmado no código
(`fetchOsrmRouteAndBuildResult`, linhas 155–173): só ali existe
`catch (OsrmClientException ex)`, preservando a ordem do A* e zerando
`geometry`/`segments`.

A chamada `/table` (passo 2, linha 89 de `compute()`) **não tem tratamento
específico**:

```java
OsrmTableResponse tableResponse = osrmClient.fetchDurationMatrix(request.waypoints());
```

Se essa chamada lançar `OsrmClientException`, a exceção sobe direto para o
`catch (Exception ex)` genérico (linhas 125–132), que **aborta todo o cálculo** —
emite um evento SSE de erro e encerra, sem calcular rota nenhuma. Ou seja: **a
degradação graciosa documentada cobre só metade do pipeline** — a chamada que
realmente alimenta a otimização (`/table`) não tem fallback nenhum (nem
Haversine, nem constante de velocidade fixa). Ver pergunta 7.3.

---

## 5. Modelo de custo empírico (`com.routewise.validation`)

Este pacote inteiro é **desacoplado da produção** — nenhuma classe aqui é chamada
por `RouteController`/`RouteOptimizerServiceImpl`. Todos os experimentos reutilizam
`AStarWaypointOptimizer` sem modificação, e existem só para validar/explorar um
modelo de custo alternativo, em R$, para a tese. Documentação de design em
`docs/superpowers/specs/2026-07-29-empirical-cost-validation-design.md` e
seguintes; resultados em `docs/refactor-math-result/*.md` e
`docs/validation-reports/*.md` (regenerados a cada execução).

### 5.1 Fórmula combinada por aresta $(i,j)$

**Arquivo:** `backend/src/main/java/com/routewise/validation/CostMatrixBuilder.java`

Constantes: `LOAD_FUEL_FACTOR = 0.30`, `LOAD_WEAR_FACTOR = 0.50`.

$$
\text{durationSec}_{ij} = \frac{\text{distanceKm}_{ij}}{\text{roadType}_{ij}.\text{avgSpeedKmh}} \cdot 3600
$$

$$
\text{loadFactor} = \min\!\Big(\max\big(\tfrac{\text{cargoWeightKg}}{\text{capacityKg}},\ \tfrac{\text{cargoVolumeM}^3}{\text{capacityM}^3}\big),\ 1\Big)
$$

(ver Seção 5.3 — mesmo por trás da fórmula, `loadFactor` é constante por cenário,
não por aresta, já que carga não muda ao longo da rota)

$$
\text{consumoAjustado} = \text{baseFuelConsumptionLPer100Km} \cdot (1 + 0.30 \cdot \text{loadFactor}) \cdot \text{roadType}_{ij}.\text{fuelMultiplier}
$$

$$
\text{fuelLiters}_{ij} = \text{distanceKm}_{ij} \cdot \frac{\text{consumoAjustado}}{100}
$$

$$
\text{tireWearFraction}_{ij} = \Big(\underbrace{\sum_{p \,\in\, \{\text{DIANTEIRO},\,\text{TRACAO},\,\text{REBOQUE}\}} \frac{\text{tireCount}(p)}{\text{tireLifeKm} \cdot \text{lifeFactor}(p)}}_{\text{baseTireWearPerKm — só depende do perfil, calculado 1x}}\Big) \cdot (1 + 0.50 \cdot \text{loadFactor}) \cdot \text{roadType}_{ij}.\text{wearMultiplier}
$$

$$
\text{tireWearReais}_{ij} = \text{distanceKm}_{ij} \cdot \text{tireWearFraction}_{ij} \cdot \text{tireReplacementCostPerTire}
$$

$$
\text{timeCostReais}_{ij} = \frac{\text{durationSec}_{ij}}{3600} \cdot \text{driverCostPerHourReais}
$$

$$
\text{fuelCostReais}_{ij} = \text{fuelLiters}_{ij} \cdot \text{fuelPricePerLiter}
$$

$$
\boxed{\text{costA}_{ij} = \text{durationSec}_{ij}} \qquad
\boxed{\text{costB}_{ij} = \text{timeCostReais}_{ij} + \text{fuelCostReais}_{ij} + \text{tireWearReais}_{ij}}
$$

"Cenário A" = idêntico ao que produção já minimiza hoje (tempo puro). "Cenário B" =
soma ponderada implicitamente $(1,1,1)$, já que todos os três termos estão em R$.
Este é o **único** lugar do código onde os pesos aparecem explicitados como
variáveis ajustáveis, fora deste arquivo — ver Seção 5.9.

### 5.2 Constantes por classe de veículo

**Arquivo:** `backend/src/main/java/com/routewise/validation/VehicleProfile.java`
(`record`) — Javadoc explicita: *"These are not measured fleet data — no real
telemetry is available at this stage. They are documented, plausible constants."*

| Perfil | eixos | layout (dianteiro,tração,reboque) | capacityKg | capacityM³ | baseFuelConsumptionLPer100Km | fuelPricePerLiter | tireReplacementCostPerTire | tireLifeKm | driverCostPerHourReais |
|---|---|---|---|---|---|---|---|---|---|
| `light()` | 2 | (2,4,0) | 1500.0 | 8.0 | 10.0 | 6.10 | 900.0 | 50 000.0 | 30.0 |
| `medium()` (= `defaultProfile()`) | 3 | (2,8,0) | 4000.0 | 25.0 | 15.0 | 6.10 | 1800.0 | 60 000.0 | 35.0 |
| `heavy()` | 5 | (2,8,8) | 12000.0 | 90.0 | 32.0 | 6.10 | 2200.0 | 80 000.0 | 42.0 |

`tireLifeKm` é definido na posição `TRACAO` (fator 1.0, referência).

**Tipos de via** (`RoadType.java`) — `(avgSpeedKmh, fuelMultiplier, wearMultiplier)`:

| Tipo | avgSpeedKmh | fuelMultiplier | wearMultiplier |
|---|---|---|---|
| `RODOVIA` | 80.0 | 0.85 | 0.8 |
| `ARTERIAL` | 50.0 | 1.0 | 1.0 |
| `URBANA` | 25.0 | 1.3 | 1.4 |

**Posição de eixo** (`AxlePosition.java`) — `lifeFactor` (multiplicador sobre
`tireLifeKm`, referência = `TRACAO`):

| Posição | lifeFactor | Justificativa (Javadoc) |
|---|---|---|
| `DIANTEIRO` | 0.85 | desgaste lateral em curvas + sensibilidade a alinhamento |
| `TRACAO` | 1.00 | referência — o log empírico é quase todo eventos de eixo de tração |
| `REBOQUE` | 1.25 | carrega peso sem torque nem esterçamento |

**Contagem de pneus por posição** (`AxleLayout.java`):

| Layout | dianteiro | tração | reboque | total |
|---|---|---|---|---|
| `light()` | 2 | 4 | 0 | 6 |
| `medium()` | 2 | 8 | 0 | 10 |
| `heavy()` | 2 | 8 | 8 | 18 |

**Nota histórica documentada no próprio código:** a fórmula antiga usava
`axleCount` (número de eixos, ex. 3) como proxy para quantidade de pneus, o que
subestimava o custo de desgaste em ~3x (um caminhão de 3 eixos roda em 10 pneus,
não 3). A versão atual (soma por posição, acima) foi implementada na branch
`review-math-a-star`; ver `docs/refactor-math-result/04-desgaste-pneu-por-eixo.md`
para a tabela numérica antes/depois (ex.: perfil médio, R$9,00 → R$31,06 por 100 km
em via ARTERIAL vazio — 3,45x).

### 5.3 Ocupação de carga (peso × cubagem)

**Arquivo:** `backend/src/main/java/com/routewise/validation/CargoOccupancy.java`

$$
\text{weightFraction} = \frac{\text{cargoWeightKg}}{\text{capacityKg}} \quad (\text{não limitado a} \le 1)
$$

$$
\text{volumeFraction} = \frac{\text{cargoVolumeM}^3}{\text{capacityM}^3} \quad (\text{não limitado a} \le 1)
$$

$$
\text{effectiveLoadFactor} = \min\big(\max(\text{weightFraction},\ \text{volumeFraction}),\ 1\big)
$$

$$
\text{volumeBound} = (\text{volumeFraction} > \text{weightFraction})
$$

$$
\text{feasible} = \big(\max(\text{weightFraction},\ \text{volumeFraction}) \le 1\big)
$$

### 5.4 Calibração empírica de consumo de combustível

**Arquivo:** `backend/src/main/java/com/routewise/validation/EmpiricalFuelConsumptionCalculator.java`

Janela móvel de tamanho `WINDOW_SIZE = 5`, **média ponderada por distância** (não
média das razões por abastecimento):

$$
\text{consumptionLPer100Km} =
\begin{cases}
\text{fallback} & \text{se log vazio} \\[6pt]
\dfrac{\displaystyle\sum_{k=1}^{\min(5,\,|\text{log}|)} \text{litersRefueled}_k}{\displaystyle\sum_{k=1}^{\min(5,\,|\text{log}|)} \text{kmSincePrevious}_k} \cdot 100 & \text{caso contrário}
\end{cases}
$$

A janela pega os **últimos** $\min(5, N)$ registros (não os primeiros). Comentário
no código: "a tank that covered more km should count for more" — soma numeradores e
denominadores antes de dividir, em vez de tirar a média das razões individuais por
abastecimento.

### 5.5 Calibração empírica de vida útil de pneu

**Arquivo:** `backend/src/main/java/com/routewise/validation/EmpiricalTireLifeCalculator.java`

Janela móvel de tamanho `WINDOW_SIZE = 2`, **média simples (não ponderada)**,
filtrando antes de aplicar a janela:

$$
\text{wearEvents} = \{\, r \in \text{log} : r.\text{motivoTroca} = \text{DESGASTE\_NORMAL} \,\}
$$

$$
\text{tireLifeKm} =
\begin{cases}
\text{fallback} & \text{se wearEvents vazio} \\[6pt]
\dfrac{1}{\min(2,|\text{wearEvents}|)} \displaystyle\sum_{k=1}^{\min(2,|\text{wearEvents}|)} \text{kmUsado}_k & \text{caso contrário}
\end{cases}
$$

onde $\text{kmUsado} = \text{kmTroca} - \text{kmInstalacao}$. Eventos com motivo
`DANO_ACIDENTE` são excluídos **estruturalmente** antes de qualquer cálculo de
janela (não entram nem na contagem $N$). Agrupamento por
`(classeVeiculo, posicaoEixo)` é responsabilidade de quem chama, não do
calculador.

### 5.6 Aparato estatístico

**Arquivo:** `backend/src/main/java/com/routewise/validation/StatisticalAnalyzer.java`
(usa `org.apache.commons.math3`)

Dado um conjunto de $N$ trials Monte Carlo (tipicamente $N=500$, seed fixa 42):

**Gap por trial** (`TrialResultBuilder`, referenciado mas não citado por completo
acima — usado por `StatisticalAnalyzer`):

$$
\text{gapReais} = \text{costB}_{\text{sob ordem A}} - \text{costB}_{\text{sob ordem B}} \quad (\ge 0 \text{ por construção, já que ordem B minimiza costB})
$$

$$
\text{gapPercent} = 100 \cdot \frac{\text{gapReais}}{\text{costB}_{\text{sob ordem A}}} \quad (\text{ou } 0 \text{ se o denominador} \le 0)
$$

**Estatística descritiva** (`DescriptiveStatistics` do commons-math3): média,
`getPercentile(50)` (mediana), desvio padrão, mínimo, máximo — aplicada
separadamente a `gapReais[]` e `gapPercent[]`.

**Teste de Wilcoxon (postos sinalizados), pareado:**

```java
private static double[] runWilcoxon(double[] a, double[] b) {
  // filtra pares onde a[i] == b[i] (empate exato) ANTES do teste
  ...
  WilcoxonSignedRankTest test = new WilcoxonSignedRankTest();
  double statistic = test.wilcoxonSignedRank(x, y);
  double pValue = test.wilcoxonSignedRankTest(x, y, false);
  return new double[]{statistic, pValue};
}
```

- Pares com diferença exatamente zero são **excluídos antes** de rodar o teste
  (`a[i] != b[i]`), não durante — política padrão para Wilcoxon, já que um empate em
  zero não carrega informação sobre qual cenário é mais barato.
- Se sobrarem menos de 2 pares não-nulos, retorna `[NaN, NaN]` (teste não é
  significativo).
- O terceiro argumento `false` em `wilcoxonSignedRankTest(x, y, false)` = **não**
  forçar distribuição exata — usa aproximação normal para amostra grande.
- Rodado sobre os vetores pareados `costBUnderOrderA[]` vs. `costBUnderOrderB[]`
  (custo em R$ de cada cenário avaliado sob sua própria ordem ótima).

**Correlação de Spearman** (`SpearmansCorrelation`) entre `gapPercent[]` e cada uma
das variáveis explicativas candidatas: `weightFraction`, `volumeFraction`,
`occupancyFraction`, `urbanFraction`, `n` (número de waypoints).

**Quebra por número de waypoints** (`byWaypointCount`): agrupa por $n$ exato,
reporta `trialCount`, `% rotas diferem`, `gapPercent` médio, por grupo.

**Quebra por nível de ocupação** (`byOccupancyLevel`): buckets fixos por limite
superior $\{0.25,\ 0.50,\ 0.75,\ 1.00,\ \infty\}$, rótulos
`"0–25%","25–50%","50–75%","75–100%","> 100% (inviável)"`. Note que o bucket
"> 100%" existe porque `CargoOccupancy.feasible` pode ser `false` (carga excede
capacidade), mas **o algoritmo não rejeita nem sinaliza isso automaticamente** — é
só reportado no bucket, não usado como restrição dura em lugar nenhum do A* ou do
`CostMatrixBuilder`.

### 5.7 Análise de contribuição marginal

**Arquivo:** `backend/src/main/java/com/routewise/validation/MarginalContributionAnalyzer.java`

Responde "se eu adicionar esta variável nova à função de custo, ela realmente ajuda
ou é ruído/redundante?" — generalização do teste Cenário A vs. B para qualquer
candidato futuro.

$$
\text{extendedCostB}_{ij} = \text{costB}_{ij} + \text{candidateComponentReais}_{ij}
$$

$$
\text{gap}_k = \text{costUnderBaseRoute}_k - \text{costUnderExtendedRoute}_k \quad \text{(ambos avaliados sob extendedCostB)}
$$

$$
\text{gapReaisMean} = \frac{1}{N}\sum_k \text{gap}_k \qquad
\text{gapPercentMean} = \frac{1}{|\{k : \text{costUnderBaseRoute}_k>0\}|} \sum_{k\,:\,\text{costUnderBaseRoute}_k>0} 100\cdot\frac{\text{gap}_k}{\text{costUnderBaseRoute}_k}
$$

Wilcoxon pareado sobre `(costUnderBaseRoute[], costUnderExtendedRoute[])` (mesma
política de exclusão de zeros da Seção 5.6). Correlação de Spearman entre
`candidateTotalOnBaseRoute[]` e `baseTotalOnBaseRoute[]` (custo do componente
candidato vs. custo base, ambos avaliados ao longo da rota-base — mede se o
candidato está essencialmente medindo a mesma coisa que o custo já existente).

**Limiares de veredito** (constantes `MIN_PCT_ROUTES_DIFFER_TO_MATTER = 5.0`,
`REDUNDANT_CORRELATION_THRESHOLD = 0.9`):

$$
\text{significant} = (\text{wilcoxonP} \neq \text{NaN}) \wedge (\text{wilcoxonP} < 0.05)
$$

$$
\text{meaningfulChange} = (\text{pctRoutesDiffer} \ge 5.0) \wedge \text{significant}
$$

$$
\text{redundant} = |\text{correlation}| \ge 0.9
$$

$$
\text{verdict} =
\begin{cases}
\text{"NEGLIGENCIÁVEL"} & \neg\,\text{meaningfulChange} \\
\text{"REDUNDANTE"} & \text{meaningfulChange} \wedge \text{redundant} \\
\text{"AGREGA VALOR"} & \text{meaningfulChange} \wedge \neg\,\text{redundant}
\end{cases}
$$

Dois exemplos concretos de `candidateComponentReais` já implementados:

- **Pedágio** (`TollCostAdditionExample.java`):
  $$\text{tollCostReais}_{ij} = \text{distanceKm}_{ij} \cdot 0.05 \cdot \text{profile.axleCount()} \quad \text{(só em arestas RODOVIA)}$$
  (`TOLL_RATE_PER_KM_PER_AXLE = 0.05` R$/km/eixo — "assunção documentada,
  estilo-ANTT", não calibrada.)
- **Indicador de risco** (`NewIndicatorAdditionExample.java`): custo fixo,
  independente de distância, por tipo de via — `RODOVIA→2.0, ARTERIAL→5.0,
  URBANA→15.0` (R$, placeholder sintético para demonstrar um sinal genuinamente
  independente da distância).

### 5.8 Experimento de sensibilidade de pesos

**Arquivo:** `backend/src/main/java/com/routewise/validation/CostWeightSensitivityExperiment.java`

Generaliza `costB` para uma soma ponderada explícita:

$$
\text{customCostB}_{ij} = \alpha \cdot \text{timeCostReais}_{ij} + \beta \cdot \text{fuelCostReais}_{ij} + \gamma \cdot \text{tireWearReais}_{ij}
$$

Configurações de peso testadas (500 cenários, seed 42, `VehicleProfile.medium()`):

| Configuração | $\alpha$ (tempo) | $\beta$ (combustível) | $\gamma$ (pneu) |
|---|---|---|---|
| Só tempo (produção atual) | 1.0 | 0.0 | 0.0 |
| Equilibrado (1,1,1) — modelo atual | 1.0 | 1.0 | 1.0 |
| Prioriza combustível | 0.3 | 2.0 | 0.3 |
| Prioriza desgaste de pneu | 0.3 | 0.3 | 2.0 |
| Só combustível | 0.0 | 1.0 | 0.0 |
| Só desgaste de pneu | 0.0 | 0.0 | 1.0 |

Pesos **não calibrados** — escolhidos manualmente para análise de sensibilidade,
não ajustados a dados reais.

### 5.9 Geração de cenários sintéticos

**Arquivo:** `backend/src/main/java/com/routewise/validation/ScenarioGenerator.java`

```
CENTER_LAT = -23.5505, CENTER_LNG = -46.6333   (São Paulo)
SPREAD_DEGREES = 0.35                           (raio ~35–40 km)
n ∈ [2, 10], uniforme
waypoint.lat = CENTER_LAT + (rand()*2-1) * SPREAD_DEGREES   (idem para lng)
distanceKm[i][j] = HaversineUtil.distanceKm(...)   (simétrica — simplificação documentada na Javadoc de Scenario)
roadType[i][j] = uniforme aleatório em {RODOVIA, ARTERIAL, URBANA}, amostrado independentemente por direção (i,j) ≠ (j,i)
cargoWeightKg ∈ [50.0, 4500.0], uniforme
cargoVolumeM3 ∈ [0.3, 28.0], uniforme
```

### 5.10 Exemplo real — comparação em Toledo/PR

**Arquivo:** `backend/src/main/java/com/routewise/validation/ToledoRouteComparisonExample.java`

Não é um experimento estatístico — é um cenário único fixo com 7 pontos de
referência reais em Toledo, PR (Prefeitura, Catedral, Terminal Rodoviário,
UNIOESTE, Shopping Toledo, Parque Ecológico, C.Vale), matriz de distância via
Haversine (não distância real de via), `RouteMode.ROUND_TRIP`,
`VehicleProfile.medium()`, carga 2800 kg / 14.0 m³. Tipo de via sorteado por
direção com limiar de distância (`ARTERIAL_THRESHOLD_KM = 3.0`): arestas > 3 km em
linha reta têm 85% de chance de `ARTERIAL` (senão 15%); arestas mais curtas têm 15%
de chance de `ARTERIAL` (senão `URBANA`). Uma busca de seed
(`SEED_BASE = 20260730L`, até 200 tentativas) escolhe a primeira seed em que
Cenário A e Cenário B escolhem ordens diferentes, só para tornar o exemplo
não-trivial.

**Nota de nomenclatura:** "Toledo" aqui é a cidade de Toledo, Paraná — não há
nenhum "modelo de Toledo" acadêmico/econométrico citado em lugar nenhum do
código.

---

## 6. Unidades usadas em todo o sistema

| Grandeza | Unidade | Onde |
|---|---|---|
| Distância | km | `distanceKm` em todo o pacote `validation`; OSRM retorna metros, convertido por `/1000.0` em `RouteOptimizerServiceImpl` |
| Duração | segundos internamente; minutos ao expor pro usuário | `durationSec`/`costMatrix` (s); `/60.0` em `RouteOptimizerServiceImpl` (min) |
| Combustível | litros (quantidade); L/100km (taxa de consumo) | `fuelLiters`, `baseFuelConsumptionLPer100Km` |
| Dinheiro | R$ (reais) | `fuelPricePerLiter`, `tireReplacementCostPerTire`, `driverCostPerHourReais`, `TOLL_RATE_PER_KM_PER_AXLE`, todos os campos `*Reais` |
| Carga | kg (peso), m³ (volume) | `cargoWeightKg`, `cargoVolumeM3`, `capacityKg`, `capacityM3` |
| Vida útil de pneu | km | `tireLifeKm` |

---

## 7. Perguntas específicas para a revisão

Pontos onde uma segunda revisão matemática é especialmente bem-vinda:

**7.1 — Consistência da heurística.** A Javadoc só alega **admissibilidade**
(nunca superestima), nunca **consistência** (monotonicidade: $h(n) \le c(n,n') +
h(n')$ para todo sucessor $n'$). O código usa deleção preguiçosa em vez de um
closed-set formal (Seção 2.4), o que é seguro sob admissibilidade pura mas
depende de reabrir estados quando um caminho melhor é achado depois — isso está
correto do jeito que está implementado (`bestG` permite reabertura), mas gostaria
de confirmação de que a heurística é de fato admissível (idealmente também
consistente, o que tornaria a primeira expansão de cada estado já ótima) dado o
termo extra de retorno somado em `roundTrip` (Seção 2.3) — ele pode, em algum
caso extremo, fazer $h$ ultrapassar o custo real restante?

**7.2 — Heurística "morta".** A Javadoc da classe `AStarWaypointOptimizer` (linha
29) chama a heurística de "admissible" citando a lógica de menor-aresta-de-entrada,
e a Javadoc de `HaversineUtil` (linha 7) alega que Haversine "is an admissible
heuristic for A* on road graphs" — mas `HaversineUtil` nunca é chamado dentro de
`AStarWaypointOptimizer`. As duas alegações de admissibilidade parecem
coincidentalmente verdadeiras cada uma isoladamente, mas a segunda é sobre um
código morto (não conectado). Isso é intencional (a real heurística usada é só a
de mínima-aresta-de-entrada) ou é vestígio de um design anterior?

**7.3 — Lacuna na degradação graciosa.** Confirmado no código
(`RouteOptimizerServiceImpl.compute`, linha 89): a chamada OSRM `/table` (que
alimenta o `costMatrix` do A*) não tem tratamento de exceção específico — falha
ali aborta o cálculo inteiro, apesar da Javadoc da classe (linhas 39–41) afirmar
genericamente que "the system degrades gracefully" sem especificar que isso vale
só para a chamada `/route` (geometria). Isso é um problema de
documentação-desatualizada-com-o-código, ou um gap funcional real que merece um
fallback (Haversine + velocidade média constante, por exemplo)?

**7.4 — `FIXED_START_END` sem heurística dedicada e sem teste.** A restrição de
FIXED_START_END é implementada só como uma poda na expansão (proibir visitar
$n-1$ cedo demais), reaproveitando a heurística de OPEN_ROUTE sem ajuste. A poda
em si preserva admissibilidade (só reduz o espaço de busca, nunca subestima
menos que antes), mas: (a) não há nenhum teste unitário cobrindo esse modo
(`AStarWaypointOptimizerTest.java` só testa OPEN_ROUTE e ROUND_TRIP); (b) vale
perguntar se a heurística poderia ser mais informativa nesse modo (ex.:
incorporar a restrição de que $n-1$ é o destino fixo) sem perder admissibilidade.

**7.5 — Desempate na fila de prioridade.** Sem regra de desempate explícita
quando $f(n_1) = f(n_2)$. Para A* isso não compromete corretude (qualquer ordem
de desempate ainda encontra o ótimo), só pode afetar o número de estados
explorados. Não é um problema de corretude, mas vale confirmar esse entendimento.

**7.6 — Consistência dimensional do "Cenário B".** `costB = timeCostReais +
fuelCostReais + tireWearReais` assume implicitamente peso $(1,1,1)$ só porque as
três parcelas já estão em R$ — isso é uma escolha de modelagem razoável (custo
real em reais), mas o "Cenário A" (só `durationSec`, sem conversão pra R$) não é
comparável na mesma unidade a "Cenário B". A comparação estatística
(`gapReais`/`gapPercent`, Seção 5.6) sempre avalia **ambas** as ordens sob a
métrica `costB` (nunca compara segundos com reais diretamente) — isso está
correto?

**7.7 — Bucket "> 100% (inviável)" nunca é imposto como restrição.** Uma carga
que excede capacidade (`feasible = false`) é só **reportada**
(`pctInfeasible`, bucket "> 100%"), nunca rejeitada ou penalizada no custo. O A*
roda normalmente sobre um cenário fisicamente inviável. Isso é intencional
(medir o quanto o algoritmo "erraria" nesses casos) ou deveria haver uma
restrição dura em algum lugar?

**7.8 — Teste de Wilcoxon com aproximação normal.** `wilcoxonSignedRankTest(x, y,
false)` usa aproximação normal (não distribuição exata) para o p-valor. Com $N$
efetivo tipicamente grande (até 500 menos empates), isso é apropriado — mas vale
confirmar se o tamanho mínimo de amostra pra essa aproximação ser válida está
sendo respeitado em todos os buckets menores (ex.: `byWaypointCount` para $n=2$
pode ter poucos trials).

---

## 8. Índice de arquivos citados

| Arquivo | Papel |
|---|---|
| `backend/src/main/java/com/routewise/algorithm/AStarWaypointOptimizer.java` | Núcleo A* de produção |
| `backend/src/main/java/com/routewise/algorithm/HaversineUtil.java` | Fórmula de Haversine (não usada pelo A* de produção) |
| `backend/src/main/java/com/routewise/model/RouteMode.java` | Enum dos 3 modos de rota |
| `backend/src/main/java/com/routewise/service/impl/RouteOptimizerServiceImpl.java` | Pipeline de produção (orquestra OSRM + A*) |
| `backend/src/main/java/com/routewise/service/impl/OsrmClientServiceImpl.java` | Cliente OSRM (`/table`, `/route`) |
| `backend/src/main/java/com/routewise/dto/RouteRequestDto.java` | Validação de entrada (`@Size(min=2,max=10)`) |
| `backend/src/test/java/com/routewise/algorithm/AStarWaypointOptimizerTest.java` | Testes do A* (sem cobertura de FIXED_START_END) |
| `backend/src/main/java/com/routewise/validation/CostMatrixBuilder.java` | Fórmula combinada de custo por aresta (Cenário A/B) |
| `backend/src/main/java/com/routewise/validation/VehicleProfile.java` | Constantes por classe de veículo |
| `backend/src/main/java/com/routewise/validation/RoadType.java` | Constantes por tipo de via |
| `backend/src/main/java/com/routewise/validation/AxlePosition.java` | Fator de vida útil por posição de eixo |
| `backend/src/main/java/com/routewise/validation/AxleLayout.java` | Contagem de pneus por posição/perfil |
| `backend/src/main/java/com/routewise/validation/CargoOccupancy.java` | Fórmula de ocupação peso×volume |
| `backend/src/main/java/com/routewise/validation/EmpiricalFuelConsumptionCalculator.java` | Calibração de consumo (janela móvel ponderada) |
| `backend/src/main/java/com/routewise/validation/EmpiricalTireLifeCalculator.java` | Calibração de vida útil de pneu (janela móvel simples) |
| `backend/src/main/java/com/routewise/validation/StatisticalAnalyzer.java` | Wilcoxon, Spearman, buckets |
| `backend/src/main/java/com/routewise/validation/MarginalContributionAnalyzer.java` | Teste de contribuição marginal + limiares de veredito |
| `backend/src/main/java/com/routewise/validation/TollCostAdditionExample.java` | Exemplo de custo de pedágio |
| `backend/src/main/java/com/routewise/validation/NewIndicatorAdditionExample.java` | Exemplo de indicador de risco |
| `backend/src/main/java/com/routewise/validation/CostWeightSensitivityExperiment.java` | Pesos explícitos $(\alpha,\beta,\gamma)$ sobre Cenário B |
| `backend/src/main/java/com/routewise/validation/ScenarioGenerator.java` | Geração de cenários sintéticos Monte Carlo |
| `backend/src/main/java/com/routewise/validation/ToledoRouteComparisonExample.java` | Exemplo real fixo (Toledo/PR) |
| `docs/superpowers/specs/2026-07-29-empirical-cost-validation-design.md` | Design original do modelo empírico |
| `docs/superpowers/specs/2026-08-01-empirical-fuel-consumption-rolling-window-design.md` | Design da janela móvel de combustível |
| `docs/superpowers/specs/2026-08-01-empirical-tire-wear-by-axle-position-design.md` | Design do desgaste de pneu por eixo |
| `docs/refactor-math-result/01-capacidade-peso-volume.md` | Revalidação numérica — ocupação de carga |
| `docs/refactor-math-result/02-consumo-combustivel.md` | Revalidação numérica — consumo de combustível |
| `docs/refactor-math-result/03-tempo-e-quilometragem.md` | Revalidação numérica — tempo/distância |
| `docs/refactor-math-result/04-desgaste-pneu-por-eixo.md` | Revalidação numérica — desgaste de pneu (antes/depois) |
