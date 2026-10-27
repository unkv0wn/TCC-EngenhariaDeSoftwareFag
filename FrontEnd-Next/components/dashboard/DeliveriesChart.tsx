import type { DashboardSummary } from "@/hooks/useDashboard";

const DAY_LABEL = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "2-digit" });

function label(isoDate: string): string {
  return DAY_LABEL.format(new Date(`${isoDate}T00:00:00`));
}

interface DeliveriesChartProps {
  deliveries: DashboardSummary["deliveries"];
}

/**
 * Entregas por dia, em barras.
 *
 * Desenhado com `div` e altura em porcentagem, sem biblioteca de gráfico: é uma série de
 * no máximo 90 pontos e uma dimensão. Trazer uma dependência de ~100 kB para isso custaria
 * mais do que entrega.
 */
export function DeliveriesChart({ deliveries }: DeliveriesChartProps) {
  const max = Math.max(...deliveries.map((day) => day.deliveries), 1);
  const total = deliveries.reduce((sum, day) => sum + day.deliveries, 0);

  // Em janelas longas, rotular todo dia vira uma faixa ilegível de números.
  const labelEvery = Math.ceil(deliveries.length / 10);

  return (
    <section className="rounded-xl border border-gray-200 bg-white px-5 py-4">
      <div className="mb-4 flex items-end justify-between">
        <div>
          <h2 className="text-[15px] font-extrabold text-gray-900">Entregas por dia</h2>
          <p className="text-[12.5px] font-semibold text-gray-500">
            Pela hora em que o motorista registrou a entrega, não pela data do pedido.
          </p>
        </div>
        <span className="text-[12px] font-bold text-gray-400">{total} no período</span>
      </div>

      {total === 0 ? (
        <p className="py-6 text-center text-[12.5px] font-semibold text-gray-400">
          Nenhuma entrega registrada neste período.
        </p>
      ) : (
        <div className="flex h-32 items-end gap-[3px]">
          {deliveries.map((day, index) => (
            <div key={day.date} className="flex h-full flex-1 flex-col justify-end gap-1">
              <div
                className="w-full rounded-sm bg-primary-500/85 transition-colors hover:bg-primary-600"
                style={{ height: `${(day.deliveries / max) * 100}%` }}
                title={`${label(day.date)} — ${day.deliveries} entrega${day.deliveries === 1 ? "" : "s"}`}
              />
              <span className="h-3 text-center text-[9px] font-bold text-gray-400">
                {index % labelEvery === 0 ? label(day.date) : ""}
              </span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
