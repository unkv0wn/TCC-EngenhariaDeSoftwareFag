"use client";

import { createContext, useCallback, useMemo, useReducer, type ReactNode } from "react";

import { ToastViewport } from "@/components/ui/toast/ToastViewport";

export type ToastVariant = "success" | "danger" | "warning";

export interface Toast {
  id: string;
  variant: ToastVariant;
  title: string;
  description?: string;
}

interface ToastState {
  /** toasts[0] is the oldest; new toasts are appended to the end. */
  toasts: Toast[];
}

type ToastAction = { type: "ADD"; toast: Toast } | { type: "REMOVE"; id: string };

const MAX_TOASTS = 4;

function toastReducer(state: ToastState, action: ToastAction): ToastState {
  switch (action.type) {
    case "ADD": {
      const toasts = [...state.toasts, action.toast];
      return { toasts: toasts.length > MAX_TOASTS ? toasts.slice(toasts.length - MAX_TOASTS) : toasts };
    }
    case "REMOVE":
      return { toasts: state.toasts.filter((toast) => toast.id !== action.id) };
    default:
      return state;
  }
}

export interface ToastContextValue {
  toasts: Toast[];
  addToast: (variant: ToastVariant, title: string, description?: string) => void;
  removeToast: (id: string) => void;
}

export const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(toastReducer, { toasts: [] });

  const addToast = useCallback((variant: ToastVariant, title: string, description?: string) => {
    dispatch({ type: "ADD", toast: { id: crypto.randomUUID(), variant, title, description } });
  }, []);

  const removeToast = useCallback((id: string) => {
    dispatch({ type: "REMOVE", id });
  }, []);

  const value = useMemo(
    () => ({ toasts: state.toasts, addToast, removeToast }),
    [state.toasts, addToast, removeToast]
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport toasts={state.toasts} onRemove={removeToast} />
    </ToastContext.Provider>
  );
}
