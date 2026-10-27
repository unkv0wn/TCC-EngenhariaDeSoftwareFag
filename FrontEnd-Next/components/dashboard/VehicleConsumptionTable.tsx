import type { DashboardSummary } from "@/hooks/useDashboard";

const NUMBER = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

const TABLE_HEADINGS = ["Veículo", "Perfil de custo", "Consumo", "Km em rota"];

interface VehicleConsumptionTableProps {
  vehicles: DashboardSummary["vehicles"];
}

export function VehicleConsumptionTable({ vehicles }: VehicleConsumptionTableProps) {
  return (
    <section className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <div className="px-5 py-4">
        <h2 className="text-[15px] font-extrabold text-gray-900">Consumo por veículo</h2>
        <p className="text-[12.5px] font-semibold leading-relaxed text-gray-500">
          Medido nos abastecimentos de cada um — média móvel dos últimos cinco, ponderada pela
          distância. Sem histórico, vale a referência do perfil.
        </p>
      </div>

      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading) => (
              <th
                key={heading}
                className="border-y border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {vehicles.map((vehicle) => (
            <tr key={vehicle.plate} className="border-b border-gray-100 last:border-0">
              <td className="px-3.5 py-3 font-bold text-gray-900">{vehicle.plate}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{vehicle.profileLabel}</td>
              <td className="px-3.5 py-3">
                <span className="font-bold text-gray-900">
                  {NUMBER.format(vehicle.consumptionLPer100Km)} L/100 km
                </span>
                {/*
                  A distinção entre medido e suposto é o ponto da tabela: dois veículos com
                  o mesmo número significam coisas diferentes se um foi medido e o outro
                  herdou a constante.
                */}
                {vehicle.measured ? (
                  <span className="ml-2 rounded-full bg-success-50 px-2 py-0.5 text-[10px] font-bold text-success-700">
                    medido · {vehicle.refuelings} abast.
                  </span>
                ) : (
                  <span className="ml-2 rounded-full bg-gray-100 px-2 py-0.5 text-[10px] font-bold text-gray-500">
                    {vehicle.refuelings === 0 ? "sem abastecimento" : "1 abastecimento — insuficiente"}
                  </span>
                )}
              </td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">
                {vehicle.routeDistanceKm > 0 ? `${NUMBER.format(vehicle.routeDistanceKm)} km` : "—"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  );
}
