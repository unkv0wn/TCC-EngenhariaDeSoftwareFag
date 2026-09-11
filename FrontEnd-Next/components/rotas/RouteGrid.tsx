import { RouteCard } from "@/components/rotas/RouteCard";
import type { Driver } from "@/hooks/useDrivers";
import type { GeneratedRoute } from "@/hooks/useRoutes";
import type { Vehicle } from "@/hooks/useVehicles";

interface RouteGridProps {
  routes: GeneratedRoute[];
  drivers: Driver[];
  vehicles: Vehicle[];
}

export function RouteGrid({ routes, drivers, vehicles }: RouteGridProps) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
      {routes.map((route) => (
        <RouteCard key={route.id} route={route} drivers={drivers} vehicles={vehicles} />
      ))}
    </div>
  );
}
