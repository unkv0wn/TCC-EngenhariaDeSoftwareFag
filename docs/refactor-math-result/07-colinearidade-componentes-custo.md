# Colinearidade entre os Componentes de `costB`

Escrito em: 2026-08-13
Fonte: `backend/src/main/java/com/routewise/validation/CostMatrixBuilder.java` e
`RoadType.java` (transcrito do código). Complementa
`06-formula-custo-atual.md` — assume as fórmulas de lá como dadas.

## 1. Pergunta

`costB = timeCostReais + fuelCostReais + tireWearReais` (implicitamente pesos
$(1,1,1)$). `CostWeightSensitivityExperiment` generaliza isso para
$\alpha,\beta,\gamma$ ajustáveis (Seção 5.8 de `MATH_VALIDATION_REVIEW.md`) e mostra
que mudar os pesos muda a rota escolhida em alguns cenários. Mas isso só é um sinal
de que os três termos carregam informação **genuinamente diferente** se eles não
forem, eles mesmos, quase a mesma variável reescalada. Esta nota verifica isso.

## 2. Decomposição algébrica

Para um perfil de veículo e uma carga fixos (logo `loadFactor = L` constante no
cenário — Seção 2 de `06-formula-custo-atual.md`), reescrevendo os três termos de
`CostMatrixBuilder.build` (linhas 76–98) isolando `distanceKm`:

$$
\text{timeCostReais}_{ij} = \text{distanceKm}_{ij} \cdot \underbrace{\frac{\text{driverCostPerHourReais}}{\text{avgSpeedKmh}(r_{ij})}}_{\text{só depende do tipo de via } r_{ij}}
$$

$$
\text{fuelCostReais}_{ij} = \text{distanceKm}_{ij} \cdot \underbrace{K_{\text{fuel}} \cdot \text{fuelMultiplier}(r_{ij})}_{K_{\text{fuel}} = \frac{\text{baseFuelConsumptionLPer100Km}\,(1+0{,}30L)\,\text{fuelPricePerLiter}}{100}}
$$

$$
\text{tireWearReais}_{ij} = \text{distanceKm}_{ij} \cdot \underbrace{K_{\text{tire}} \cdot \text{wearMultiplier}(r_{ij})}_{K_{\text{tire}} = \text{baseTireWearPerKm}\,(1+0{,}50L)\,\text{tireReplacementCostPerTire}}
$$

Ou seja: **os três termos têm exatamente a mesma forma**,
$\text{distanceKm}_{ij} \times \text{constante}_{\text{componente}} \times \text{fatorDeVia}_{\text{componente}}(r_{ij})$,
onde `fatorDeVia` é $1/\text{avgSpeedKmh}$ para tempo, `fuelMultiplier` para
combustível, `wearMultiplier` para pneu — e $r_{ij}$ (o tipo de via) é **o mesmo
sorteio** para os três, porque `ScenarioGenerator` sorteia um `RoadType` por aresta,
não um por componente.

Isso já entrega dois mecanismos de colinearidade sobrepostos: (a) todos escalam pela
mesma `distanceKm`, e (b) todos variam com o mesmo `RoadType` por aresta.

## 3. Bookend A — via fixa: colinearidade exata

Fixando o tipo de via, os três termos viram $\text{distanceKm}_{ij}$ multiplicado
por uma constante diferente cada — retas pela origem em função de
`distanceKm`. Correlação de Pearson entre qualquer par, dentro de um mesmo tipo de
via: $r = 1{,}000$ exatamente. Não há ruído nenhum aqui; é álgebra, não estatística.

## 4. Bookend B — via variando, distância "congelada": quase colinear também

O outro extremo: ignorar a variação de `distanceKm` e olhar só o efeito do tipo de
via. `RoadType` (`RODOVIA/ARTERIAL/URBANA`) é sorteado uniformemente
(`ScenarioGenerator`, 1/3 cada). Os três fatores de via:

| Tipo de via | fatorTempo $= 1/\text{avgSpeedKmh}$ | fatorCombustível $=$ `fuelMultiplier` | fatorPneu $=$ `wearMultiplier` |
|---|---|---|---|
| RODOVIA | 0,0125 | 0,85 | 0,80 |
| ARTERIAL | 0,0200 | 1,00 | 1,00 |
| URBANA | 0,0400 | 1,30 | 1,40 |

Os três **crescem juntos** de RODOVIA → ARTERIAL → URBANA (via pior = mais lenta,
mais combustível, mais desgaste — nenhum dos três se move na direção oposta dos
outros). Calculando a correlação de Pearson diretamente sobre esses 3 pontos
(cada tipo de via com peso 1/3):

$$
r(\text{tempo},\text{combustível}) \approx 0{,}998 \qquad
r(\text{tempo},\text{pneu}) \approx 0{,}998 \qquad
r(\text{combustível},\text{pneu}) \approx 0{,}9999\;(\approx 1)
$$

`combustível` e `pneu` saem praticamente indistinguíveis entre si: seus
multiplicadores (0,85/1,0/1,3 vs. 0,80/1,0/1,4) são quase proporcionais um ao outro
— a pequena diferença de proporcionalidade é a única coisa que os separa de $r=1$
exato. `tempo` diverge um pouco mais dos outros dois porque RODOVIA reduz o fator de
tempo mais agressivamente (0,625× o valor de ARTERIAL) do que reduz combustível
(0,85×) ou pneu (0,80×) — essa é a única fonte real de variação independente entre
os três componentes em todo o modelo.

## 5. O caso real (ambos variando) não fica melhor que os bookends

No `ScenarioGenerator` real, `distanceKm` **também** varia (Haversine entre pontos
sorteados num raio de ~35–40 km) e é sorteada independentemente do `RoadType` da
aresta. Escrevendo $X = D \cdot a(R)$, $Y = D \cdot b(R)$ com $D \perp R$:

$$
\text{Cov}(X,Y) = \mu_D^2 \cdot \text{Cov}(a,b) + \sigma_D^2 \cdot E[ab]
$$

Como $a,b > 0$ sempre (todo `fatorDeVia` é positivo) e $\text{Cov}(a,b) > 0$ (Seção
4), os dois termos do lado direito têm o mesmo sinal — variância extra em
`distanceKm` **soma** covariância, nunca subtrai. Isso confirma o que a intuição já
sugere: uma aresta de 50 km puxa os três custos para cima juntos, uma de 2 km puxa
os três para baixo juntos, **reforçando** a colinearidade do Bookend B em vez de
dilui-la. Não deriva daqui um valor fechado (dependeria da variância exata de
`distanceKm` no gerador), mas dá o suficiente para descartar a hipótese de que
misturar as duas fontes de variação "cancela" o efeito — os dois mecanismos
empurram na mesma direção.

## 6. Comparando com o próprio limiar de redundância do sistema

`MarginalContributionAnalyzer` já define, para decidir se um componente candidato
vale a pena adicionar:

```
REDUNDANT_CORRELATION_THRESHOLD = 0.9
redundant = |correlation| >= 0.9
```

Aplicando esse mesmo critério aos três componentes **que já existem** em `costB`
(não a um candidato novo): pelos números da Seção 4 ($r \approx 0{,}998$–$0{,}9999$,
e a Seção 5 argumenta que o número real só tende a subir), qualquer par
`(tempo, combustível)`, `(tempo, pneu)`, `(combustível, pneu)` passaria no próprio
critério de "REDUNDANTE" do código se um deles fosse testado como candidato contra
o outro.

## 7. O que isso significa para `CostWeightSensitivityExperiment`

Não invalida o experimento de sensibilidade de pesos — ele mede um efeito real
(Seção 5.8 de `MATH_VALIDATION_REVIEW.md` mostra rotas mudando com os pesos), e essa
mudança é exatamente a pequena fração **não-colinear** identificada na Seção 4 (o
jeito como RODOVIA favorece tempo mais do que favorece combustível/pneu). Mas
qualifica a interpretação: os pesos $(\alpha,\beta,\gamma)$ não estão escolhendo
entre três sinais independentes — estão, na maior parte, redistribuindo peso sobre
a **mesma** variável subjacente (`distanceKm` × tipo de via), com uma margem
estreita de variação genuína vindo só da relação não-proporcional entre
`avgSpeedKmh` e `(fuelMultiplier, wearMultiplier)` por tipo de via. Isso é
plausível para uma tese que já documenta os pesos como "não calibrados, escolhidos
manualmente" (Seção 5.8) — mas é uma limitação que vale declarar explicitamente se
o texto da tese apresentar os três componentes como fontes independentes de sinal.

## 8. Para obter o número exato (não feito aqui)

Esta nota é só a análise algébrica/estrutural, por pedido explícito — sem rodar
experimento novo. Se quiser o valor empírico exato de
$r(\text{tempo},\text{combustível})$ etc. sobre os 500 cenários reais do
`ScenarioGenerator` (que confirmaria ou ajustaria a estimativa de piso da Seção 4),
o `SpearmansCorrelation`/`PearsonsCorrelation` do `commons-math3` já usado em
`StatisticalAnalyzer` e `MarginalContributionAnalyzer` resolve isso em poucas
linhas — não implementado aqui.
