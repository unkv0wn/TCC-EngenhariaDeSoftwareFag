import { VehicleCard } from "@/components/vehicles/VehicleCard";
import type { Vehicle } from "@/hooks/useVehicles";

interface VehicleGridProps {
  vehicles: Vehicle[];
  onEdit: (vehicle: Vehicle) => void;
  onDelete: (vehicle: Vehicle) => void;
}

export function VehicleGrid({ vehicles, onEdit, onDelete }: VehicleGridProps) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
      {vehicles.map((vehicle) => (
        <VehicleCard key={vehicle.id} vehicle={vehicle} onEdit={onEdit} onDelete={onDelete} />
      ))}
    </div>
  );
}
