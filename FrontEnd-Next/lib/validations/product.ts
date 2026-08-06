import { z } from "zod";

import { requiredNumber } from "@/lib/validations/zodNumber";

export const productSchema = z.object({
  sku: z
    .string()
    .min(1, "Informe o código.")
    .transform((value) => value.toUpperCase()),
  name: z.string().min(1, "Informe o nome."),
  description: z.string().max(200, "Máximo de 200 caracteres.").optional(),
  unit: z.string().min(1, "Selecione a unidade."),
  unitPrice: requiredNumber("Informe o preço.").refine((value) => value > 0, {
    message: "Informe um preço válido.",
  }),
  weightKg: requiredNumber("Informe o peso.").refine((value) => value > 0, {
    message: "Informe um peso válido.",
  }),
});

export type ProductFormData = z.infer<typeof productSchema>;
