/**
 * Cliente HTTP fino pro backend Spring Boot. Usado pelos hooks de CRUD
 * (useUnits, usePaymentMethods, ...) que chamam a API diretamente do
 * navegador — diferente do fluxo de login, que passa pelas rotas /api do
 * Next.js por causa do cookie httpOnly.
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export class ApiError extends Error {
  constructor(message: string, public status?: number) {
    super(message);
    this.name = "ApiError";
  }
}

/** Corpo de erro RFC 9457 que o `GlobalExceptionHandler` do backend devolve. */
interface ProblemDetail {
  title?: string;
  detail?: string;
}

async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as ProblemDetail;
    return body.detail ?? body.title ?? `Erro ${response.status}`;
  } catch {
    return `Erro ${response.status}`;
  }
}

interface ApiFetchOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
}

export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  if (!API_URL) {
    throw new ApiError("NEXT_PUBLIC_API_URL não está configurada — confira o .env.local.");
  }

  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method: options.method ?? "GET",
      headers: options.body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError("Não foi possível conectar ao servidor. Verifique se o backend está rodando.");
  }

  if (!response.ok) {
    throw new ApiError(await parseErrorMessage(response), response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
