"use client";

import { cn } from "@/lib/utils";

export type CustomerTypeFilterValue = "todos" | "cliente" | "fornecedor";

const OPTIONS: { value: CustomerTypeFilterValue; label: string }[] = [
  { value: "todos", label: "Todos" },
  { value: "cliente", label: "Clientes" },
  { value: "fornecedor", label: "Fornecedores" },
];

interface CustomerTypeFilterProps {
  value: CustomerTypeFilterValue;
  onChange: (value: CustomerTypeFilterValue) => void;
}

export function CustomerTypeFilter({ value, onChange }: CustomerTypeFilterProps) {
  return (
    <div className="inline-flex items-center gap-0.5 rounded-lg border border-gray-200 bg-white p-0.5">
      {OPTIONS.map((option) => (
        <button
          key={option.value}
          type="button"
          onClick={() => onChange(option.value)}
          aria-pressed={value === option.value}
          className={cn(
            "flex h-7 items-center justify-center rounded-md px-2.5 text-[12px] font-bold",
            value === option.value ? "bg-primary-50 text-primary-600" : "text-gray-400 hover:text-gray-600"
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
