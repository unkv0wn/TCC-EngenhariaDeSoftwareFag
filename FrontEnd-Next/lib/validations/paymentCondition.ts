import { z } from "zod";

import { requiredNumber } from "@/lib/validations/zodNumber";

export const paymentConditionSchema = z.object({
  name: z.string().min(1, "Informe o nome."),
  installments: requiredNumber("Informe o número de parcelas.").refine((value) => value >= 1, {
    message: "Informe ao menos 1 parcela.",
  }),
  intervalDays: requiredNumber("Informe o intervalo entre parcelas.").refine((value) => value >= 0, {
    message: "Informe um valor válido.",
  }),
});

export type PaymentConditionFormData = z.infer<typeof paymentConditionSchema>;
