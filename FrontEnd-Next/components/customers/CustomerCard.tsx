"use client";

import { Pencil, Trash2 } from "lucide-react";

import { cn } from "@/lib/utils";
import { formatDocument, formatPhone } from "@/lib/format";
import type { Customer } from "@/hooks/useCustomers";
import { CUSTOMER_TYPES, type CustomerType } from "@/lib/validations/customer";

const TYPE_BADGE_STYLES: Record<CustomerType, string> = {
  cliente: "bg-primary-50 text-primary-700",
  fornecedor: "bg-secondary-50 text-secondary-700",
  ambos: "bg-success-50 text-success-700",
};

interface CustomerCardProps {
  customer: Customer;
  onEdit: (customer: Customer) => void;
  onDelete: (customer: Customer) => void;
}

export function CustomerCard({ customer, onEdit, onDelete }: CustomerCardProps) {
  const typeLabel = CUSTOMER_TYPES.find((option) => option.value === customer.type)?.label;

  return (
    <div className="group relative flex flex-col gap-2.5 rounded-xl border border-gray-200 bg-white p-3.5 transition-shadow hover:shadow-md">
      <div className="absolute right-2.5 top-2.5 flex items-center gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
        <button
          type="button"
          onClick={() => onEdit(customer)}
          aria-label={`Editar cliente ${customer.name}`}
          className="rounded-md p-1 text-gray-300 hover:bg-gray-100 hover:text-gray-600"
        >
          <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
        <button
          type="button"
          onClick={() => onDelete(customer)}
          aria-label={`Excluir cliente ${customer.name}`}
          className="rounded-md p-1 text-gray-300 hover:bg-danger-50 hover:text-danger-600"
        >
          <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      </div>

      <button type="button" onClick={() => onEdit(customer)} className="flex flex-col gap-2.5 text-left">
        <div className="flex items-center justify-between pr-12">
          <p className="text-sm font-extrabold text-gray-900">{customer.name}</p>
          <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", TYPE_BADGE_STYLES[customer.type])}>
            {typeLabel}
          </span>
        </div>
        <div className="text-[11.5px] font-semibold text-gray-500">
          <p>{formatDocument(customer.document)}</p>
          <p>
            {customer.address.city} &middot; {customer.address.state}
          </p>
        </div>
        <div className="flex flex-col gap-0.5 border-t border-gray-100 pt-2 text-[11.5px] font-semibold text-gray-500">
          <span>{customer.email}</span>
          <span>{formatPhone(customer.phone)}</span>
        </div>
      </button>
    </div>
  );
}
