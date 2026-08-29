"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertCircle, CheckCircle2, Eye, EyeOff, Lock, Mail } from "lucide-react";

import { Button } from "@/components/ui/Button";
import { Checkbox } from "@/components/ui/Checkbox";
import { Divider } from "@/components/ui/Divider";
import { Input } from "@/components/ui/Input";
import { SocialButton } from "@/components/ui/SocialButton";
import { loginSchema, type LoginFormData } from "@/lib/validations/auth";
import { AuthError, login, loginWithProvider } from "@/services/auth";

type AuthStatus = "idle" | "loading" | "success" | "error";

export function LoginForm() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);
  const [status, setStatus] = useState<AuthStatus>("idle");
  const [authMessage, setAuthMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "", rememberMe: false },
  });

  async function onSubmit(data: LoginFormData) {
    setStatus("loading");
    setAuthMessage(null);
    try {
      await login(data);
      setStatus("success");
      setAuthMessage("Login realizado com sucesso!");
      router.push("/dashboard");
    } catch (err) {
      setStatus("error");
      setAuthMessage(
        err instanceof AuthError ? err.message : "Não foi possível entrar. Tente novamente."
      );
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
        <Input
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@empresa.com"
          startIcon={<Mail className="h-4 w-4" aria-hidden="true" />}
          error={errors.email?.message}
          {...register("email")}
        />

        <div className="flex flex-col gap-1.5">
          <Input
            label="Senha"
            type={showPassword ? "text" : "password"}
            autoComplete="current-password"
            placeholder="••••••••"
            startIcon={<Lock className="h-4 w-4" aria-hidden="true" />}
            error={errors.password?.message}
            endAdornment={
              <button
                type="button"
                onClick={() => setShowPassword((v) => !v)}
                aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                aria-pressed={showPassword}
                className="rounded text-gray-400 transition-colors hover:text-gray-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/30"
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" aria-hidden="true" />
                ) : (
                  <Eye className="h-4 w-4" aria-hidden="true" />
                )}
              </button>
            }
            {...register("password")}
          />
          <div className="flex items-center justify-between pt-0.5">
            <Checkbox label="Lembrar de mim" {...register("rememberMe")} />
            <a
              href="/forgot-password"
              className="text-sm font-medium text-gray-600 underline-offset-4 transition-colors hover:text-primary-700 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/30 rounded"
            >
              Esqueceu sua senha?
            </a>
          </div>
        </div>

        {status === "error" && authMessage && (
          <div
            role="alert"
            className="flex items-center gap-2 rounded-lg border border-danger-200 bg-danger-50 px-3.5 py-2.5 text-sm text-danger-700"
          >
            <AlertCircle className="h-4 w-4 shrink-0" aria-hidden="true" />
            {authMessage}
          </div>
        )}

        {status === "success" && authMessage && (
          <div
            role="status"
            className="flex items-center gap-2 rounded-lg border border-success-200 bg-success-50 px-3.5 py-2.5 text-sm text-success-700"
          >
            <CheckCircle2 className="h-4 w-4 shrink-0" aria-hidden="true" />
            {authMessage}
          </div>
        )}

        <Button type="submit" isLoading={status === "loading"}>
          {status === "loading" ? "Entrando..." : "Entrar"}
        </Button>
      </form>

      <Divider label="ou" />

      <div className="flex flex-col gap-3">
        <SocialButton provider="google" onClick={() => loginWithProvider("google")} />
        <SocialButton provider="microsoft" onClick={() => loginWithProvider("microsoft")} />
      </div>

      <p className="text-center text-sm text-gray-600">
        Não tem uma conta?{" "}
        <a
          href="/signup"
          className="font-medium text-primary-700 underline-offset-4 transition-colors hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/30 rounded"
        >
          Criar conta
        </a>
      </p>
    </div>
  );
}
