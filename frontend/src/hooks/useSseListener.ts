import { useState, useCallback, useRef, useEffect } from 'react';
import type {
  TComputationStatus,
  TRouteResult,
  TSseEvent,
} from '../types/route.types';
import { getSseUrl } from '../services/routeApi';

interface ISseListenerState {
  status: TComputationStatus;
  result: TRouteResult | null;
  error: string | null;
  isComputing: boolean;
}

interface IUseSseListenerReturn extends ISseListenerState {
  connect: (requestId: string) => void;
  disconnect: () => void;
  reset: () => void;
}

/**
 * Custom hook that wraps the native EventSource API
 * for consuming SSE events from the RouteWise backend.
 */
export function useSseListener(): IUseSseListenerReturn {
  const [status, setStatus] = useState<TComputationStatus>('IDLE');
  const [result, setResult] = useState<TRouteResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  const disconnect = useCallback(() => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
  }, []);

  const connect = useCallback((requestId: string) => {
    // Clean up any existing connection
    disconnect();

    setStatus('PROCESSING');
    setError(null);
    setResult(null);

    const url = getSseUrl(requestId);
    const es = new EventSource(url);
    eventSourceRef.current = es;

    // Listen for named events matching backend types
    es.addEventListener('PROCESSING', (event: MessageEvent) => {
      try {
        const data: TSseEvent = JSON.parse(event.data);
        setStatus('PROCESSING');
        // Log for debugging in dev
        console.debug('[SSE] PROCESSING event received:', data.requestId);
      } catch {
        console.warn('[SSE] Failed to parse PROCESSING event data');
      }
    });

    es.addEventListener('COMPLETED', (event: MessageEvent) => {
      try {
        const data: TSseEvent = JSON.parse(event.data);
        if (data.payload) {
          setResult(data.payload);
          setStatus('COMPLETED');
          console.debug('[SSE] COMPLETED event received:', data.requestId);
        }
      } catch {
        console.warn('[SSE] Failed to parse COMPLETED event data');
        setError('Invalid route data received from server.');
        setStatus('ERROR');
      }
      // Backend closes the stream after COMPLETED
      es.close();
      eventSourceRef.current = null;
    });

    es.addEventListener('ERROR', (event: MessageEvent) => {
      try {
        const data: TSseEvent = JSON.parse(event.data);
        setError(data.message ?? 'An unknown error occurred during route computation.');
        setStatus('ERROR');
      } catch {
        setError('Route computation failed.');
        setStatus('ERROR');
      }
      es.close();
      eventSourceRef.current = null;
    });

    // Generic error handler (connection failures, etc.)
    es.onerror = () => {
      // Only set error if we haven't already received a terminal event
      if (eventSourceRef.current === es) {
        setError('Lost connection to the server. Please try again.');
        setStatus('ERROR');
        es.close();
        eventSourceRef.current = null;
      }
    };
  }, [disconnect]);

  const reset = useCallback(() => {
    disconnect();
    setStatus('IDLE');
    setResult(null);
    setError(null);
  }, [disconnect]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return {
    status,
    result,
    error,
    isComputing: status === 'SUBMITTING' || status === 'PROCESSING',
    connect,
    disconnect,
    reset,
  };
}
