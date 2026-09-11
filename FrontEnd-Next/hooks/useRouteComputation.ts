"use client";

import { useCallback, useEffect, useRef, useState } from "react";

import {
  computeRoute,
  routeEventsUrl,
  type RouteComputeRequest,
  type RouteResultDto,
} from "@/services/routes";
import { ApiError } from "@/lib/apiClient";

export type RouteComputationStatus = "IDLE" | "SUBMITTING" | "PROCESSING" | "COMPLETED" | "ERROR";

interface SseEnvelope {
  type: string;
  requestId: string;
  payload: unknown;
}

/**
 * Wrapper do fluxo de otimização de rota: dispara o POST /compute e consome o
 * stream SSE via `EventSource`, expondo status/result/error pra tela.
 */
export function useRouteComputation() {
  const [status, setStatus] = useState<RouteComputationStatus>("IDLE");
  const [result, setResult] = useState<RouteResultDto | null>(null);
  const [error, setError] = useState<string | null>(null);
  const sourceRef = useRef<EventSource | null>(null);

  const disconnect = useCallback(() => {
    if (sourceRef.current) {
      sourceRef.current.close();
      sourceRef.current = null;
    }
  }, []);

  const reset = useCallback(() => {
    disconnect();
    setStatus("IDLE");
    setResult(null);
    setError(null);
  }, [disconnect]);

  const compute = useCallback(
    async (request: RouteComputeRequest) => {
      disconnect();
      setStatus("SUBMITTING");
      setError(null);
      setResult(null);

      let requestId: string;
      try {
        requestId = await computeRoute(request);
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Falha ao solicitar o cálculo da rota.");
        setStatus("ERROR");
        return;
      }

      setStatus("PROCESSING");

      const source = new EventSource(routeEventsUrl(requestId));
      sourceRef.current = source;

      source.addEventListener("PROCESSING", () => {
        setStatus("PROCESSING");
      });

      source.addEventListener("COMPLETED", (event) => {
        try {
          const data = JSON.parse((event as MessageEvent).data) as SseEnvelope;
          setResult(data.payload as RouteResultDto);
          setStatus("COMPLETED");
        } catch {
          setError("Resposta inválida recebida do servidor.");
          setStatus("ERROR");
        }
        source.close();
        sourceRef.current = null;
      });

      source.addEventListener("ERROR", (event) => {
        try {
          const data = JSON.parse((event as MessageEvent).data) as SseEnvelope;
          const message = (data.payload as { message?: string } | null)?.message;
          setError(message ?? "Erro ao calcular a rota.");
        } catch {
          setError("Erro ao calcular a rota.");
        }
        setStatus("ERROR");
        source.close();
        sourceRef.current = null;
      });

      // Falha de conexão (não um evento ERROR nomeado do backend).
      source.onerror = () => {
        if (sourceRef.current === source) {
          setError("Conexão com o servidor perdida. Tente novamente.");
          setStatus("ERROR");
          source.close();
          sourceRef.current = null;
        }
      };
    },
    [disconnect]
  );

  useEffect(() => disconnect, [disconnect]);

  return {
    status,
    result,
    error,
    isComputing: status === "SUBMITTING" || status === "PROCESSING",
    compute,
    reset,
  };
}
