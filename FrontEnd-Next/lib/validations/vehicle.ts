import { z } from "zod";

const OLD_PLATE_REGEX = /^[A-Z]{3}-\d{4}$/;
const MERCOSUL_PLATE_REGEX = /^[A-Z]{3}\d[A-Z]\d{2}$/;

const FUEL_VALUES = ["diesel", "gasolina", "etanol", "eletrico"] as const;

export type FuelType = (typeof FUEL_VALUES)[number];

export const FUEL_TYPES: { value: FuelType; label: string }[] = [
  { value: "diesel", label: "Diesel" },
  { value: "gasolina", label: "Gasolina" },
  { value: "etanol", label: "Etanol" },
  { value: "eletrico", label: "Elétrico" },
];

const currentYear = new Date().getFullYear();

// react-hook-form's valueAsNumber turns an empty numeric input into NaN, not undefined —
// these checks catch NaN explicitly so the error message stays in Portuguese instead of
// falling through to zod's default "invalid_type" message.
export const vehicleSchema = z.object({
  plate: z
    .string()
    .min(1, "Informe a placa.")
    .transform((value) => value.toUpperCase())
    .refine((value) => OLD_PLATE_REGEX.test(value) || MERCOSUL_PLATE_REGEX.test(value), {
      message: "Informe uma placa válida (ABC-1234 ou ABC1D23).",
    }),
  model: z.string().min(1, "Informe o modelo."),
  brand: z.string().min(1, "Informe a marca."),
  year: z
    .number()
    .refine((value) => !Number.isNaN(value), { message: "Informe o ano." })
    .refine((value) => Number.isInteger(value) && value >= 1950 && value <= currentYear + 1, {
      message: "Informe um ano válido.",
    }),
  color: z.string().min(1, "Informe a cor."),
  capacityKg: z
    .number()
    .refine((value) => !Number.isNaN(value), { message: "Informe a capacidade." })
    .refine((value) => value > 0, { message: "Informe uma capacidade válida." }),
  fuelType: z.enum(FUEL_VALUES, { message: "Selecione o tipo de combustível." }),
});

export type VehicleFormData = z.infer<typeof vehicleSchema>;
