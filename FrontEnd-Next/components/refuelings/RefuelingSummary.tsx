import { Droplets, Gauge, Receipt, TrendingUp } from "lucide-react";

import type { Refueling } from "@/hooks/useRefuelings";
import { formatCurrency } from "@/lib/format";
import {
  calculateFleetAverageKmPerLiter,
  calculateTotalPrice,
} from "@/lib/refuelingCalculations";

const NUMBER = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

interface RefuelingSummaryProps {
  /** Já filtrados: o resumo descreve o que está na tela, não a frota inteira. */
  refuelings: Refueling[];
  /** Nome do veículo quando há um filtro ativo — muda o que os números significam. */
  vehicleLabel: string | null;
}

export function RefuelingSummary({ refuelings, vehicleLabel }: RefuelingSummaryProps) {
  const totalLiters = refuelings.reduce((sum, refueling) => sum + refueling.litersRefueled, 0);
  const totalSpent = refuelings.reduce(
    (sum, refueling) => sum + calculateTotalPrice(refueling.litersRefueled, refueling.pricePerLiter),
    0
  );
  const average = calculateFleetAverageKmPerLiter(refuelings);
  const averagePrice = totalLiters > 0 ? totalSpent / totalLiters : 0;

  const tiles = [
    { icon: Receipt, value: formatCurrency(totalSpent), label: "Total abastecido" },
    { icon: Droplets, value: `${NUMBER.format(totalLiters)} L`, label: "Litros" },
    {
      icon: Gauge,
      value: average === null ? "—" : `${NUMBER.format(average)} km/L`,
      // O primeiro abastecimento de cada veículo não entra na média: tem litros, mas não
      // tem distância percorrida por eles. Dizer isso evita o gestor procurar a diferença.
      label:
        average === null
          ? "Sem trecho medido ainda"
          : vehicleLabel
            ? "Média do veículo"
            : "Média da frota",
    },
    { icon: TrendingUp, value: `${formatCurrency(averagePrice)}/L`, label: "Preço médio pago" },
  ];

  return (
    <div className="mb-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
      {tiles.map(({ icon: Icon, value, label }) => (
        <div
          key={label}
          className="flex items-center gap-3 rounded-xl border border-gray-200 bg-white px-4 py-3"
        >
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary-50 text-primary-600">
            <Icon className="h-4 w-4" aria-hidden="true" />
          </div>
          <div className="min-w-0">
            <div className="truncate text-[17px] font-extrabold leading-tight text-gray-900">
              {value}
            </div>
            <div className="truncate text-[11.5px] font-semibold text-gray-500">{label}</div>
          </div>
        </div>
      ))}
    </div>
  );
}
