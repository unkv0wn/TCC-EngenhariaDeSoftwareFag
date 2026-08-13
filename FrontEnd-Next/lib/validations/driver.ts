import { z } from "zod";

import { isValidCpf } from "@/lib/validations/document";

const CNH_CATEGORY_VALUES = ["A", "B", "C", "D", "E", "AB", "AC", "AD", "AE"] as const;
const DRIVER_STATUS_VALUES = ["ativo", "inativo"] as const;

export type CnhCategory = (typeof CNH_CATEGORY_VALUES)[number];
export type DriverStatus = (typeof DRIVER_STATUS_VALUES)[number];

export const CNH_CATEGORIES: { value: CnhCategory; label: string }[] = CNH_CATEGORY_VALUES.map((value) => ({
  value,
  label: value,
}));

export const DRIVER_STATUSES: { value: DriverStatus; label: string }[] = [
  { value: "ativo", label: "Ativo" },
  { value: "inativo", label: "Inativo" },
];

export const driverSchema = z
  .object({
    fullName: z.string().min(1, "Informe o nome completo."),
    cpf: z.string().refine(isValidCpf, { message: "Informe um CPF válido." }),
    phone: z
      .string()
      .refine((value) => [10, 11].includes(value.replace(/\D/g, "").length), {
        message: "Informe um telefone válido.",
      }),
    cnhNumber: z
      .string()
      .refine((value) => value.replace(/\D/g, "").length === 11, {
        message: "Informe um número de CNH válido (11 dígitos).",
      }),
    cnhCategory: z.enum(CNH_CATEGORY_VALUES, { message: "Selecione a categoria da CNH." }),
    cnhValidity: z.string().min(1, "Informe a validade da CNH."),
    status: z.enum(DRIVER_STATUS_VALUES, { message: "Selecione o status." }),
  });

export type DriverFormData = z.infer<typeof driverSchema>;
