import type { LoginFormData } from "@/lib/validations/auth";

export interface AuthResponse {
  token: string;
  user: {
    id: string;
    name: string;
    email: string;
  };
}

export class AuthError extends Error {
  constructor(message: string, public status?: number) {
    super(message);
    this.name = "AuthError";
  }
}

// Backend ainda não conectado — chamada de rede removida temporariamente.
export async function login(_data: LoginFormData): Promise<AuthResponse> {
  throw new Error("login() está desconectado do backend por enquanto.");
}

export function loginWithProvider(provider: "google" | "microsoft") {
  console.warn(`loginWithProvider(${provider}) está desconectado do backend por enquanto.`);
}
