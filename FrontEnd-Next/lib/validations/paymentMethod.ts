import { z } from "zod";

export const paymentMethodSchema = z.object({
  name: z.string().min(1, "Informe o nome."),
});

export type PaymentMethodFormData = z.infer<typeof paymentMethodSchema>;
