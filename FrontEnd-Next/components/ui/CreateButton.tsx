"use client";

import { Plus } from "lucide-react";

interface CreateButtonProps {
  label: string;
  onClick: () => void;
}

export function CreateButton({ label, onClick }: CreateButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2.5 text-[13.5px] font-bold text-white hover:bg-primary-700"
    >
      <Plus className="h-[15px] w-[15px]" aria-hidden="true" />
      {label}
    </button>
  );
}
