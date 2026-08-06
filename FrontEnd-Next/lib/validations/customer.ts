import { z } from "zod";

import { isValidCnpj, isValidCpf } from "@/lib/validations/document";

const PERSON_TYPE_VALUES = ["fisica", "juridica"] as const;
const CUSTOMER_TYPE_VALUES = ["cliente", "fornecedor", "ambos"] as const;

export type PersonType = (typeof PERSON_TYPE_VALUES)[number];
export type CustomerType = (typeof CUSTOMER_TYPE_VALUES)[number];

export const PERSON_TYPES: { value: PersonType; label: string }[] = [
  { value: "fisica", label: "Pessoa física" },
  { value: "juridica", label: "Pessoa jurídica" },
];

export const CUSTOMER_TYPES: { value: CustomerType; label: string }[] = [
  { value: "cliente", label: "Cliente" },
  { value: "fornecedor", label: "Fornecedor" },
  { value: "ambos", label: "Ambos" },
];

export const BRAZIL_STATES: { value: string; label: string }[] = [
  "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
  "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
  "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO",
].map((uf) => ({ value: uf, label: uf }));

const addressSchema = z.object({
  zipCode: z
    .string()
    .refine((value) => value.replace(/\D/g, "").length === 8, { message: "Informe um CEP válido." }),
  street: z.string().min(1, "Informe o logradouro."),
  number: z.string().min(1, "Informe o número."),
  complement: z.string().optional(),
  district: z.string().min(1, "Informe o bairro."),
  city: z.string().min(1, "Informe a cidade."),
  state: z.enum(BRAZIL_STATES.map((uf) => uf.value) as [string, ...string[]], {
    message: "Selecione a UF.",
  }),
});

export const customerSchema = z
  .object({
    personType: z.enum(PERSON_TYPE_VALUES, { message: "Selecione o tipo de pessoa." }),
    document: z.string().min(1, "Informe o documento."),
    name: z.string().min(1, "Informe o nome."),
    tradeName: z.string().optional(),
    type: z.enum(CUSTOMER_TYPE_VALUES, { message: "Selecione o tipo." }),
    email: z.string().min(1, "Informe o e-mail.").email("Informe um e-mail válido."),
    phone: z
      .string()
      .refine((value) => [10, 11].includes(value.replace(/\D/g, "").length), {
        message: "Informe um telefone válido.",
      }),
    address: addressSchema,
  })
  .superRefine((data, ctx) => {
    const isValid = data.personType === "fisica" ? isValidCpf(data.document) : isValidCnpj(data.document);
    if (!isValid) {
      ctx.addIssue({
        code: "custom",
        path: ["document"],
        message: data.personType === "fisica" ? "Informe um CPF válido." : "Informe um CNPJ válido.",
      });
    }
  });

export type CustomerFormData = z.infer<typeof customerSchema>;
