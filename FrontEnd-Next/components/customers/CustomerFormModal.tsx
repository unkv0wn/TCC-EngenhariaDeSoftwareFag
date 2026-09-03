"use client";

import { useEffect, useRef, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Divider } from "@/components/ui/Divider";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import type { Customer } from "@/hooks/useCustomers";
import { useToast } from "@/hooks/useToast";
import { maskDocument, maskPhone, maskZipCode } from "@/lib/masks";
import {
  BRAZIL_STATES,
  CUSTOMER_TYPES,
  PERSON_TYPES,
  customerSchema,
  type CustomerFormData,
  type PersonType,
} from "@/lib/validations/customer";
import { fetchCompanyByCnpj } from "@/services/brasilApi";
import { fetchAddressByZipCode } from "@/services/viaCep";

const LOOKUP_DEBOUNCE_MS = 600;

const EMPTY_VALUES: Partial<CustomerFormData> = {
  personType: "fisica",
  document: "",
  name: "",
  tradeName: "",
  email: "",
  phone: "",
  address: {
    zipCode: "",
    street: "",
    number: "",
    complement: "",
    district: "",
    city: "",
    state: "",
  },
};

interface CustomerFormModalProps {
  customer: Customer | null;
  onClose: () => void;
  onSubmit: (data: CustomerFormData) => void;
}

export function CustomerFormModal({ customer, onClose, onSubmit }: CustomerFormModalProps) {
  const isEditing = customer !== null;
  const { error } = useToast();

  const {
    register,
    handleSubmit,
    reset,
    resetField,
    setValue,
    watch,
    control,
    formState: { errors },
  } = useForm<CustomerFormData>({
    resolver: zodResolver(customerSchema),
    defaultValues: customer ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(customer ?? EMPTY_VALUES);
  }, [customer, reset]);

  const personType = watch("personType");
  const isJuridica = personType === "juridica";

  const [isLookingUpCep, setIsLookingUpCep] = useState(false);
  const [isLookingUpCnpj, setIsLookingUpCnpj] = useState(false);
  const cepTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const cnpjTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const cepRequestIdRef = useRef(0);
  const cnpjRequestIdRef = useRef(0);

  useEffect(() => {
    return () => {
      clearTimeout(cepTimerRef.current);
      clearTimeout(cnpjTimerRef.current);
    };
  }, []);

  // Debounced: fires LOOKUP_DEBOUNCE_MS after the CEP reaches 8 digits, cancelling on every
  // keystroke in between. A monotonic request id guards against an older response overwriting
  // a newer one if the user edits the field again before the first lookup resolves.
  function scheduleZipCodeLookup(maskedZipCode: string) {
    clearTimeout(cepTimerRef.current);
    if (maskedZipCode.replace(/\D/g, "").length !== 8) return;

    cepTimerRef.current = setTimeout(async () => {
      const requestId = ++cepRequestIdRef.current;
      setIsLookingUpCep(true);
      try {
        const address = await fetchAddressByZipCode(maskedZipCode);
        if (cepRequestIdRef.current !== requestId) return;
        if (!address) {
          error("CEP não encontrado", "Não foi possível localizar esse CEP. Preencha o endereço manualmente.");
          return;
        }
        setValue("address.street", address.street, { shouldValidate: true });
        setValue("address.district", address.district, { shouldValidate: true });
        setValue("address.city", address.city, { shouldValidate: true });
        setValue("address.state", address.state, { shouldValidate: true });
      } catch {
        if (cepRequestIdRef.current === requestId) {
          error("Não foi possível consultar o CEP", "Tente novamente ou preencha o endereço manualmente.");
        }
      } finally {
        if (cepRequestIdRef.current === requestId) setIsLookingUpCep(false);
      }
    }, LOOKUP_DEBOUNCE_MS);
  }

  function scheduleDocumentLookup(maskedDocument: string, currentPersonType: PersonType) {
    clearTimeout(cnpjTimerRef.current);
    if (currentPersonType !== "juridica" || maskedDocument.replace(/\D/g, "").length !== 14) return;

    cnpjTimerRef.current = setTimeout(async () => {
      const requestId = ++cnpjRequestIdRef.current;
      setIsLookingUpCnpj(true);
      try {
        const company = await fetchCompanyByCnpj(maskedDocument);
        if (cnpjRequestIdRef.current !== requestId) return;
        if (!company) {
          error("CNPJ não encontrado", "Não foi possível localizar esse CNPJ. Preencha os dados manualmente.");
          return;
        }
        setValue("name", company.name, { shouldValidate: true });
        setValue("tradeName", company.tradeName, { shouldValidate: true });
        setValue("address.zipCode", maskZipCode(company.address.zipCode), { shouldValidate: true });
        setValue("address.street", company.address.street, { shouldValidate: true });
        setValue("address.number", company.address.number, { shouldValidate: true });
        setValue("address.complement", company.address.complement, { shouldValidate: true });
        setValue("address.district", company.address.district, { shouldValidate: true });
        setValue("address.city", company.address.city, { shouldValidate: true });
        setValue("address.state", company.address.state, { shouldValidate: true });
      } catch {
        if (cnpjRequestIdRef.current === requestId) {
          error("Não foi possível consultar o CNPJ", "Tente novamente ou preencha os dados manualmente.");
        }
      } finally {
        if (cnpjRequestIdRef.current === requestId) setIsLookingUpCnpj(false);
      }
    }, LOOKUP_DEBOUNCE_MS);
  }

  return (
    <Modal title={isEditing ? "Editar cliente" : "Novo cliente"} onClose={onClose} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Divider label="Dados cadastrais" />

        <div className="grid grid-cols-2 gap-3">
          <Controller
            name="personType"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label="Tipo de pessoa"
                placeholder="Selecione..."
                options={PERSON_TYPES}
                value={field.value}
                onChange={(newValue) => {
                  field.onChange(newValue);
                  clearTimeout(cnpjTimerRef.current);
                  resetField("document");
                  resetField("tradeName");
                }}
                onBlur={field.onBlur}
                error={errors.personType?.message}
              />
            )}
          />
          <Controller
            name="type"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label="Tipo"
                placeholder="Selecione..."
                options={CUSTOMER_TYPES}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.type?.message}
              />
            )}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input
            label={isJuridica ? "CNPJ" : "CPF"}
            placeholder={isJuridica ? "00.000.000/0000-00" : "000.000.000-00"}
            error={errors.document?.message}
            {...register("document", {
              onChange: (event) => {
                const masked = maskDocument(event.target.value, personType);
                setValue("document", masked, { shouldValidate: false });
                scheduleDocumentLookup(masked, personType);
              },
            })}
          />
          <Input
            label={isJuridica ? "Razão social" : "Nome"}
            placeholder={isJuridica ? "Distribuidora ABC Ltda" : "Maria Souza"}
            error={errors.name?.message}
            {...register("name")}
          />
        </div>
        {isLookingUpCnpj && <p className="-mt-2 text-xs font-medium text-gray-400">Consultando CNPJ...</p>}

        {isJuridica && (
          <Input
            label="Nome fantasia"
            placeholder="ABC Bebidas"
            error={errors.tradeName?.message}
            {...register("tradeName")}
          />
        )}

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="E-mail"
            type="email"
            placeholder="contato@email.com"
            error={errors.email?.message}
            {...register("email")}
          />
          <Input
            label="Telefone"
            placeholder="(00) 00000-0000"
            error={errors.phone?.message}
            {...register("phone", {
              onChange: (event) => {
                setValue("phone", maskPhone(event.target.value), { shouldValidate: false });
              },
            })}
          />
        </div>

        <Divider label="Endereço" />

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="CEP"
            placeholder="00000-000"
            error={errors.address?.zipCode?.message}
            {...register("address.zipCode", {
              onChange: (event) => {
                const masked = maskZipCode(event.target.value);
                setValue("address.zipCode", masked, { shouldValidate: false });
                scheduleZipCodeLookup(masked);
              },
            })}
          />
          <Input
            label="Logradouro"
            placeholder="Avenida Paulista"
            error={errors.address?.street?.message}
            {...register("address.street")}
          />
        </div>
        {isLookingUpCep && <p className="-mt-2 text-xs font-medium text-gray-400">Consultando CEP...</p>}

        <div className="grid grid-cols-2 gap-3">
          <Input
            label="Número"
            placeholder="1000"
            error={errors.address?.number?.message}
            {...register("address.number")}
          />
          <Input
            label="Complemento"
            placeholder="Sala 2 (opcional)"
            error={errors.address?.complement?.message}
            {...register("address.complement")}
          />
        </div>

        <div className="grid grid-cols-3 gap-3">
          <Input
            label="Bairro"
            placeholder="Centro"
            error={errors.address?.district?.message}
            {...register("address.district")}
          />
          <Input
            label="Cidade"
            placeholder="São Paulo"
            error={errors.address?.city?.message}
            {...register("address.city")}
          />
          <Controller
            name="address.state"
            control={control}
            render={({ field }) => (
              <SearchableSelect
                label="UF"
                placeholder="Selecione..."
                options={BRAZIL_STATES}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errors.address?.state?.message}
              />
            )}
          />
        </div>

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar cliente
          </Button>
        </div>
      </form>
    </Modal>
  );
}
