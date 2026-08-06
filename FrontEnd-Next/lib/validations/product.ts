import { z } from "zod";

const UNIT_VALUES = ["un", "kg", "cx", "l"] as const;

export type ProductUnit = (typeof UNIT_VALUES)[number];

export const PRODUCT_UNITS: { value: ProductUnit; label: string }[] = [
  { value: "un", label: "Unidade (UN)" },
  { value: "kg", label: "Quilo (KG)" },
  { value: "cx", label: "Caixa (CX)" },
  { value: "l", label: "Litro (L)" },
];

// react-hook-form's valueAsNumber turns an empty numeric input into NaN, not undefined —
// these checks catch NaN explicitly so the error message stays in Portuguese instead of
// falling through to zod's default "invalid_type" message.
export const productSchema = z.object({
  sku: z
    .string()
    .min(1, "Informe o código.")
    .transform((value) => value.toUpperCase()),
  name: z.string().min(1, "Informe o nome."),
  description: z.string().max(200, "Máximo de 200 caracteres.").optional(),
  unit: z.enum(UNIT_VALUES, { message: "Selecione a unidade." }),
  unitPrice: z
    .number()
    .refine((value) => !Number.isNaN(value), { message: "Informe o preço." })
    .refine((value) => value > 0, { message: "Informe um preço válido." }),
  weightKg: z
    .number()
    .refine((value) => !Number.isNaN(value), { message: "Informe o peso." })
    .refine((value) => value > 0, { message: "Informe um peso válido." }),
});

export type ProductFormData = z.infer<typeof productSchema>;
