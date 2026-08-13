# A* — Desempate por Maior g: Antes vs. Depois

Gerado em: 2026-08-06 22:25:00

## Contexto

Comparação entre o `Comparator` antigo da fila de prioridade do A* (`f = g + h`, sem desempate) e o novo (`f = g + h`, empate resolvido a favor do maior `g`), medida em várias escalas de waypoints — não só o máximo de produção (10) — para ver se o efeito do desempate muda conforme o espaço de estados cresce. Durações quantizadas em múltiplos de 5 minutos (ver Javadoc de `randomDurationMatrix` — com durações contínuas, empates exatos em `f` são estatisticamente improváveis e as duas versões convergem para o mesmo desempenho). Modo `ROUND_TRIP`, execuções de aquecimento (JIT) descartadas antes de cronometrar cada matriz.

**Aviso:** os tempos de parede (wall-clock) abaixo foram medidos na máquina que rodou este teste — não são um benchmark de laboratório (tipo JMH) e variam entre execuções e entre máquinas. A garantia formal que importa é a igualdade do custo total, verificada matriz por matriz e checada por asserção em todas as escalas: o desempate nunca muda qual rota é ótima, só a ordem em que estados de mesmo `f` são expandidos (ver Javadoc de `AStarWaypointOptimizer.DEFAULT_COMPARATOR`). **O ganho de velocidade não é garantido em toda instância** — é um efeito estatístico agregado, não uma melhoria universal por requisição.

## Por que não 30 waypoints

Foi pedido para rodar este benchmark com 30 waypoints. `AStarWaypointOptimizer` aloca `double[n][2ⁿ]` (`bestG`) e `int[n][2ⁿ][2]` (`parent`) — em `n=10` isso é 10×1024 células, irrelevante; em `n=30`, `2³⁰ = 1.073.741.824`, e cada uma dessas duas estruturas sozinha já exigiria:

```
bestG:  30 × 2^30 × 8 bytes (double) ≈ 257.7 GB
parent: 30 × 2^30 × 2 × 4 bytes (int) ≈ 257.7 GB
total (só estas duas estruturas)     ≈ 515.4 GB
```

Isso é sem contar a fila de prioridade, que no pior caso guarda entradas para uma fração grande desses mesmos estados. `n=31` já estouraria o limite de indexação de array do Java (`2³¹ > Integer.MAX_VALUE`), então esse desenho de dados (array denso indexado pela máscara de bits inteira) tem um teto físico por volta de `n=30`, independente de qualquer otimização de constantes.

Este teste sobe até `n=18` (a maior escala que ainda roda em segundos, sem risco de estourar o heap da JVM). Isso é um limite de **viabilidade deste benchmark**, não uma recomendação sobre onde o limite de produção deveria ficar — essa é uma decisão separada, guiada por UX e pelo custo da chamada de matriz do OSRM, não só por este algoritmo. Se um `n` maior for realmente necessário no futuro, o caminho não é mais memória — é trocar a estrutura de estado densa (array indexado pela máscara inteira) por uma esparsa (`Map<Long, Double>` ou similar), já que a imensa maioria dos `2ⁿ` estados nunca chega a ser visitada de fato.

## Resultado agregado por escala

| n (waypoints) | Estados/linha (2ⁿ) | Matrizes | Antes (µs) | Nova (µs) | Mais rápida em |
|---|---|---|---|---|---|
| 10 | 1,024 | 20 | 2036.4 | 2043.6 | 10 de 20 |
| 15 | 32,768 | 10 | 88261.1 | 87173.7 | 8 de 10 |
| 18 | 262,144 | 5 | 867386.4 | 842963.5 | 3 de 5 |

## Custo total por matriz e por escala (prova de que o resultado ótimo não muda)

### n = 10

| Matriz | Custo — antes (s) | Custo — nova (s) | Igual? |
|---|---|---|---|
| 0 | 5400.0 | 5400.0 | sim |
| 1 | 5400.0 | 5400.0 | sim |
| 2 | 5400.0 | 5400.0 | sim |
| 3 | 4800.0 | 4800.0 | sim |
| 4 | 4800.0 | 4800.0 | sim |
| 5 | 4500.0 | 4500.0 | sim |
| 6 | 5100.0 | 5100.0 | sim |
| 7 | 8100.0 | 8100.0 | sim |
| 8 | 4500.0 | 4500.0 | sim |
| 9 | 4800.0 | 4800.0 | sim |
| 10 | 6600.0 | 6600.0 | sim |
| 11 | 4800.0 | 4800.0 | sim |
| 12 | 5100.0 | 5100.0 | sim |
| 13 | 4800.0 | 4800.0 | sim |
| 14 | 6300.0 | 6300.0 | sim |
| 15 | 5700.0 | 5700.0 | sim |
| 16 | 6600.0 | 6600.0 | sim |
| 17 | 9000.0 | 9000.0 | sim |
| 18 | 6000.0 | 6000.0 | sim |
| 19 | 8400.0 | 8400.0 | sim |

### n = 15

| Matriz | Custo — antes (s) | Custo — nova (s) | Igual? |
|---|---|---|---|
| 0 | 6300.0 | 6300.0 | sim |
| 1 | 6600.0 | 6600.0 | sim |
| 2 | 7500.0 | 7500.0 | sim |
| 3 | 7500.0 | 7500.0 | sim |
| 4 | 5700.0 | 5700.0 | sim |
| 5 | 6600.0 | 6600.0 | sim |
| 6 | 6300.0 | 6300.0 | sim |
| 7 | 7500.0 | 7500.0 | sim |
| 8 | 6000.0 | 6000.0 | sim |
| 9 | 5400.0 | 5400.0 | sim |

### n = 18

| Matriz | Custo — antes (s) | Custo — nova (s) | Igual? |
|---|---|---|---|
| 0 | 7200.0 | 7200.0 | sim |
| 1 | 6600.0 | 6600.0 | sim |
| 2 | 7200.0 | 7200.0 | sim |
| 3 | 7800.0 | 7800.0 | sim |
| 4 | 6900.0 | 6900.0 | sim |

Em todas as matrizes, em todas as escalas testadas, o custo total encontrado foi idêntico entre as duas versões (diferença < 1e-06 s), confirmando que o desempate não altera a otimalidade — só potencialmente a ordem de expansão. O comportamento de velocidade entre as escalas fica registrado na tabela agregada acima; leia-o como tendência estatística, não como garantia por requisição.
