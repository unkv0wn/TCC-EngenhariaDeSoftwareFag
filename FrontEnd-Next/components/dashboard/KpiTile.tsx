import type { LucideIcon } from "lucide-react";

interface KpiTileProps {
  icon: LucideIcon;
  value: string;
  label: string;
}

export function KpiTile({ icon: Icon, value, label }: KpiTileProps) {
  return (
    <div className="flex flex-col gap-2.5 rounded-xl border border-gray-200 bg-white px-4 py-3.5">
      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary-50 text-primary-600">
        <Icon className="h-4 w-4" aria-hidden="true" />
      </div>
      <div className="text-2xl font-extrabold leading-none text-gray-900">{value}</div>
      <div className="text-[12.5px] font-semibold text-gray-500">{label}</div>
    </div>
  );
}
