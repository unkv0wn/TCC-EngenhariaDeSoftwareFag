import type { Metadata } from "next";
import { Route } from "lucide-react";

import { LoginForm } from "@/components/auth/LoginForm";

export const metadata: Metadata = {
  title: "Entrar",
  description: "Acesse sua conta para continuar.",
};

const APP_VERSION = "v0.1.0";

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-linear-to-b from-primary-50 via-white to-secondary-50 px-4 py-12 sm:px-6">
      <div className="flex w-full max-w-md flex-col items-center gap-6">
        <div
          className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary-600 text-white shadow-sm shadow-primary-600/20"
          aria-hidden="true"
        >
          <Route className="h-6 w-6" />
        </div>

        <div className="w-full rounded-2xl border border-gray-100 bg-white p-8 shadow-xl shadow-gray-900/5 sm:p-10">
          <div className="mb-8 flex flex-col items-center gap-2 text-center">
            <h1 className="text-2xl font-semibold tracking-tight text-gray-900">
              Entrar
            </h1>
            <p className="text-sm text-gray-500">
              Bem-vindo de volta! Insira seus dados para acessar sua conta.
            </p>
          </div>

          <LoginForm />
        </div>

        <footer className="text-center text-xs text-gray-400">
          {APP_VERSION} &middot; &copy; {new Date().getFullYear()} Todos os direitos reservados.
        </footer>
      </div>
    </main>
  );
}
