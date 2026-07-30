import { z } from "zod";

export const loginSchema = z.object({
  email: z
    .string()
    .min(1, "Informe seu e-mail.")
    .email("Informe um e-mail válido."),
  password: z
    .string()
    .min(1, "Informe sua senha.")
    .min(8, "A senha deve ter no mínimo 8 caracteres."),
  rememberMe: z.boolean(),
});

export type LoginFormData = z.infer<typeof loginSchema>;
