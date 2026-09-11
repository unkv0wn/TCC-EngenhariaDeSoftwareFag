/**
 * Geocoding do endereço do cliente → coordenada, pra usar como waypoint na rota.
 *
 * Dois níveis:
 *  1. Nominatim (OSM) — busca por rua + bairro + cidade. Quando encontra a via de
 *     verdade, é bem preciso (inclusive interior). Rejeitamos resultado que caia
 *     só em cidade/estado (baixa confiança).
 *  2. BrasilAPI CEP v2 — fallback. Devolve a coordenada do CEP (nível de quadra),
 *     que em cidade pequena pode vir imprecisa, mas sempre acerta a cidade.
 *
 * Se os dois falharem, o modal abre no centro da cidade e o usuário posiciona o
 * pino na mão. Ambos são chamados direto do navegador (sem CORS).
 */

export interface GeoCoord {
  lat: number;
  lng: number;
}

export interface GeocodeAddress {
  street?: string;
  number?: string;
  district?: string;
  city?: string;
  state?: string;
  zipCode?: string;
}

function parseCoord(lat: unknown, lng: unknown): GeoCoord | null {
  const latNum = typeof lat === "string" ? Number(lat) : (lat as number);
  const lngNum = typeof lng === "string" ? Number(lng) : (lng as number);
  if (!Number.isFinite(latNum) || !Number.isFinite(lngNum)) return null;
  if (latNum < -90 || latNum > 90 || lngNum < -180 || lngNum > 180) return null;
  if (latNum === 0 && lngNum === 0) return null;
  return { lat: latNum, lng: lngNum };
}

/** Coordenada a partir do CEP (BrasilAPI v2). `null` se o CEP não tiver coordenada. */
export async function geocodeByCep(zipCode: string): Promise<GeoCoord | null> {
  const digits = zipCode.replace(/\D/g, "");
  if (digits.length !== 8) return null;

  try {
    const response = await fetch(`https://brasilapi.com.br/api/cep/v2/${digits}`);
    if (!response.ok) return null;
    const data: { location?: { coordinates?: { latitude?: string; longitude?: string } } } = await response.json();
    const coords = data.location?.coordinates;
    if (!coords) return null;
    return parseCoord(coords.latitude, coords.longitude);
  } catch {
    return null;
  }
}

// Tipos de resultado do Nominatim que representam um ponto específico o bastante
// pra usar como endereço de entrega. `city`, `administrative`, `state` etc. são
// coarse demais — nesses casos preferimos o fallback pelo CEP.
const PRECISE_ADDRESS_TYPES = new Set([
  "house_number",
  "building",
  "road",
  "residential",
  "neighbourhood",
  "suburb",
  "quarter",
  "hamlet",
  "place",
]);

/**
 * Coordenada a partir do endereço (Nominatim / OpenStreetMap).
 * Retorna `null` se não encontrar ou se o match for coarse demais (só cidade/UF).
 */
export async function geocodeByAddress(address: GeocodeAddress): Promise<GeoCoord | null> {
  if (!address.city || !address.street) return null;

  const query = [
    [address.street, address.number].filter(Boolean).join(", "),
    address.district,
    address.city,
    address.state,
    "Brasil",
  ]
    .filter(Boolean)
    .join(", ");

  try {
    const url = `https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=br&addressdetails=0&q=${encodeURIComponent(query)}`;
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    if (!response.ok) return null;
    const data: { lat?: string; lon?: string; addresstype?: string; type?: string }[] = await response.json();
    if (!Array.isArray(data) || data.length === 0) return null;

    const hit = data[0];
    const kind = hit.addresstype ?? hit.type ?? "";
    if (!PRECISE_ADDRESS_TYPES.has(kind)) return null;

    return parseCoord(hit.lat, hit.lon);
  } catch {
    return null;
  }
}

/** Tenta o endereço (mais preciso) primeiro, cai pro CEP. `null` se nenhum resolver. */
export async function geocodeCustomerAddress(address: GeocodeAddress): Promise<GeoCoord | null> {
  const byAddress = await geocodeByAddress(address);
  if (byAddress) return byAddress;
  return address.zipCode ? geocodeByCep(address.zipCode) : null;
}
