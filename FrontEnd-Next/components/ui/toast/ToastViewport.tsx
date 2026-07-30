"use client";

import { ToastCard } from "@/components/ui/toast/ToastCard";
import type { Toast } from "@/components/ui/toast/ToastProvider";

interface ToastViewportProps {
  toasts: Toast[];
  onRemove: (id: string) => void;
}

export function ToastViewport({ toasts, onRemove }: ToastViewportProps) {
  if (toasts.length === 0) return null;

  // Reducer keeps toasts[0] as oldest (needed for eviction); reverse only for
  // display so the newest toast renders closest to the top edge.
  const displayOrder = [...toasts].reverse();

  return (
    <div
      role="status"
      aria-live="polite"
      className="pointer-events-none fixed top-4 right-4 z-[100] flex w-full max-w-sm flex-col gap-2"
    >
      {displayOrder.map((toast) => (
        <div key={toast.id} className="pointer-events-auto">
          <ToastCard toast={toast} onClose={() => onRemove(toast.id)} />
        </div>
      ))}
    </div>
  );
}
