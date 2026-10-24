import { apiFetch } from "@/lib/apiClient";
import type { ApiClient, ApiClientCreated, IntegrationLogEntry } from "@/hooks/useApiClients";
import type { ApiClientFormData } from "@/lib/validations/apiClient";

/**
 * Note o caminho: `/api/api-clients`, e não `/api/integration/...`. Aquele prefixo é a
 * superfície que terceiros consomem e exige token de integração; isto é cadastro do
 * painel.
 */
const BASE_PATH = "/api/api-clients";

export function listApiClients(): Promise<ApiClient[]> {
  return apiFetch<ApiClient[]>(BASE_PATH);
}

/** Devolve o segredo em claro — a única vez que ele existe fora do hash. */
export function createApiClient(data: ApiClientFormData): Promise<ApiClientCreated> {
  return apiFetch<ApiClientCreated>(BASE_PATH, { method: "POST", body: data });
}

export function updateApiClient(id: string, data: ApiClientFormData): Promise<ApiClient> {
  return apiFetch<ApiClient>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function regenerateApiClientSecret(id: string): Promise<ApiClientCreated> {
  return apiFetch<ApiClientCreated>(`${BASE_PATH}/${id}/secret`, { method: "POST" });
}

export function deleteApiClient(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}

export function listIntegrationLogs(limit = 100): Promise<IntegrationLogEntry[]> {
  return apiFetch<IntegrationLogEntry[]>(`${BASE_PATH}/logs?limit=${limit}`);
}
