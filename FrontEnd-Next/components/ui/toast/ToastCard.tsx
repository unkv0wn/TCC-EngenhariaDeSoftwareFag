"use client";

import { useCallback, useEffect, useRef } from "react";
import { AlertCircle, AlertTriangle, CheckCircle2, X, type LucideIcon } from "lucide-react";

import { cn } from "@/lib/utils";
import type { Toast, ToastVariant } from "@/components/ui/toast/ToastProvider";

const DURATION_MS = 5000;

const VARIANT_STYLES: Record<ToastVariant, { icon: LucideIcon; badge: string; bar: string }> = {
  success: { icon: CheckCircle2, badge: "bg-success-50 text-success-600", bar: "bg-success-500" },
  danger: { icon: AlertCircle, badge: "bg-danger-50 text-danger-600", bar: "bg-danger-500" },
  warning: { icon: AlertTriangle, badge: "bg-warning-50 text-warning-600", bar: "bg-warning-500" },
};

interface ToastCardProps {
  toast: Toast;
  onClose: () => void;
}

export function ToastCard({ toast, onClose }: ToastCardProps) {
  const { icon: Icon, badge, bar } = VARIANT_STYLES[toast.variant];

  const remainingRef = useRef(DURATION_MS);
  // Set for real by armTimer() in the mount effect below — Date.now() must not run during render.
  const startedAtRef = useRef(0);
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const barRef = useRef<HTMLDivElement>(null);

  const clearTimer = useCallback(() => {
    if (timeoutRef.current !== null) {
      clearTimeout(timeoutRef.current);
      timeoutRef.current = null;
    }
  }, []);

  const armTimer = useCallback(
    (durationMs: number) => {
      startedAtRef.current = Date.now();
      timeoutRef.current = setTimeout(onClose, durationMs);

      const barEl = barRef.current;
      if (barEl) {
        barEl.style.transitionDuration = "0ms";
        barEl.style.width = `${(remainingRef.current / DURATION_MS) * 100}%`;
        void barEl.offsetWidth; // force reflow so the reset width commits before re-enabling the transition
        barEl.style.transitionDuration = `${durationMs}ms`;
        barEl.style.width = "0%";
      }
    },
    [onClose]
  );

  useEffect(() => {
    armTimer(remainingRef.current);
    return clearTimer;
    // Runs once on mount only — pause/resume re-arm via the hover handlers below.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleMouseEnter() {
    clearTimer();
    const elapsed = Date.now() - startedAtRef.current;
    remainingRef.current = Math.max(remainingRef.current - elapsed, 0);

    const barEl = barRef.current;
    if (barEl) {
      const computedWidth = getComputedStyle(barEl).width;
      barEl.style.transitionDuration = "0ms";
      barEl.style.width = computedWidth;
    }
  }

  function handleMouseLeave() {
    if (remainingRef.current <= 0) {
      onClose();
      return;
    }
    armTimer(remainingRef.current);
  }

  return (
    <div
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      className="relative w-full overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-xl shadow-gray-900/10"
    >
      <div className="flex items-start gap-3 p-4">
        <div className={cn("flex h-8 w-8 shrink-0 items-center justify-center rounded-lg", badge)}>
          <Icon className="h-4 w-4" aria-hidden="true" />
        </div>

        <div className="min-w-0 flex-1">
          <p className="text-sm font-bold text-gray-900">{toast.title}</p>
          {toast.description && (
            <p className="mt-0.5 text-[13px] font-medium text-gray-500">{toast.description}</p>
          )}
        </div>

        <button
          type="button"
          onClick={onClose}
          aria-label="Fechar notificação"
          className="shrink-0 rounded-lg p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
        >
          <X className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <div ref={barRef} className={cn("absolute inset-x-0 bottom-0 h-[2.5px] transition-[width] ease-linear", bar)} />
    </div>
  );
}
