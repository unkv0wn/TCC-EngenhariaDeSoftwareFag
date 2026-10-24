import { z } from "zod";

const API_CLIENT_STATUS_VALUES = ["ativo", "revogado"] as const;

export type ApiClientStatus = (typeof API_CLIENT_STATUS_VALUES)[number];

export const API_CLIENT_STATUSES: { value: ApiClientStatus; label: string }[] = [
  { value: "ativo", label: "Ativo" },
  { value: "revogado", label: "Revogado" },
];

/**
 * Escopos disponíveis. Espelha o `SCOPE_BY_PREFIX` do `IntegrationAuthFilter`, que é quem
 * de fato recusa — aqui a lista só evita oferecer um escopo que o servidor não conhece.
 *
 * Só leitura por enquanto: escrita externa (um ERP criando pedido) traz validação e
 * idempotência que ficaram fora deste escopo.
 */
export const API_CLIENT_SCOPES = [
  { value: "orders:read", label: "Pedidos (leitura)", hint: "Lista de pedidos, status e total" },
  { value: "routes:read", label: "Rotas (leitura)", hint: "Rotas, progresso e veículo" },
  { value: "deliveries:read", label: "Entregas (leitura)", hint: "O que foi entregue e quando" },
] as const;

const SCOPE_VALUES = API_CLIENT_SCOPES.map((scope) => scope.value) as [string, ...string[]];

export const apiClientSchema = z.object({
  name: z.string().min(1, "Informe o nome da integração.").max(120, "Máximo de 120 caracteres."),
  scopes: z.array(z.enum(SCOPE_VALUES)).min(1, "Selecione ao menos um escopo."),
  status: z.enum(API_CLIENT_STATUS_VALUES, { message: "Selecione o status." }),
});

export type ApiClientFormData = z.infer<typeof apiClientSchema>;

/** Rótulos dos desfechos do registro de acesso — espelha `IntegrationRequestLog`. */
export const INTEGRATION_OUTCOMES: Record<string, string> = {
  sucesso: "Sucesso",
  credencial_invalida: "Credencial inválida",
  cliente_revogado: "Integração revogada",
  token_invalido: "Token ausente ou inválido",
  escopo_insuficiente: "Escopo insuficiente",
};
