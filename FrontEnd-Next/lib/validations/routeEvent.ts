import { z } from "zod";

/**
 * Espelha as listas fechadas de `RouteEventServiceImpl` no backend. Se um tipo for
 * adicionado lá, precisa entrar aqui também — igual ao que acontece com as transições
 * de status de pedido (`useOrders.ts` ↔ `OrderServiceImpl`).
 */
const EVENT_TYPE_VALUES = ["saida", "chegada", "entrega", "ocorrencia", "abastecimento", "retorno"] as const;

export type RouteEventTypeValue = (typeof EVENT_TYPE_VALUES)[number];

/** Tipos que acontecem numa parada específica — o backend exige `stopIndex` neles. */
export const TYPES_REQUIRING_STOP: RouteEventTypeValue[] = ["chegada", "entrega"];

export const EVENT_TYPES: { value: RouteEventTypeValue; label: string }[] = [
  { value: "saida", label: "Saída do depósito" },
  { value: "chegada", label: "Chegada na parada" },
  { value: "entrega", label: "Entrega concluída" },
  { value: "ocorrencia", label: "Ocorrência" },
  { value: "abastecimento", label: "Abastecimento" },
  { value: "retorno", label: "Retorno ao depósito" },
];

const AUTHOR_ROLE_VALUES = ["motorista", "gestor"] as const;

export type RouteEventAuthorRoleValue = (typeof AUTHOR_ROLE_VALUES)[number];

export const AUTHOR_ROLES: { value: RouteEventAuthorRoleValue; label: string }[] = [
  { value: "motorista", label: "Motorista" },
  { value: "gestor", label: "Gestor" },
];

export const routeEventSchema = z
  .object({
    type: z.enum(EVENT_TYPE_VALUES, { message: "Selecione o tipo do evento." }),
    // "" representa "nenhuma parada" — é o valor que um <select> vazio produz.
    stopIndex: z.string(),
    authorRole: z.enum(AUTHOR_ROLE_VALUES, { message: "Selecione quem está registrando." }),
    authorName: z.string().min(1, "Informe o nome de quem está registrando.").max(120, "Nome muito longo."),
    notes: z.string().max(2000, "Observação muito longa.").optional(),
  })
  .refine((data) => !TYPES_REQUIRING_STOP.includes(data.type) || data.stopIndex !== "", {
    message: "Selecione a parada deste evento.",
    path: ["stopIndex"],
  });

export type RouteEventFormData = z.infer<typeof routeEventSchema>;
