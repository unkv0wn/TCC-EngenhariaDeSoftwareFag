export interface FipeOption {
  /** Código interno da Fipe — usado só pra buscar os modelos de uma marca, não é salvo. */
  code: string;
  name: string;
}

interface FipeBrandResponse {
  nome: string;
  valor: string;
}

interface FipeModelResponse {
  modelo: string;
  valor: string;
}

// BrasilAPI: free, no API key, no CORS restrictions — https://brasilapi.com.br
// (mesmo provedor já usado pra CNPJ em services/brasilApi.ts)

export async function fetchCarBrands(): Promise<FipeOption[]> {
  const response = await fetch("https://brasilapi.com.br/api/fipe/marcas/v1/carros");
  if (!response.ok) return [];

  const data: FipeBrandResponse[] = await response.json();
  return data
    .map((item) => ({ code: item.valor, name: item.nome }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

export async function fetchCarModelsByBrand(brandCode: string): Promise<FipeOption[]> {
  const response = await fetch(`https://brasilapi.com.br/api/fipe/veiculos/v1/carros/${brandCode}`);
  if (!response.ok) return [];

  const data: FipeModelResponse[] = await response.json();
  return data
    .map((item) => ({ code: item.valor, name: item.modelo }))
    .sort((a, b) => a.name.localeCompare(b.name));
}
