"use client";

import { LayoutGrid, List } from "lucide-react";
import { cn } from "@/lib/utils";

export type VehicleView = "cards" | "table";

interface ViewToggleProps {
  view: VehicleView;
  onChange: (view: VehicleView) => void;
}

export function ViewToggle({ view, onChange }: ViewToggleProps) {
  return (
    <div className="inline-flex items-center gap-0.5 rounded-lg border border-gray-200 bg-white p-0.5">
      <button
        type="button"
        onClick={() => onChange("cards")}
        aria-pressed={view === "cards"}
        aria-label="Visualizar em cards"
        className={cn(
          "flex h-7 w-8 items-center justify-center rounded-md",
          view === "cards" ? "bg-primary-50 text-primary-600" : "text-gray-400 hover:text-gray-600"
        )}
      >
        <LayoutGrid className="h-4 w-4" aria-hidden="true" />
      </button>
      <button
        type="button"
        onClick={() => onChange("table")}
        aria-pressed={view === "table"}
        aria-label="Visualizar em lista"
        className={cn(
          "flex h-7 w-8 items-center justify-center rounded-md",
          view === "table" ? "bg-primary-50 text-primary-600" : "text-gray-400 hover:text-gray-600"
        )}
      >
        <List className="h-4 w-4" aria-hidden="true" />
      </button>
    </div>
  );
}
