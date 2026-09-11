/**
 * Configuração fixa do ponto de partida das rotas.
 *
 * O backend não tem cadastro de depósito/matriz — a rota é só uma lista de
 * waypoints. Pra o modo ROUND_TRIP, o A* fecha o ciclo voltando pro
 * `waypoints[0]`, então a matriz precisa ser sempre o primeiro waypoint.
 */
export const DEPOT_COORD = { lat: -23.5505, lng: -46.6333 } as const;

export const DEPOT_LABEL = "Depósito (matriz)";
