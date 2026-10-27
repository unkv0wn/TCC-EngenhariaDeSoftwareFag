import { Fuel, CircleDollarSign, Clock, Disc3 } from "lucide-react";

import { formatCurrency } from "@/lib/format";
import type { DashboardSummary } from "@/hooks/useDashboard";

const NUMBER = new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 });

interface CostCardProps {
  cost: DashboardSummary["cost"];
  distanceKm: number;
}

export function CostCard({ cost, distanceKm }: CostCardProps) {
  const parts = [
    { label: "Combustível", value: cost.fuelCost, icon: Fuel, bar: "bg-primary-500" },
    { label: "Pneu", value: cost.tireCost, icon: Disc3, bar: "bg-secondary-500" },
    { label: "Motorista", value: cost.driverCost, icon: Clock, bar: "bg-success-500" },
  ];

  return (
    <section className="rounded-xl border border-gray-200 bg-white px-5 py-4">
      <div className="mb-4 flex items-start justify-between gap-4">
        <div>
          <h2 className="text-[15px] font-extrabold text-gray-900">Custo estimado da operação</h2>
          <p className="text-[12.5px] font-semibold leading-relaxed text-gray-500">
            Pelo modelo de custo do projeto, aplicado às rotas do período.
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

      <div className="flex flex-col gap-2.5">
        {parts.map(({ label, value, icon: Icon, bar }) => {
          const share = cost.total > 0 ? (value / cost.total) * 100 : 0;
          return (
            <div key={label} className="flex items-center gap-3">
              <Icon className="h-4 w-4 shrink-0 text-gray-400" aria-hidden="true" />
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
          );
        })}
      </div>

      {/*
        O gasto real fica ao lado da estimativa, e não no lugar dela: são números
        diferentes. Os dois não fecham entre si, e não deveriam — o caminhão abastece pra
        rodar tudo, inclusive o que nunca virou rota aqui dentro. Esconder isso faria o
        gestor ler a diferença como erro do modelo.
      */}
      <div className="mt-4 grid grid-cols-2 gap-3 border-t border-gray-100 pt-3.5">
        <div>
          <div className="flex items-center gap-1.5 text-[11.5px] font-bold uppercase tracking-wider text-gray-400">
            <CircleDollarSign className="h-3.5 w-3.5" aria-hidden="true" />
            Abastecido no período
          </div>
          <div className="mt-1 text-[15px] font-extrabold text-gray-900">
            {formatCurrency(cost.refueledCost)}
          </div>
          <p className="text-[11.5px] font-semibold text-gray-500">
            {cost.refueledLiters > 0
              ? `${NUMBER.format(cost.refueledLiters)} L registrados em abastecimentos`
              : "Nenhum abastecimento registrado neste período"}
          </p>
        </div>

        <div>
          <div className="text-[11.5px] font-bold uppercase tracking-wider text-gray-400">
            Estimativa de combustível
          </div>
          <div className="mt-1 text-[15px] font-extrabold text-gray-900">
            {NUMBER.format(cost.fuelLiters)} L
          </div>
          <p className="text-[11.5px] font-semibold text-gray-500">
            {distanceKm > 0
              ? `a ${formatCurrency(cost.fuelPricePerLiter)}/L — ${
                  cost.fuelPriceMeasured ? "preço pago pela frota" : "preço de referência"
                }`
              : "sem rotas no período"}
          </p>
        </div>
      </div>

      {/*
        A honestidade do bloco mora aqui. Consumo e preço podem ser medidos; pneu e hora
        de motorista são constantes do experimento, porque o sistema não cadastra troca de
        pneu nem valor-hora. Omitir isso venderia estimativa como despesa apurada.
      */}
      <p className="mt-3.5 rounded-lg bg-gray-50 px-3 py-2.5 text-[11.5px] font-semibold leading-relaxed text-gray-500">
        Estimativa, não despesa apurada. Consumo vem dos abastecimentos de cada veículo quando há
        histórico; custo de pneu e hora de motorista são constantes documentadas do modelo, porque o
        sistema ainda não cadastra troca de pneu nem valor-hora.
      </p>
    </section>
  );
}
