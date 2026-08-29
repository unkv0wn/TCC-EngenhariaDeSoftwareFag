import { z } from "zod";

import { requiredNumber } from "@/lib/validations/zodNumber";

const ORDER_STATUS_VALUES = ["aguardando", "em_rota", "entregue", "cancelado"] as const;

export type OrderStatus = (typeof ORDER_STATUS_VALUES)[number];

export const ORDER_STATUSES: { value: OrderStatus; label: string }[] = [
  { value: "aguardando", label: "Aguardando" },
  { value: "em_rota", label: "Em rota" },
  { value: "entregue", label: "Entregue" },
  { value: "cancelado", label: "Cancelado" },
];

export const orderItemSchema = z.object({
  productId: z.string().min(1, "Selecione o produto."),
  quantity: requiredNumber("Informe a quantidade.").refine((value) => value > 0, {
    message: "Informe uma quantidade válida.",
  }),
  unitPrice: requiredNumber("Informe o preço.").refine((value) => value > 0, {
    message: "Informe um preço válido.",
  }),
});

export const orderSchema = z.object({
  customerId: z.string().min(1, "Selecione o cliente."),
  vehicleId: z.string().min(1, "Selecione o veículo."),
  driverId: z.string().min(1, "Selecione o motorista."),
  paymentMethodId: z.string().min(1, "Selecione a forma de pagamento."),
  paymentConditionId: z.string().min(1, "Selecione a condição de pagamento."),
  date: z.string().min(1, "Informe a data."),
  status: z.enum(ORDER_STATUS_VALUES, { message: "Selecione o status." }),
  items: z.array(orderItemSchema).min(1, "Adicione ao menos um item."),
  discount: requiredNumber("Informe o desconto.").refine((value) => value >= 0, {
    message: "O desconto não pode ser negativo.",
  }),
  shippingCost: requiredNumber("Informe o frete.").refine((value) => value >= 0, {
    message: "O frete não pode ser negativo.",
  }),
  notes: z.string().max(500, "Máximo de 500 caracteres.").optional(),
});

export type OrderItemFormData = z.infer<typeof orderItemSchema>;
export type OrderFormData = z.infer<typeof orderSchema>;
