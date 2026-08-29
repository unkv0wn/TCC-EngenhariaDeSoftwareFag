import type { LoginFormData } from "@/lib/validations/auth";

export interface AuthResponse {
  email: string;
}

export class AuthError extends Error {
  constructor(message: string, public status?: number) {
    super(message);
    this.name = "AuthError";
  }
}

export async function login(data: LoginFormData): Promise<AuthResponse> {
  const response = await fetch("/api/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });

  const body = await response.json();

  if (!response.ok) {
    throw new AuthError(body.message ?? "Não foi possível entrar.", response.status);
  }

  return body as AuthResponse;
}

export function loginWithProvider(provider: "google" | "microsoft") {
  console.warn(`loginWithProvider(${provider}) está desconectado do backend por enquanto.`);
}