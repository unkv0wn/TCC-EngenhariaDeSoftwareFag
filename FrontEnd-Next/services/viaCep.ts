export interface ViaCepAddress {
  street: string;
  district: string;
  city: string;
  state: string;
}

interface ViaCepResponse {
  erro?: boolean | string;
  logradouro?: string;
  bairro?: string;
  localidade?: string;
  uf?: string;
}

// ViaCEP: free, no API key, no CORS restrictions — https://viacep.com.br
export async function fetchAddressByZipCode(zipCode: string): Promise<ViaCepAddress | null> {
  const digits = zipCode.replace(/\D/g, "");
  if (digits.length !== 8) return null;

  const response = await fetch(`https://viacep.com.br/ws/${digits}/json/`);
  if (!response.ok) return null;

  const data: ViaCepResponse = await response.json();
  if (data.erro) return null;

  return {
    street: data.logradouro ?? "",
    district: data.bairro ?? "",
    city: data.localidade ?? "",
    state: data.uf ?? "",
  };
}
