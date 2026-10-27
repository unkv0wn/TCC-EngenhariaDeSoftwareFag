/**
 * Cliente HTTP fino pro backend Spring Boot. Usado pelos hooks de CRUD
 * (useUnits, usePaymentMethods, ...) que chamam a API diretamente do
 * navegador — diferente do fluxo de login, que passa pelas rotas /api do
 * Next.js por causa do cookie httpOnly.
 *
 * Toda chamada vai com `credentials: "include"` porque o backend passou a exigir sessão:
 * o cookie `routewise` é httpOnly, então a página não consegue lê-lo para montar um
 * cabeçalho `Authorization` — e é justamente por não conseguir que um XSS também não
 * consegue. O preço é que o CORS do backend precisa listar a origem explicitamente, já
 * que o navegador recusa curinga quando há credencial na chamada.
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

/**
 * Manda o navegador para o login, uma vez só.
 *
 * Várias telas disparam chamadas em paralelo, e uma sessão que expirou faz todas
 * responderem 401 juntas. Sem a trava, seriam vários redirecionamentos competindo.
 */
let redirecting = false;

function redirectToLogin() {
  if (typeof window === "undefined" || redirecting) return;
  redirecting = true;
  window.location.href = "/login";
}

interface ApiFetchOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
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
      credentials: "include",
    });
  } catch {
    throw new ApiError("Não foi possível conectar ao servidor. Verifique se o backend está rodando.");
  }

  if (response.status === 401) {
    // Sessão expirada ou cookie inválido. Sem isto, a tela continuaria aberta mostrando
    // erro em cada ação, porque o `proxy.ts` só olha o cookie na navegação entre páginas
    // — quem fica numa tela só nunca passa por ele. A página não consegue apagar o
    // cookie (é httpOnly); quem o apaga é o próprio proxy, ao redirecionar.
    redirectToLogin();
    throw new ApiError("Sua sessão expirou. Entre novamente para continuar.", 401);
  }

  if (!response.ok) {
    throw new ApiError(await parseErrorMessage(response), response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
