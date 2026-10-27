import { NextRequest, NextResponse } from "next/server";

/**
 * Login do painel.
 *
 * Esta rota existia para esconder um par fixo de credencial (`DataMock`) que morava aqui
 * no código do front: ela comparava e-mail e senha com esse par e, se batesse, pedia ao
 * backend um token — que o backend emitia para qualquer e-mail, sem conferir nada.
 *
 * Agora quem confere é o backend, contra `panel_users` e com senha em BCrypt. Esta rota
 * continua existindo por um motivo só, que não mudou: o cookie é `httpOnly`, e só pode
 * ser gravado pelo servidor. É o que impede o JavaScript da página — e portanto um XSS —
 * de ler o token.
 */

const COOKIE_NAME = "routewise";

/** Precisa bater com `routewise.jwt.expiration-ms` do backend: o cookie não deve
 *  sobreviver ao token, senão a sessão parece viva e toda chamada responde 401. */
const MAX_AGE_SECONDS = 3600;

export async function POST(request: NextRequest) {
  const { email, password } = await request.json();

  let backendResponse: Response;
  try {
    backendResponse = await fetch(`${process.env.BACKEND_URL}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    });
  } catch {
    return NextResponse.json(
      { message: "Não foi possível falar com o servidor. Verifique se o backend está rodando." },
      { status: 503 }
    );
  }

  if (!backendResponse.ok) {
    // 401 (credencial errada) e 403 (acesso bloqueado) dizem coisas diferentes para quem
    // está tentando entrar, então repassamos o status e a frase do backend em vez de
    // traduzir tudo para um "não foi possível entrar" genérico.
    const problem = await backendResponse.json().catch(() => null);
    const message =
      problem?.detail ?? problem?.title ?? "Não foi possível entrar. Tente novamente.";
    return NextResponse.json({ message }, { status: backendResponse.status });
  }

  const { token, name } = await backendResponse.json();

  const response = NextResponse.json({ email, name });

  response.cookies.set(COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: MAX_AGE_SECONDS,
  });

  return response;
}
