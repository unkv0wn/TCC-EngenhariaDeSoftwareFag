import { cn } from "@/lib/utils";
import type { IntegrationLogEntry } from "@/hooks/useApiClients";
import { formatDateTime } from "@/lib/format";
import { INTEGRATION_OUTCOMES } from "@/lib/validations/apiClient";

const TABLE_HEADINGS = ["Quando", "Integração", "Requisição", "Resultado"];

/** Verde só para sucesso; o resto é recusa, e recusa é o que esta tela existe para mostrar. */
function outcomeStyle(outcome: string): string {
  return outcome === "sucesso" ? "bg-success-50 text-success-700" : "bg-danger-50 text-danger-600";
}

interface IntegrationLogTableProps {
  logs: IntegrationLogEntry[];
}

export function IntegrationLogTable({ logs }: IntegrationLogTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading) => (
              <th
                key={heading}
                className="border-b border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {logs.map((entry) => (
            <tr key={entry.id} className="border-b border-gray-100 last:border-0">
              <td className="whitespace-nowrap px-3.5 py-2.5 font-semibold text-gray-500">
                {formatDateTime(entry.occurredAt)}
              </td>
              <td className="px-3.5 py-2.5">
                {/*
                  Sem nome significa tentativa sem dono — client_id que não existe, ou
                  integração já apagada. Nesse caso o que identifica quem tentou é o id
                  enviado, e é justamente a linha mais interessante da tabela.
                */}
                {entry.clientName ? (
                  <span className="font-bold text-gray-900">{entry.clientName}</span>
                ) : (
                  <span className="font-mono text-[11.5px] font-semibold text-gray-500">
                    {entry.attemptedClientId ?? "— sem identificação —"}
                  </span>
                )}
              </td>
              <td className="px-3.5 py-2.5">
                <code className="font-mono text-[11.5px] font-semibold text-gray-500">
                  {entry.method} {entry.path}
                </code>
              </td>
              <td className="px-3.5 py-2.5">
                <span
                  className={cn(
                    "rounded-full px-2 py-0.5 text-[10px] font-bold",
                    outcomeStyle(entry.outcome)
                  )}
                >
                  {INTEGRATION_OUTCOMES[entry.outcome] ?? entry.outcome} · {entry.statusCode}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
