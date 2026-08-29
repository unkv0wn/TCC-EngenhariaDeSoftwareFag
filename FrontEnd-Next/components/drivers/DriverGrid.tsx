import { DriverCard } from "@/components/drivers/DriverCard";
import type { Driver } from "@/hooks/useDrivers";

interface DriverGridProps {
  drivers: Driver[];
  onEdit: (driver: Driver) => void;
  onDelete: (driver: Driver) => void;
}

export function DriverGrid({ drivers, onEdit, onDelete }: DriverGridProps) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
      {drivers.map((driver) => (
        <DriverCard key={driver.id} driver={driver} onEdit={onEdit} onDelete={onDelete} />
      ))}
    </div>
  );
}
