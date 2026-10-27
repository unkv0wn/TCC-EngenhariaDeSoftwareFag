"use client";

import { Clock, Disc3, Fuel, Info } from "lucide-react";

import { formatCurrency, formatWeight } from "@/lib/format";
import type { RouteCost } from "@/hooks/useRouteCost";

const NUMBER = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

interface RouteCostPanelProps {
  cost: RouteCost | null;
  isLoading: boolean;
  error: string | null;
}

/**
 * Quanto esta rota custou, pelo modelo de custo do projeto.
 *
 * <p>O modelo existia só agregado, no dashboard: dava para saber o custo do mês e não o
 * desta rota, que é onde a pergunta nasce — o gestor está olhando uma rota quando quer
 * saber quanto ela custou.
 *
 * <p>A distinção entre medido e assumido é repetida por componente, e não só no rodapé,
 * porque é por componente que ela muda: combustível pode ser medição da própria frota,
 * pneu e motorista nunca são. Um total que não diga isso vende estimativa como despesa.
 */
export function RouteCostPanel({ cost, isLoading, error }: RouteCostPanelProps) {
  if (isLoading) {
    return (
      <section className="rounded-xl border border-gray-200 bg-white px-5 py-4">
        <p className="text-[12.5px] font-semibold text-gray-500">Calculando o custo da rota...</p>
      </section>
    );
  }

  if (error || !cost) {
    return (
      <section className="rounded-xl border border-gray-200 bg-white px-5 py-4">
        <p className="text-[12.5px] font-semibold text-danger-600">
          {error ?? "Não foi possível calcular o custo desta rota."}
        </p>
      </section>
    );
  }

  const parts = [
    {
      label: "Combustível",
      value: cost.fuelCost,
      icon: Fuel,
      bar: "bg-primary-500",
      note: `${NUMBER.format(cost.fuelLiters)} L a ${formatCurrency(cost.fuelPricePerLiter)}/L — ${
        cost.fuelPriceMeasured ? "preço pago pela frota" : "preço de referência"
      }`,
    },
    {
      label: "Pneu",
      value: cost.tireCost,
      icon: Disc3,
      bar: "bg-secondary-500",
      note: "desgaste pelo modelo, constante do experimento",
    },
    {
      label: "Motorista",
      value: cost.driverCost,
      icon: Clock,
      bar: "bg-success-500",
      note: `${NUMBER.format(cost.durationMin / 60)} h a valor-hora do perfil`,
    },
  ];

  // Carga zero é ambíguo: pode ser caminhão vazio ou rota salva antes de LabeledStop
  // guardar orderIds, que não tem vínculo com pedido e nunca terá. Dizer "sem vínculo"
  // evita que o gestor leia como "saiu vazio".
  //
  // A capacidade é a do perfil, não a cadastrada no veículo, porque é a do perfil que
  // entra no fator de carga do custo. São duas, e a tela mostra as duas — o cabeçalho
  // traz a do veículo —, então esta precisa dizer de qual está falando.
  const loadShare = cost.capacityKg > 0 ? (cost.cargoWeightKg / cost.capacityKg) * 100 : 0;

  return (
    <section className="rounded-xl border border-gray-200 bg-white px-5 py-4">
      <div className="mb-4 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-[15px] font-extrabold text-gray-900">Custo estimado desta rota</h2>
          <p className="text-[12.5px] font-semibold leading-relaxed text-gray-500">
            {NUMBER.format(cost.distanceKm)} km · {cost.profileLabel}
            {cost.cargoWeightKg > 0
              ? ` · ${formatWeight(cost.cargoWeightKg)} de carga — ${NUMBER.format(loadShare)}% dos ${formatWeight(
                  cost.capacityKg
                )} do perfil`
              : " · sem carga vinculada a pedidos"}
          </p>
        </div>
        <div className="shrink-0 text-right">
          <div className="text-2xl font-extrabold leading-none text-gray-900">
            {formatCurrency(cost.total)}
          </div>
          <div className="mt-1 text-[12px] font-bold text-gray-400">
            {formatCurrency(cost.perKm)} por km
          </div>
        </div>
      </div>

      <div className="flex flex-col gap-3">
        {parts.map(({ label, value, icon: Icon, bar, note }) => {
          const share = cost.total > 0 ? (value / cost.total) * 100 : 0;
          return (
            <div key={label} className="flex items-start gap-3">
              <Icon className="mt-0.5 h-4 w-4 shrink-0 text-gray-400" aria-hidden="true" />
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-3">
                  <span className="w-24 shrink-0 text-[12.5px] font-bold text-gray-700">{label}</span>
                  <div className="h-2 flex-1 overflow-hidden rounded-full bg-gray-100">
                    <div className={`h-full rounded-full ${bar}`} style={{ width: `${share}%` }} />
                  </div>
                  <span className="w-24 shrink-0 text-right text-[12.5px] font-bold text-gray-900">
                    {formatCurrency(value)}
                  </span>
                  <span className="w-10 shrink-0 text-right text-[11.5px] font-semibold text-gray-400">
                    {NUMBER.format(share)}%
                  </span>
                </div>
                <p className="mt-0.5 text-[11.5px] font-semibold text-gray-400">{note}</p>
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-4 flex items-start gap-2 border-t border-gray-100 pt-3.5">
        <Info className="mt-0.5 h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
        <p className="text-[11.5px] font-semibold leading-relaxed text-gray-500">
          Estimativa, não despesa apurada. O consumo usado foi{" "}
          <strong className="font-bold text-gray-700">
            {NUMBER.format(cost.consumptionLPer100Km)} L/100 km
          </strong>
          {cost.consumptionMeasured
            ? ", medido nos abastecimentos deste veículo"
            : ", referência do perfil — este veículo ainda não tem abastecimentos suficientes"}
          . Pneu e hora de motorista são constantes documentadas do modelo, porque o sistema não
          cadastra troca de pneu nem valor-hora. A rota guarda só os totais, então o tipo de via
          entra como via arterial: rota urbana fica subestimada, rodovia superestimada.
        </p>
      </div>
    </section>
  );
}
