import { z } from "zod";

const MOBILE_USER_STATUS_VALUES = ["ativo", "bloqueado"] as const;

export type MobileUserStatus = (typeof MOBILE_USER_STATUS_VALUES)[number];

export const MOBILE_USER_STATUSES: { value: MobileUserStatus; label: string }[] = [
  { value: "ativo", label: "Ativo" },
  { value: "bloqueado", label: "Bloqueado" },
];

/**
 * O identificador do aparelho é o `Settings.Secure.ANDROID_ID`, que o app mostra na
 * tela de login para o motorista enviar. Não é o IMEI: desde o Android 10 o IMEI exige
 * permissão privilegiada e nenhum app de terceiro consegue lê-lo.
 *
 * Aceita espaços porque é assim que o app o exibe (em grupos de quatro, para dar para
 * ler em voz alta) — colar daquele jeito tem que funcionar. A limpeza é no submit.
 */
const DEVICE_ID_PATTERN = /^[a-zA-Z0-9\s-]*$/;

export function normalizeDeviceId(value: string): string {
  return value.replace(/\s/g, "").trim();
}

export const mobileUserSchema = z.object({
  driverId: z.string().min(1, "Selecione o motorista."),
  username: z
    .string()
    .min(3, "O usuário precisa de ao menos 3 caracteres.")
    .max(60, "Máximo de 60 caracteres.")
    .regex(/^[a-zA-Z0-9._-]+$/, "Use apenas letras, números, ponto, hífen ou sublinhado."),
  /**
   * Vazio só faz sentido na edição, e aí quer dizer "mantém a senha atual" — o modal
   * exige o campo na criação, porque ali não há senha para manter.
   */
  password: z
    .string()
    .refine((value) => value === "" || value.length >= 6, {
      message: "A senha deve ter ao menos 6 caracteres.",
    }),
  deviceId: z
    .string()
    .regex(DEVICE_ID_PATTERN, "O identificador só tem letras, números e hífen.")
    .refine((value) => normalizeDeviceId(value).length <= 64, {
      message: "Identificador de aparelho muito longo.",
    }),
  deviceLabel: z.string().max(80, "Máximo de 80 caracteres."),
  status: z.enum(MOBILE_USER_STATUS_VALUES, { message: "Selecione o status." }),
});

export type MobileUserFormData = z.infer<typeof mobileUserSchema>;
