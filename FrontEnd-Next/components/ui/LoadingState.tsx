import { Loader2 } from "lucide-react";

interface LoadingStateProps {
  message?: string;
}

export function LoadingState({ message = "Carregando..." }: LoadingStateProps) {
  return (
    <div className="flex items-center justify-center gap-2 rounded-xl border border-dashed border-gray-300 px-6 py-8 text-[12.5px] font-semibold text-gray-400">
      <Loader2 className="h-4 w-4 animate-spin" aria-hidden="true" />
      {message}
    </div>
  );
}
