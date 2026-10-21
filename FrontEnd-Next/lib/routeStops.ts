/**
 * Converte pedidos selecionados nas paradas que vão virar waypoints da rota.
 *
 * Um cliente costuma ter mais de um pedido, e todos saem no mesmo endereço. Cada
 * endereço tem que virar UMA parada: mandar a mesma coordenada duas vezes faz o A*
 * resolver um TSP com pontos duplicados (trechos de 0 km, espaço de estados
 * inflado) e empilha dois marcadores no mesmo pixel do mapa, escondendo a
 * numeração de um deles.
 *
 * Fica fora da tela porque a contagem de paradas é necessária antes de gerar a
 * rota — é ela, e não a de pedidos, que precisa caber no limite do backend.
 */
import type { Customer } from "@/hooks/useCustomers";
import type { Order } from "@/hooks/useOrders";
import { CITY_COORDINATES, jitterCoordinate } from "@/lib/routePrototype";
import type { LabeledStop } from "@/lib/routeResult";

export function buildRouteStops(orders: Order[], customers: Customer[]): LabeledStop[] {
  const byLocation = new Map<string, LabeledStop>();

  for (const order of orders) {
    const customer = customers.find((option) => option.id === order.customerId);
    const label = customer?.name ?? "Cliente";

    // Coordenada real do cliente (geocoding no cadastro). Sem ela, cai pro
    // aproximado por cidade + jitter, só pra rota não quebrar. O jitter usa o
    // cliente como semente, não o pedido, senão dois pedidos do mesmo cliente sem
    // coordenada cairiam em pontos diferentes e voltariam a duplicar parada.
    const lat = customer?.address.latitude;
    const lng = customer?.address.longitude;
    const coord =
      lat != null && lng != null
        ? { lat, lng }
        : jitterCoordinate(
            order.customerId ?? order.id,
            (customer ? CITY_COORDINATES[customer.address.city] : undefined) ?? CITY_COORDINATES["São Paulo"]
          );

    const key = `${coord.lat},${coord.lng}`;
    const existing = byLocation.get(key);
    if (existing) {
      existing.ordersCount = (existing.ordersCount ?? 1) + 1;
      existing.orderIds = [...(existing.orderIds ?? []), order.id];
    } else {
      // `orderIds` acompanha a contagem porque é o vínculo que fecha o pedido quando a
      // entrega é registrada. Agrupar por endereço sem guardar os ids deixaria a parada
      // sabendo *quantos* pedidos entrega e não *quais* — e era isso que mantinha o
      // ciclo do pedido aberto. Ver `RouteOrderLifecycle` no backend.
      byLocation.set(key, { label, lat: coord.lat, lng: coord.lng, ordersCount: 1, orderIds: [order.id] });
    }
  }

  return [...byLocation.values()];
}
