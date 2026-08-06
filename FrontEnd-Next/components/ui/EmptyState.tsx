import { ReactNode } from "react";

interface EmptyStateProps {
  message: ReactNode;
}

export function EmptyState({ message }: EmptyStateProps) {
  return (
    <div className="rounded-xl border border-dashed border-gray-300 px-6 py-8 text-center text-[12.5px] font-semibold text-gray-400">
      {message}
    </div>
  );
}
