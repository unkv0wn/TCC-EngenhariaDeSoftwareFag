import { z } from "zod";

export const unitSchema = z.object({
  code: z
    .string()
    .min(1, "Informe o código.")
    .max(10, "Máximo de 10 caracteres.")
    .transform((value) => value.toUpperCase()),
  name: z.string().min(1, "Informe o nome."),
});

export type UnitFormData = z.infer<typeof unitSchema>;
