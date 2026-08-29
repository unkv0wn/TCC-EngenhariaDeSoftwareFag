export interface CnpjCompanyAddress {
  zipCode: string;
  street: string;
  number: string;
  complement: string;
  district: string;
  city: string;
  state: string;
}

export interface CnpjCompany {
  name: string;
  tradeName: string;
  address: CnpjCompanyAddress;
}

interface BrasilApiCnpjResponse {
  razao_social?: string;
  nome_fantasia?: string;
  cep?: string;
  logradouro?: string;
  numero?: string;
  complemento?: string;
  bairro?: string;
  municipio?: string;
  uf?: string;
}

// BrasilAPI: free, no API key, no CORS restrictions — https://brasilapi.com.br
export async function fetchCompanyByCnpj(cnpj: string): Promise<CnpjCompany | null> {
  const digits = cnpj.replace(/\D/g, "");
  if (digits.length !== 14) return null;

  const response = await fetch(`https://brasilapi.com.br/api/cnpj/v1/${digits}`);
  if (!response.ok) return null;

  const data: BrasilApiCnpjResponse = await response.json();

  return {
    name: data.razao_social ?? "",
    tradeName: data.nome_fantasia ?? "",
    address: {
      zipCode: data.cep ?? "",
      street: data.logradouro ?? "",
      number: data.numero ?? "",
      complement: data.complemento ?? "",
      district: data.bairro ?? "",
      city: data.municipio ?? "",
      state: data.uf ?? "",
    },
  };
}
