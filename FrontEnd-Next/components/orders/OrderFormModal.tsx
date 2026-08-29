"use client";

import { useEffect } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { Select } from "@/components/ui/Select";
import type { Customer } from "@/hooks/useCustomers";
import type { Driver } from "@/hooks/useDrivers";
import type { Order } from "@/hooks/useOrders";
import type { PaymentCondition } from "@/hooks/usePaymentConditions";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import type { Product } from "@/hooks/useProducts";
import type { Vehicle } from "@/hooks/useVehicles";
import { formatCurrency, formatWeight } from "@/lib/format";
import { calculateOrderTotal, calculateOrderWeightKg } from "@/lib/orderCalculations";
import { ORDER_STATUSES, orderSchema, type OrderFormData } from "@/lib/validations/order";

const EMPTY_VALUES: OrderFormData = {
  customerId: "",
  vehicleId: "",
  driverId: "",
  paymentMethodId: "",
  paymentConditionId: "",
  date: "",
  status: "aguardando",
  items: [{ productId: "", quantity: 1, unitPrice: 0 }],
};

interface OrderFormModalProps {
  order: Order | null;
  customers: Customer[];
  vehicles: Vehicle[];
  drivers: Driver[];
  products: Product[];
  paymentMethods: PaymentMethod[];
  paymentConditions: PaymentCondition[];
  onClose: () => void;
  onSubmit: (data: OrderFormData) => void;
}

export function OrderFormModal({
  order,
  customers,
  vehicles,
  drivers,
  products,
  paymentMethods,
  paymentConditions,
  onClose,
  onSubmit,
}: OrderFormModalProps) {
  const isEditing = order !== null;

  const {
    register,
    control,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<OrderFormData>({
    resolver: zodResolver(orderSchema),
    defaultValues: order ?? EMPTY_VALUES,
  });

  useEffect(() => {
    reset(order ?? EMPTY_VALUES);
  }, [order, reset]);

  const { fields, append, remove } = useFieldArray({ control, name: "items" });

  const items = watch("items");
  const vehicleId = watch("vehicleId");

  const totalPreview = calculateOrderTotal(items);
  const weightPreviewKg = calculateOrderWeightKg(items, products);
  const selectedVehicle = vehicles.find((vehicle) => vehicle.id === vehicleId);
  const isOverCapacity = !!selectedVehicle && weightPreviewKg > selectedVehicle.capacityKg;

  function handleProductChange(index: number, productId: string) {
    const product = products.find((candidate) => candidate.id === productId);
    if (product) {
      setValue(`items.${index}.unitPrice`, product.unitPrice, { shouldValidate: true });
    }
  }

  return (
    <Modal title={isEditing ? "Editar pedido" : "Novo pedido"} onClose={onClose} size="lg">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4 px-6 py-5">
        <div className={isEditing ? "grid grid-cols-2 gap-3" : ""}>
          <Select
            label="Cliente"
            placeholder="Selecione..."
            options={customers.map((customer) => ({ value: customer.id, label: customer.name }))}
            error={errors.customerId?.message}
            {...register("customerId")}
          />
          {isEditing && (
            <Select
              label="Status"
              options={ORDER_STATUSES}
              error={errors.status?.message}
              {...register("status")}
            />
          )}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Select
            label="Veículo"
            placeholder="Selecione..."
            options={vehicles.map((vehicle) => ({ value: vehicle.id, label: `${vehicle.plate} — ${vehicle.model}` }))}
            error={errors.vehicleId?.message}
            {...register("vehicleId")}
          />
          <Select
            label="Motorista"
            placeholder="Selecione..."
            options={drivers.map((driver) => ({ value: driver.id, label: driver.fullName }))}
            error={errors.driverId?.message}
            {...register("driverId")}
          />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Select
            label="Forma de pagamento"
            placeholder="Selecione..."
            options={paymentMethods.map((method) => ({ value: method.id, label: method.name }))}
            error={errors.paymentMethodId?.message}
            {...register("paymentMethodId")}
          />
          <Select
            label="Condição de pagamento"
            placeholder="Selecione..."
            options={paymentConditions.map((condition) => ({ value: condition.id, label: condition.name }))}
            error={errors.paymentConditionId?.message}
            {...register("paymentConditionId")}
          />
        </div>

        <Input label="Data" type="date" error={errors.date?.message} {...register("date")} />

        <div className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-gray-700">Itens</span>
            <button
              type="button"
              onClick={() => append({ productId: "", quantity: 1, unitPrice: 0 })}
              className="flex items-center gap-1 text-xs font-bold text-primary-700 hover:text-primary-800"
            >
              <Plus className="h-3.5 w-3.5" aria-hidden="true" />
              Adicionar item
            </button>
          </div>

          {errors.items?.message && (
            <p role="alert" className="text-xs font-medium text-red-600">
              {errors.items.message}
            </p>
          )}

          {fields.length > 0 && (
            <div className="flex items-center gap-2 px-0.5">
              <span className="min-w-0 flex-[2] text-xs font-semibold text-gray-500">Produto</span>
              <span className="w-20 text-xs font-semibold text-gray-500">Qtd.</span>
              <span className="w-28 text-xs font-semibold text-gray-500">Preço unit.</span>
              <span className="w-8" />
            </div>
          )}

          <div className="flex max-h-56 flex-col gap-2 overflow-y-auto pr-1">
            {fields.map((field, index) => (
              <div key={field.id} className="flex items-start gap-2">
                <div className="min-w-0 flex-[2]">
                  <Select
                    label=""
                    placeholder="Selecione..."
                    options={products.map((product) => ({ value: product.id, label: product.name }))}
                    error={errors.items?.[index]?.productId?.message}
                    {...register(`items.${index}.productId` as const, {
                      onChange: (event) => handleProductChange(index, event.target.value),
                    })}
                  />
                </div>
                <div className="w-20">
                  <Input
                    label=""
                    type="number"
                    placeholder="1"
                    error={errors.items?.[index]?.quantity?.message}
                    {...register(`items.${index}.quantity` as const, { valueAsNumber: true })}
                  />
                </div>
                <div className="w-28">
                  <Input
                    label=""
                    type="number"
                    step="0.01"
                    placeholder="0.00"
                    error={errors.items?.[index]?.unitPrice?.message}
                    {...register(`items.${index}.unitPrice` as const, { valueAsNumber: true })}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => remove(index)}
                  disabled={fields.length === 1}
                  aria-label="Remover item"
                  className="rounded-md p-2 text-gray-400 hover:bg-danger-50 hover:text-danger-600 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-1 rounded-lg bg-gray-50 px-3.5 py-2.5">
          <p className="text-xs font-semibold text-gray-500">
            Peso total: {formatWeight(weightPreviewKg)}
            {selectedVehicle && ` · Capacidade do veículo: ${formatWeight(selectedVehicle.capacityKg)}`}
          </p>
          <p className="text-sm font-bold text-gray-900">Total: {formatCurrency(totalPreview)}</p>
          {isOverCapacity && (
            <p role="alert" className="text-xs font-bold text-danger-600">
              O peso total excede a capacidade do veículo selecionado.
            </p>
          )}
        </div>

        <div className="mt-2 flex justify-end gap-2.5 border-t border-gray-100 pt-4">
          <Button type="button" variant="secondary" className="w-auto" onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" className="w-auto">
            Salvar pedido
          </Button>
        </div>
      </form>
    </Modal>
  );
}
