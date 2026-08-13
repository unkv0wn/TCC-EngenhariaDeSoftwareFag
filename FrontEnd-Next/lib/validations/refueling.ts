import { z } from "zod";

import { requiredNumber } from "@/lib/validations/zodNumber";

function todayIsoDate(): string {
  const now = new Date();
  const yyyy = now.getFullYear();
  const mm = String(now.getMonth() + 1).padStart(2, "0");
  const dd = String(now.getDate()).padStart(2, "0");
  return `${yyyy}-${mm}-${dd}`;
}

export const refuelingSchema = z.object({
  vehicleId: z.string().min(1, "Selecione o veículo."),
  driverId: z.string().min(1, "Selecione o motorista."),
  date: z
    .string()
    .min(1, "Informe a data.")
    .refine((value) => value <= todayIsoDate(), { message: "A data não pode ser futura." }),
  odometerKm: requiredNumber("Informe o odômetro.").refine((value) => value > 0, {
    message: "Informe um valor de odômetro válido.",
  }),
  litersRefueled: requiredNumber("Informe os litros abastecidos.").refine((value) => value > 0, {
    message: "Informe uma quantidade válida.",
  }),
  pricePerLiter: requiredNumber("Informe o preço por litro.").refine((value) => value > 0, {
    message: "Informe um preço válido.",
  }),
  location: z.string().max(120, "Máximo de 120 caracteres.").optional(),
});

export type RefuelingFormData = z.infer<typeof refuelingSchema>;
