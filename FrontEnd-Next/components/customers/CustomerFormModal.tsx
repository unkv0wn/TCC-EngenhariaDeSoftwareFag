"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/Button";
import { Divider } from "@/components/ui/Divider";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import type { Customer } from "@/hooks/useCustomers";
import { maskDocument, maskPhone, maskZipCode } from "@/lib/masks";
import {
  BRAZIL_STATES,
  CUSTOMER_TYPES,
  PERSON_TYPES,
  customerSchema,
  type CustomerFormData,
} from "@/lib/validations/customer";

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

  const {
    register,
    handleSubmit,
    reset,
    resetField,
    setValue,
    watch,
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

  return (
    <Modal title={isEditing ? "Editar cliente" : "Novo cliente"} onClose={onClose} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <Divider label="Dados cadastrais" />

        <div className="grid grid-cols-2 gap-3">
          <Select
            label="Tipo de pessoa"
            options={PERSON_TYPES}
            error={errors.personType?.message}
            {...register("personType", {
              onChange: () => {
                resetField("document");
                resetField("tradeName");
              },
            })}
          />
          <Select
            label="Tipo"
            placeholder="Selecione..."
            options={CUSTOMER_TYPES}
            error={errors.type?.message}
            {...register("type")}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Input
            label={isJuridica ? "CNPJ" : "CPF"}
            placeholder={isJuridica ? "00.000.000/0000-00" : "000.000.000-00"}
            error={errors.document?.message}
            {...register("document", {
              onChange: (event) => {
                setValue("document", maskDocument(event.target.value, personType), { shouldValidate: false });
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
                setValue("address.zipCode", maskZipCode(event.target.value), { shouldValidate: false });
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
          <Select
            label="UF"
            placeholder="Selecione..."
            options={BRAZIL_STATES}
            error={errors.address?.state?.message}
            {...register("address.state")}
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
