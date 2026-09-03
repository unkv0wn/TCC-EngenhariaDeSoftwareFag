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

/** Highest odometer reading among a vehicle's existing refuelings, or null if it has none yet. */
export function findLatestOdometer(
  refuelings: OdometerRecord[],
  vehicleId: string,
  excludeId?: string
): number | null {
  const odometers = refuelings
    .filter((refueling) => refueling.vehicleId === vehicleId && refueling.id !== excludeId)
    .map((refueling) => refueling.odometerKm);

  return odometers.length === 0 ? null : Math.max(...odometers);
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
    const previousOdometerKm = findLatestOdometer(refuelings, refueling.vehicleId, refueling.id);
    return { ...refueling, kmSincePrevious: calculateKmSincePrevious(refueling.odometerKm, previousOdometerKm) };
  });
}
