import { z } from "zod";

const PANEL_USER_STATUS_VALUES = ["ativo", "bloqueado"] as const;

export type PanelUserStatus = (typeof PANEL_USER_STATUS_VALUES)[number];

export const PANEL_USER_STATUSES: { value: PanelUserStatus; label: string }[] = [
  { value: "ativo", label: "Ativo" },
  { value: "bloqueado", label: "Bloqueado" },
];

export const panelUserSchema = z.object({
  name: z.string().min(1, "Informe o nome.").max(120, "Máximo de 120 caracteres."),
  email: z.string().min(1, "Informe o e-mail.").email("E-mail inválido.").max(160, "Máximo de 160 caracteres."),
  /**
   * Vazio só faz sentido na edição, e aí quer dizer "mantém a senha atual" — o modal
   * exige o campo na criação, porque ali não há senha para manter. Mesmo arranjo dos
   * acessos do app.
   */
  password: z.string().refine((value) => value === "" || value.length >= 6, {
    message: "A senha deve ter ao menos 6 caracteres.",
  }),
  status: z.enum(PANEL_USER_STATUS_VALUES, { message: "Selecione o status." }),
});

export type PanelUserFormData = z.infer<typeof panelUserSchema>;
