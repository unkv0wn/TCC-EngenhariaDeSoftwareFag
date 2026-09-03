"use client";

import { useEffect, useState } from "react";
import { Controller, useFieldArray, useForm, type FieldErrors } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Plus, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Modal } from "@/components/ui/Modal";
import { SearchableSelect } from "@/components/ui/SearchableSelect";
import { Textarea } from "@/components/ui/Textarea";
import type { Customer } from "@/hooks/useCustomers";
import type { Driver } from "@/hooks/useDrivers";
import type { Order } from "@/hooks/useOrders";
import type { PaymentCondition } from "@/hooks/usePaymentConditions";
import type { PaymentMethod } from "@/hooks/usePaymentMethods";
import type { Product } from "@/hooks/useProducts";
import type { Vehicle } from "@/hooks/useVehicles";
import { formatCurrency, formatDate, formatDateTime, formatWeight } from "@/lib/format";
import {
  calculateInstallments,
  calculateOrderSubtotal,
  calculateOrderTotal,
  calculateOrderWeightKg,
} from "@/lib/orderCalculations";
import { cn } from "@/lib/utils";
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
  discount: 0,
  shippingCost: 0,
  notes: "",
};

const CLIENT_FIELDS = ["customerId", "vehicleId", "driverId", "date", "notes"] as const;
const PAYMENT_FIELDS = ["paymentMethodId", "paymentConditionId"] as const;

type Tab = "cliente" | "itens" | "pagamento";

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
  const [activeTab, setActiveTab] = useState<Tab>("cliente");

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
    setActiveTab("cliente");
  }, [order, reset]);

  const { fields, append, remove } = useFieldArray({ control, name: "items" });

  const items = watch("items");
  const vehicleId = watch("vehicleId");
  const date = watch("date");
  const discount = watch("discount");
  const shippingCost = watch("shippingCost");
  const paymentConditionId = watch("paymentConditionId");

  const subtotalPreview = calculateOrderSubtotal(items);
  const totalPreview = calculateOrderTotal(items, discount, shippingCost);
  const weightPreviewKg = calculateOrderWeightKg(items, products);
  const selectedVehicle = vehicles.find((vehicle) => vehicle.id === vehicleId);
  const isOverCapacity = !!selectedVehicle && weightPreviewKg > selectedVehicle.capacityKg;

  const selectedPaymentCondition = paymentConditions.find((condition) => condition.id === paymentConditionId);
  const installments = selectedPaymentCondition
    ? calculateInstallments(totalPreview, selectedPaymentCondition.installments, selectedPaymentCondition.intervalDays, date)
    : [];

  const clientHasError = CLIENT_FIELDS.some((field) => errors[field]);
  const itemsHasError = !!errors.items || !!errors.discount || !!errors.shippingCost;
  const paymentHasError = PAYMENT_FIELDS.some((field) => errors[field]);

  function handleProductChange(index: number, productId: string) {
    const product = products.find((candidate) => candidate.id === productId);
    if (product) {
      setValue(`items.${index}.unitPrice`, product.unitPrice, { shouldValidate: true });
    }
  }

  function onInvalid(formErrors: FieldErrors<OrderFormData>) {
    if (CLIENT_FIELDS.some((field) => formErrors[field])) {
      setActiveTab("cliente");
    } else if (formErrors.items || formErrors.discount || formErrors.shippingCost) {
      setActiveTab("itens");
    } else {
      setActiveTab("pagamento");
    }
  }

  return (
    <Modal title={isEditing ? "Editar pedido" : "Novo pedido"} onClose={onClose} size="lg">
      <form onSubmit={handleSubmit(onSubmit, onInvalid)} noValidate className="flex flex-col">
        <div className="grid grid-cols-3 border-b border-gray-100 px-6">
          <TabButton label="Cliente" active={activeTab === "cliente"} hasError={clientHasError} onClick={() => setActiveTab("cliente")} />
          <TabButton label="Itens" active={activeTab === "itens"} hasError={itemsHasError} onClick={() => setActiveTab("itens")} />
          <TabButton
            label="Pagamento"
            active={activeTab === "pagamento"}
            hasError={paymentHasError}
            onClick={() => setActiveTab("pagamento")}
          />
        </div>

        <div className="flex flex-col gap-4 px-6 py-5">
          {activeTab === "cliente" && (
            <>
              <Controller
                name="customerId"
                control={control}
                render={({ field }) => (
                  <SearchableSelect
                    label="Cliente"
                    placeholder="Selecione..."
                    options={customers.map((customer) => ({ value: customer.id, label: customer.name }))}
                    value={field.value}
                    onChange={field.onChange}
                    onBlur={field.onBlur}
                    error={errors.customerId?.message}
                  />
                )}
              />

              <div className="grid grid-cols-2 gap-3">
                <Controller
                  name="vehicleId"
                  control={control}
                  render={({ field }) => (
                    <SearchableSelect
                      label="Veículo"
                      placeholder="Selecione..."
                      options={vehicles.map((vehicle) => ({ value: vehicle.id, label: `${vehicle.plate} — ${vehicle.model}` }))}
                      value={field.value}
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      error={errors.vehicleId?.message}
                    />
                  )}
                />
                <Controller
                  name="driverId"
                  control={control}
                  render={({ field }) => (
                    <SearchableSelect
                      label="Motorista"
                      placeholder="Selecione..."
                      options={drivers.map((driver) => ({ value: driver.id, label: driver.fullName }))}
                      value={field.value}
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      error={errors.driverId?.message}
                    />
                  )}
                />
              </div>

              <Input label="Data" type="date" error={errors.date?.message} {...register("date")} />

              <Textarea
                label="Observações"
                placeholder="Instruções de entrega, observações do cliente..."
                rows={2}
                error={errors.notes?.message}
                {...register("notes")}
              />
            </>
          )}

          {activeTab === "itens" && (
            <>
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
                        <Controller
                          name={`items.${index}.productId` as const}
                          control={control}
                          render={({ field }) => (
                            <SearchableSelect
                              label=""
                              placeholder="Selecione..."
                              options={products.map((product) => ({ value: product.id, label: product.name }))}
                              value={field.value}
                              onChange={(newValue) => {
                                field.onChange(newValue);
                                handleProductChange(index, newValue);
                              }}
                              onBlur={field.onBlur}
                              error={errors.items?.[index]?.productId?.message}
                            />
                          )}
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

              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="Desconto (R$)"
                  type="number"
                  step="0.01"
                  placeholder="0.00"
                  error={errors.discount?.message}
                  {...register("discount", { valueAsNumber: true })}
                />
                <Input
                  label="Frete (R$)"
                  type="number"
                  step="0.01"
                  placeholder="0.00"
                  error={errors.shippingCost?.message}
                  {...register("shippingCost", { valueAsNumber: true })}
                />
              </div>

              <div className="flex flex-col gap-1 rounded-lg bg-gray-50 px-3.5 py-2.5">
                <p className="text-xs font-semibold text-gray-500">
                  Peso total: {formatWeight(weightPreviewKg)}
                  {selectedVehicle && ` · Capacidade do veículo: ${formatWeight(selectedVehicle.capacityKg)}`}
                </p>
                <p className="text-xs font-semibold text-gray-500">Subtotal: {formatCurrency(subtotalPreview)}</p>
                {discount > 0 && (
                  <p className="text-xs font-semibold text-gray-500">Desconto: −{formatCurrency(discount)}</p>
                )}
                {shippingCost > 0 && (
                  <p className="text-xs font-semibold text-gray-500">Frete: +{formatCurrency(shippingCost)}</p>
                )}
                <p className="text-sm font-bold text-gray-900">Total: {formatCurrency(totalPreview)}</p>
                {isOverCapacity && (
                  <p role="alert" className="text-xs font-bold text-danger-600">
                    O peso total excede a capacidade do veículo selecionado.
                  </p>
                )}
              </div>
            </>
          )}

          {activeTab === "pagamento" && (
            <>
              <div className="grid grid-cols-2 gap-3">
                <Controller
                  name="paymentMethodId"
                  control={control}
                  render={({ field }) => (
                    <SearchableSelect
                      label="Forma de pagamento"
                      placeholder="Selecione..."
                      options={paymentMethods.map((method) => ({ value: method.id, label: method.name }))}
                      value={field.value}
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      error={errors.paymentMethodId?.message}
                    />
                  )}
                />
                <Controller
                  name="paymentConditionId"
                  control={control}
                  render={({ field }) => (
                    <SearchableSelect
                      label="Condição de pagamento"
                      placeholder="Selecione..."
                      options={paymentConditions.map((condition) => ({ value: condition.id, label: condition.name }))}
                      value={field.value}
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      error={errors.paymentConditionId?.message}
                    />
                  )}
                />
              </div>

              <div className="flex flex-col gap-1 rounded-lg bg-gray-50 px-3.5 py-2.5">
                <p className="text-sm font-bold text-gray-900">Total do pedido: {formatCurrency(totalPreview)}</p>
                {!selectedPaymentCondition ? (
                  <p className="text-xs font-medium text-gray-400">
                    Selecione a condição de pagamento pra ver as parcelas.
                  </p>
                ) : totalPreview <= 0 ? (
                  <p className="text-xs font-medium text-gray-400">Adicione itens ao pedido pra calcular as parcelas.</p>
                ) : (
                  <div className="mt-1 flex flex-col gap-1">
                    {installments.map((installment) => (
                      <div key={installment.number} className="flex items-center justify-between text-xs">
                        <span className="font-semibold text-gray-700">
                          Parcela {installment.number}/{installments.length}
                          {installment.dueDate && ` · vence em ${formatDate(installment.dueDate)}`}
                        </span>
                        <span className="font-bold text-gray-900">{formatCurrency(installment.value)}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {isEditing && order.history.length > 0 && (
                <div className="flex flex-col gap-1.5">
                  <span className="text-sm font-medium text-gray-700">Histórico</span>
                  <div className="flex max-h-32 flex-col gap-1 overflow-y-auto rounded-lg border border-gray-100 px-3 py-2">
                    {[...order.history].reverse().map((entry, index) => (
                      <div
                        key={`${entry.status}-${entry.changedAt}-${index}`}
                        className="flex items-center justify-between text-xs"
                      >
                        <span className="font-semibold text-gray-700">
                          {ORDER_STATUSES.find((status) => status.value === entry.status)?.label ?? entry.status}
                        </span>
                        <span className="text-gray-400">{formatDateTime(entry.changedAt)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        <div className="flex justify-end gap-2.5 border-t border-gray-100 px-6 py-4">
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

function TabButton({
  label,
  active,
  hasError,
  onClick,
}: {
  label: string;
  active: boolean;
  hasError: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "relative flex items-center justify-center gap-1.5 pb-2.5 pt-3 text-sm font-semibold transition-colors",
        active ? "text-primary-700" : "text-gray-400 hover:text-gray-600"
      )}
    >
      {label}
      {hasError && <span className="h-1.5 w-1.5 rounded-full bg-danger-500" aria-hidden="true" />}
      {active && <span className="absolute inset-x-0 -bottom-px h-0.5 rounded-full bg-primary-600" />}
    </button>
  );
}
