import { KeyRound, Pencil, Trash2 } from "lucide-react";

import { cn } from "@/lib/utils";
import type { ApiClient } from "@/hooks/useApiClients";
import { formatDateTime } from "@/lib/format";
import { API_CLIENT_SCOPES, type ApiClientStatus } from "@/lib/validations/apiClient";

const TABLE_HEADINGS = ["Integração", "Client ID", "Escopos", "Último uso", "Status", ""];

const STATUS_BADGE_STYLES: Record<ApiClientStatus, string> = {
  ativo: "bg-success-50 text-success-700",
  revogado: "bg-danger-50 text-danger-600",
};

const STATUS_LABELS: Record<ApiClientStatus, string> = {
  ativo: "Ativo",
  revogado: "Revogado",
};

function scopeLabel(value: string): string {
  return API_CLIENT_SCOPES.find((scope) => scope.value === value)?.label ?? value;
}

interface ApiClientTableProps {
  apiClients: ApiClient[];
  onEdit: (client: ApiClient) => void;
  onRegenerate: (client: ApiClient) => void;
  onDelete: (client: ApiClient) => void;
}

export function ApiClientTable({ apiClients, onEdit, onRegenerate, onDelete }: ApiClientTableProps) {
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
          {apiClients.map((client) => (
            <tr key={client.id} className="border-b border-gray-100 last:border-0">
              <td className="px-3.5 py-3 font-bold text-gray-900">{client.name}</td>
              <td className="px-3.5 py-3">
                {/* Truncado: o Client ID não é segredo, mas ocupa a tabela inteira. */}
                <code className="font-mono text-[11.5px] font-semibold text-gray-500">
                  {client.clientId.slice(0, 16)}…
                </code>
              </td>
              <td className="px-3.5 py-3">
                <div className="flex flex-wrap gap-1">
                  {client.scopes.map((scope) => (
                    <span
                      key={scope}
                      className="rounded-full bg-gray-100 px-2 py-0.5 text-[10.5px] font-bold text-gray-600"
                    >
                      {scopeLabel(scope)}
                    </span>
                  ))}
                </div>
              </td>
              <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                {client.lastUsedAt ? formatDateTime(client.lastUsedAt) : "Nunca usada"}
              </td>
              <td className="px-3.5 py-3">
                <span
                  className={cn(
                    "rounded-full px-2 py-0.5 text-[10px] font-bold",
                    STATUS_BADGE_STYLES[client.status]
                  )}
                >
                  {STATUS_LABELS[client.status]}
                </span>
              </td>
              <td className="px-3.5 py-3">
                <div className="flex items-center justify-end gap-1">
                  <button
                    type="button"
                    onClick={() => onRegenerate(client)}
                    aria-label={`Gerar novo segredo para ${client.name}`}
                    title="Gerar novo segredo"
                    className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                  >
                    <KeyRound className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onEdit(client)}
                    aria-label={`Editar integração ${client.name}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                  >
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(client)}
                    aria-label={`Excluir integração ${client.name}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-danger-50 hover:text-danger-600"
                  >
                    <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
