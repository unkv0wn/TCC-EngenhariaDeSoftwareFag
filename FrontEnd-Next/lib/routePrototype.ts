/**
 * PROTÓTIPO — dados e cálculos falsos usados só pra visualizar a tela de geração de rota
 * antes de existir a integração real com o backend (POST /api/routes/compute + SSE).
 *
 * Nada aqui é o algoritmo de verdade: é reta entre pontos + heurística de vizinho mais
 * próximo, só pra ter uma rota plausível pra desenhar no mapa.
 */

/** Coordenadas aproximadas das cidades que aparecem no cadastro de clientes (mock). */
export const CITY_COORDINATES: Record<string, { lat: number; lng: number }> = {
  "São Paulo": { lat: -23.5505, lng: -46.6333 },
  Curitiba: { lat: -25.4284, lng: -49.2733 },
  "Belo Horizonte": { lat: -19.9167, lng: -43.9345 },
  "Porto Alegre": { lat: -30.0346, lng: -51.2177 },
  "Rio de Janeiro": { lat: -22.9068, lng: -43.1729 },
  Florianópolis: { lat: -27.5954, lng: -48.548 },
};

/** Depósito/matriz fictício — origem e destino final da rota (modo "ida e volta"). */
export const DEPOT = { lat: -23.5505, lng: -46.6333, label: "Depósito (matriz)" };

/**
 * Mesma paleta de 10 cores citada no comentário do backend (`RouteResultDto.RouteSegmentDto`)
 * pra cada segmento da rota ter uma cor distinta.
 */
export const SEGMENT_COLORS = [
  "#4E79A7",
  "#F28E2B",
  "#E15759",
  "#76B7B2",
  "#59A14F",
  "#EDC948",
  "#B07AA1",
  "#FF9DA7",
  "#9C755F",
  "#BAB0AC",
];

/** Pequeno deslocamento determinístico pra clientes na mesma cidade não ficarem sobrepostos no mapa. */
export function jitterCoordinate(seed: string, coord: { lat: number; lng: number }) {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) hash = (hash * 31 + seed.charCodeAt(i)) % 10000;
  const angle = (hash / 10000) * Math.PI * 2;
  const radius = 0.015;
  return { lat: coord.lat + Math.cos(angle) * radius, lng: coord.lng + Math.sin(angle) * radius };
}

function toRad(deg: number): number {
  return (deg * Math.PI) / 180;
}

/** Distância em km entre duas coordenadas (fórmula de Haversine). */
export function haversineKm(a: { lat: number; lng: number }, b: { lat: number; lng: number }): number {
  const R = 6371;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
}

export interface RouteStop {
  orderId: string;
  label: string;
  coord: { lat: number; lng: number };
}

export interface RouteSegment {
  from: { label: string; coord: { lat: number; lng: number } };
  to: { label: string; coord: { lat: number; lng: number } };
  distanceKm: number;
  durationMin: number;
  color: string;
}

export interface PrototypeRouteResult {
  segments: RouteSegment[];
  totalDistanceKm: number;
  totalDurationMin: number;
}

const AVG_SPEED_KMH = 45;

/**
 * Ordena as paradas por vizinho mais próximo a partir do depósito (heurística simples, só
 * pra não desenhar uma rota em zigue-zague). O algoritmo real (A*) faz isso de verdade,
 * com custo de estrada, não linha reta.
 */
export function computePrototypeRoute(stops: RouteStop[]): PrototypeRouteResult {
  const remaining = [...stops];
  const ordered: RouteStop[] = [];
  let current: { lat: number; lng: number } = { lat: DEPOT.lat, lng: DEPOT.lng };

  while (remaining.length > 0) {
    let nearestIndex = 0;
    let nearestDist = Infinity;
    remaining.forEach((stop, index) => {
      const dist = haversineKm(current, stop.coord);
      if (dist < nearestDist) {
        nearestDist = dist;
        nearestIndex = index;
      }
    });
    const [next] = remaining.splice(nearestIndex, 1);
    ordered.push(next);
    current = next.coord;
  }

  const points = [{ label: DEPOT.label, coord: { lat: DEPOT.lat, lng: DEPOT.lng } }, ...ordered.map((s) => ({ label: s.label, coord: s.coord })), { label: DEPOT.label, coord: { lat: DEPOT.lat, lng: DEPOT.lng } }];

  const segments: RouteSegment[] = [];
  for (let i = 0; i < points.length - 1; i++) {
    const distanceKm = haversineKm(points[i].coord, points[i + 1].coord);
    segments.push({
      from: points[i],
      to: points[i + 1],
      distanceKm,
      durationMin: (distanceKm / AVG_SPEED_KMH) * 60,
      color: SEGMENT_COLORS[i % SEGMENT_COLORS.length],
    });
  }

  return {
    segments,
    totalDistanceKm: segments.reduce((sum, s) => sum + s.distanceKm, 0),
    totalDurationMin: segments.reduce((sum, s) => sum + s.durationMin, 0),
  };
}
