interface DividerProps {
  label?: string;
}

export function Divider({ label }: DividerProps) {
  if (!label) {
    return <hr className="border-t border-gray-200" />;
  }

  return (
    <div className="flex items-center gap-3" role="separator">
      <hr className="h-px flex-1 border-0 bg-gray-200" />
      <span className="text-xs font-medium uppercase tracking-wide text-gray-400">
        {label}
      </span>
      <hr className="h-px flex-1 border-0 bg-gray-200" />
    </div>
  );
}
