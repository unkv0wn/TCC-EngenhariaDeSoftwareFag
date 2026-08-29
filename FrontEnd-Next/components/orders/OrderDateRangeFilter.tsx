interface OrderDateRangeFilterProps {
  from: string;
  to: string;
  onChangeFrom: (value: string) => void;
  onChangeTo: (value: string) => void;
}

const INPUT_CLASSES =
  "rounded-lg border border-gray-200 bg-white py-2.5 px-3 text-sm text-gray-900 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15";

export function OrderDateRangeFilter({ from, to, onChangeFrom, onChangeTo }: OrderDateRangeFilterProps) {
  return (
    <div className="flex shrink-0 items-center gap-1.5">
      <input
        type="date"
        value={from}
        onChange={(event) => onChangeFrom(event.target.value)}
        aria-label="Data inicial"
        max={to || undefined}
        className={INPUT_CLASSES}
      />
      <span className="text-xs font-bold text-gray-400">até</span>
      <input
        type="date"
        value={to}
        onChange={(event) => onChangeTo(event.target.value)}
        aria-label="Data final"
        min={from || undefined}
        className={INPUT_CLASSES}
      />
    </div>
  );
}
