import { z } from "zod";

/** Configurações da empresa — o endereço/coordenada define o ponto de partida das rotas. */
export const companySettingsSchema = z.object({
  companyName: z.string().min(1, "Informe o nome da empresa."),
  address: z.object({
    zipCode: z.string().optional(),
    street: z.string().optional(),
    number: z.string().optional(),
    district: z.string().optional(),
    city: z.string().optional(),
    state: z.string().optional(),
  }),
  latitude: z.number().min(-90).max(90),
  longitude: z.number().min(-180).max(180),
});

export type CompanySettingsFormData = z.infer<typeof companySettingsSchema>;
