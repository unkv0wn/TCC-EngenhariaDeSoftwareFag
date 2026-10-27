import type { ApiClient } from "@/hooks/useApiClients";

export const ALL_CLIENTS_VALUE = "__all__";

/**
 * Pseudo-id das chamadas que não identificaram client — espelha
 * `IntegrationLogQueryService.UNIDENTIFIED` no backend. Mudou lá, mude aqui.
 */
export const UNIDENTIFIED_CLIENT_VALUE = "00000000-0000-0000-0000-000000000000";

interface IntegrationLogClientFilterProps {
  value: string;
  onChange: (value: string) => void;
  apiClients: ApiClient[];
}

export function IntegrationLogClientFilter({
  value,
  onChange,
  apiClients,
}: IntegrationLogClientFilterProps) {
  return (
    <select
      value={value}
      onChange={(event) => onChange(event.target.value)}
      aria-label="Filtrar o registro por integração"
      className="w-56 shrink-0 rounded-lg border border-gray-200 bg-white py-2 px-3.5 text-[12.5px] font-semibold text-gray-900 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15"
    >
      <option value={ALL_CLIENTS_VALUE}>Todas as integrações</option>
      {apiClients.map((client) => (
        <option key={client.id} value={client.id}>
          {client.name}
        </option>
      ))}
      {/*
        Separado dos clients porque não é um deles: são tentativas com credencial que o
        sistema não reconheceu. É a linha que o gestor procura quando desconfia de acesso
        indevido, e misturá-la em "todas" a esconde no volume das chamadas que deram certo.
      */}
      <option value={UNIDENTIFIED_CLIENT_VALUE}>Sem integração identificada</option>
    </select>
  );
}
