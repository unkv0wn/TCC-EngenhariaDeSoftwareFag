"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { ApiClientFormData, ApiClientStatus } from "@/lib/validations/apiClient";
import {
  createApiClient as createApiClientApi,
  deleteApiClient as deleteApiClientApi,
  listApiClients,
  listIntegrationLogs,
  regenerateApiClientSecret as regenerateSecretApi,
  updateApiClient as updateApiClientApi,
} from "@/services/apiClients";

/**
 * Integração como o servidor a devolve.
 *
 * Não estende `ApiClientFormData` porque os dois lados não coincidem: `clientId`,
 * `lastUsedAt` e `createdAt` só voltam, e o segredo só vai — e só na criação.
 */
export interface ApiClient {
  id: string;
  name: string;
  clientId: string;
  scopes: string[];
  status: ApiClientStatus;
  lastUsedAt: string | null;
  createdAt: string | null;
}

/**
 * Resposta da criação, ou da regeração do segredo.
 *
 * Tipo próprio e não um campo opcional em `ApiClient`: um tipo que às vezes carrega
 * segredo e às vezes não convida a ser renderizado por engano numa listagem.
 */
export interface ApiClientCreated {
  client: ApiClient;
  /** Em claro, uma única vez. Não é recuperável — some do servidor. */
  clientSecret: string;
}

export interface IntegrationLogEntry {
  id: string;
  /** Null quando a chamada não chegou a identificar a integração — ver `attemptedClientId`. */
  apiClientId: string | null;
  clientName: string | null;
  attemptedClientId: string | null;
  method: string;
  path: string;
  outcome: string;
  statusCode: number;
  occurredAt: string;
}

export function useApiClients() {
  const [apiClients, setApiClients] = useState<ApiClient[]>([]);
  const [logs, setLogs] = useState<IntegrationLogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingLogs, setIsLoadingLogs] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [logsError, setLogsError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      // Juntas: a tela mostra as duas coisas e separá-las faria a lista aparecer antes
      // do registro, com um pedaço piscando depois do outro.
      const [clients, entries] = await Promise.all([listApiClients(), listIntegrationLogs()]);
      setApiClients(clients);
      setLogs(entries);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar as integrações.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // queueMicrotask evita chamar setState de forma síncrona dentro do efeito
    // (refresh já começa com setIsLoading(true) antes de qualquer await).
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  /**
   * Recarrega só o registro, para o filtro por integração.
   *
   * Separado do `refresh` porque trocar o filtro não deveria apagar a tabela de
   * integrações da tela — ela não muda, e fazê-la piscar a cada troca faria o filtro
   * parecer mais caro do que é.
   */
  const refreshLogs = useCallback(async (clientId?: string) => {
    setIsLoadingLogs(true);
    setLogsError(null);
    try {
      setLogs(await listIntegrationLogs(100, clientId));
    } catch (err) {
      setLogsError(err instanceof ApiError ? err.message : "Não foi possível carregar o registro.");
    } finally {
      setIsLoadingLogs(false);
    }
  }, []);

  const createApiClient = useCallback(async (data: ApiClientFormData) => {
    const created = await createApiClientApi(data);
    setApiClients((prev) => [created.client, ...prev]);
    return created;
  }, []);

  const updateApiClient = useCallback(async (id: string, data: ApiClientFormData) => {
    const updated = await updateApiClientApi(id, data);
    setApiClients((prev) => prev.map((client) => (client.id === id ? updated : client)));
    return updated;
  }, []);

  const regenerateSecret = useCallback(async (id: string) => {
    const created = await regenerateSecretApi(id);
    setApiClients((prev) => prev.map((client) => (client.id === id ? created.client : client)));
    return created;
  }, []);

  const deleteApiClient = useCallback(async (id: string) => {
    await deleteApiClientApi(id);
    setApiClients((prev) => prev.filter((client) => client.id !== id));
  }, []);

  return {
    apiClients,
    logs,
    isLoadingLogs,
    logsError,
    refreshLogs,
    isLoading,
    error,
    refresh,
    createApiClient,
    updateApiClient,
    regenerateSecret,
    deleteApiClient,
  };
}
