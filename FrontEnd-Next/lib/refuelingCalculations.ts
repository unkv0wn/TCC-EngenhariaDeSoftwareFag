/**
 * Minimal shape needed to look up a vehicle's odometer history — satisfied structurally by
 * `Refueling` from `hooks/useRefuelings.ts` without importing it here, so this module (and the
 * live-preview code in `RefuelingFormModal`) stays decoupled from the hook's state shape.
 */
interface OdometerRecord {
  id: string;
  vehicleId: string;
  odometerKm: number;
}

/**
 * Leitura imediatamente anterior a `odometerKm` no histórico do veículo — a maior que ainda
 * seja menor que ela. Null quando não há nenhuma antes, ou seja, no primeiro abastecimento.
 *
 * Existe porque a versão anterior usava a **maior** leitura do veículo como anterior. Para
 * o registro mais antigo de um veículo que tem outros, a maior leitura é uma leitura
 * posterior, e a subtração dava −650 km e −14 km/L na tela, com cara de dado. Comparar
 * só com leituras abaixo também torna o resultado independente da ordenação da lista.
 */
export function findPreviousOdometer(
  refuelings: OdometerRecord[],
  vehicleId: string,
  odometerKm: number,
  excludeId?: string
): number | null {
  const earlier = refuelings
    .filter(
      (refueling) =>
        refueling.vehicleId === vehicleId &&
        refueling.id !== excludeId &&
        refueling.odometerKm < odometerKm
    )
    .map((refueling) => refueling.odometerKm);

  return earlier.length === 0 ? null : Math.max(...earlier);
}

/** `OdometerRecord` com a data — necessária para validar a ordem do histórico. */
interface DatedOdometerRecord extends OdometerRecord {
  /** "YYYY-MM-DD", comparável como texto. */
  date: string;
}

/**
 * Faixa em que a leitura precisa cair, dada a posição do registro na linha do tempo do veículo:
 * maior que a do abastecimento anterior **em data**, menor que a do seguinte.
 *
 * A invariante é que data e odômetro contem a mesma história. A regra anterior era "maior que
 * a leitura mais recente do veículo", que confundia duas coisas: serve para um registro novo,
 * mas torna **impossível corrigir um antigo** — qualquer valor plausível para ele é menor que
 * o mais recente, então a tela recusava a edição inteira.
 *
 * `null` em qualquer das pontas significa que não há registro daquele lado — o primeiro ou o
 * último abastecimento do veículo.
 */
export function odometerBounds(
  refuelings: DatedOdometerRecord[],
  vehicleId: string,
  date: string,
  excludeId?: string
): { min: number | null; max: number | null } {
  const others = refuelings.filter(
    (refueling) => refueling.vehicleId === vehicleId && refueling.id !== excludeId
  );

  const before = others.filter((refueling) => refueling.date < date).map((r) => r.odometerKm);
  const after = others.filter((refueling) => refueling.date > date).map((r) => r.odometerKm);

  return {
    min: before.length === 0 ? null : Math.max(...before),
    max: after.length === 0 ? null : Math.min(...after),
  };
}

/** Km rodado desde o abastecimento anterior — null when there is no previous record. */
export function calculateKmSincePrevious(odometerKm: number, previousOdometerKm: number | null): number | null {
  return previousOdometerKm === null ? null : odometerKm - previousOdometerKm;
}

/** Total pago, sempre derivado — nunca armazenado num registro. */
export function calculateTotalPrice(litersRefueled: number, pricePerLiter: number): number {
  return litersRefueled * pricePerLiter;
}

/** Média de consumo (km/L) do trecho — null quando não há km rodado pra comparar (primeiro abastecimento). */
export function calculateAverageKmPerLiter(kmSincePrevious: number | null, litersRefueled: number): number | null {
  return kmSincePrevious === null || litersRefueled <= 0 ? null : kmSincePrevious / litersRefueled;
}

/**
 * Recalcula `kmSincePrevious` pra cada item da lista, com base nos outros registros do mesmo
 * veículo — usado depois de buscar do backend, já que esse campo não é armazenado lá (ver
 * comentário na migration V7): é sempre derivado, nunca congelado, pra não ficar desatualizado
 * se outro abastecimento do mesmo veículo for editado depois.
 */
export function attachKmSincePrevious<T extends OdometerRecord>(
  refuelings: T[]
): (T & { kmSincePrevious: number | null })[] {
  return refuelings.map((refueling) => {
    const previousOdometerKm = findPreviousOdometer(
      refuelings,
      refueling.vehicleId,
      refueling.odometerKm,
      refueling.id
    );
    return { ...refueling, kmSincePrevious: calculateKmSincePrevious(refueling.odometerKm, previousOdometerKm) };
  });
}

/**
 * Consumo médio de um conjunto de abastecimentos, em km/L.
 *
 * Soma os quilômetros e soma os litros, em vez de tirar a média das médias de cada linha:
 * um tanque que rodou 650 km tem que pesar mais do que um que rodou 80. É o mesmo método
 * do `EmpiricalFuelConsumptionCalculator` no backend — se os dois discordarem, a tela e o
 * dashboard passam a contar histórias diferentes sobre a mesma frota.
 *
 * Registros sem trecho conhecido (o primeiro de cada veículo) ficam de fora: eles têm
 * litros, mas não têm distância que aqueles litros percorreram.
 */
export function calculateFleetAverageKmPerLiter(
  refuelings: { kmSincePrevious: number | null; litersRefueled: number }[]
): number | null {
  const measurable = refuelings.filter(
    (refueling) => refueling.kmSincePrevious !== null && refueling.litersRefueled > 0
  );
  if (measurable.length === 0) return null;

  const totalKm = measurable.reduce((sum, refueling) => sum + (refueling.kmSincePrevious ?? 0), 0);
  const totalLiters = measurable.reduce((sum, refueling) => sum + refueling.litersRefueled, 0);

  return totalLiters > 0 ? totalKm / totalLiters : null;
}
