import { NextRequest, NextResponse } from "next/server";
import { jwtVerify } from "jose";

const COOKIE_NAME = "routewise";

function getSecretKey() {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw new Error("JWT_SECRET não está configurada — confira o .env.local");
  }
  return new TextEncoder().encode(secret);
}

export async function proxy(request: NextRequest) {
  const token = request.cookies.get(COOKIE_NAME)?.value;

  if (!token) {
    return redirectToLogin(request);
  }

  try {
    // Verifica assinatura + expiração do JWT — mesma secret que o backend usou pra assinar.
    // Se alguém adulterar o cookie ou ele estiver expirado, isso lança e cai no catch.
    await jwtVerify(token, getSecretKey());
    return NextResponse.next();
  } catch {
    return redirectToLogin(request);
  }
}

function redirectToLogin(request: NextRequest) {
  const loginUrl = new URL("/login", request.url);
  const response = NextResponse.redirect(loginUrl);
  // Limpa qualquer cookie inválido/expirado que ainda esteja no navegador.
  response.cookies.delete(COOKIE_NAME);
  return response;
}

export const config = {
  matcher: ["/dashboard/:path*"],
};
