"use client";

import { ClipboardList, FileText, ListChecks } from "lucide-react";

import { cn } from "@/lib/utils";

export type TrackingTab = "paradas" | "eventos" | "documentos";

const TABS: { value: TrackingTab; label: string; icon: typeof ListChecks }[] = [
  { value: "paradas", label: "Paradas", icon: ListChecks },
  { value: "eventos", label: "Eventos", icon: ClipboardList },
  { value: "documentos", label: "Documentos", icon: FileText },
];

interface TrackingTabsProps {
  value: TrackingTab;
  onChange: (tab: TrackingTab) => void;
  /** Contador opcional por aba, ex.: { paradas: 9, eventos: 3 }. */
  counts?: Partial<Record<TrackingTab, number>>;
}

/**
 * Top bar da tela de acompanhamento. Troca o painel lateral, não o mapa — o mapa
 * fica fixo, porque é a referência visual enquanto se lê qualquer uma das abas.
 */
export function TrackingTabs({ value, onChange, counts }: TrackingTabsProps) {
  return (
    <div role="tablist" aria-label="Seções do acompanhamento" className="flex items-center gap-1 rounded-xl border border-gray-200 bg-white p-1">
      {TABS.map((tab) => {
        const Icon = tab.icon;
        const active = tab.value === value;
        const count = counts?.[tab.value];
        return (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={active}
            onClick={() => onChange(tab.value)}
            className={cn(
              "flex flex-1 items-center justify-center gap-1.5 rounded-lg px-3 py-2 text-[12.5px] font-bold transition-colors duration-150 ease-out",
              "focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:outline-none",
              active ? "bg-primary-600 text-white" : "text-gray-500 hover:bg-gray-100"
            )}
          >
            <Icon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
            {tab.label}
            {count !== undefined && (
              <span
                className={cn(
                  "rounded-full px-1.5 py-0.5 text-[10px] font-bold",
                  active ? "bg-white/20 text-white" : "bg-gray-100 text-gray-500"
                )}
              >
                {count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
