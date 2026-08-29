import { Pencil, Trash2 } from "lucide-react";

import { formatDocument, formatPhone } from "@/lib/format";
import { CUSTOMER_TYPES } from "@/lib/validations/customer";
import type { Customer } from "@/hooks/useCustomers";

const TABLE_HEADINGS = ["Nome", "Tipo", "Documento", "Cidade/UF", "Telefone", ""];

interface CustomerTableProps {
  customers: Customer[];
  onEdit: (customer: Customer) => void;
  onDelete: (customer: Customer) => void;
}

export function CustomerTable({ customers, onEdit, onDelete }: CustomerTableProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-gray-200 bg-white">
      <table className="w-full text-left text-sm">
        <thead>
          <tr className="bg-gray-50">
            {TABLE_HEADINGS.map((heading) => (
              <th
                key={heading}
                className="border-b border-gray-200 px-3.5 py-2.5 text-[11px] font-extrabold uppercase tracking-wider text-gray-400"
              >
                {heading}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {customers.map((customer) => (
            <tr key={customer.id} className="border-b border-gray-100 last:border-0">
              <td className="px-3.5 py-3 font-bold text-gray-900">{customer.name}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">
                {CUSTOMER_TYPES.find((option) => option.value === customer.type)?.label}
              </td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{formatDocument(customer.document)}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">
                {customer.address.city}/{customer.address.state}
              </td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{formatPhone(customer.phone)}</td>
              <td className="px-3.5 py-3">
                <div className="flex items-center justify-end gap-1">
                  <button
                    type="button"
                    onClick={() => onEdit(customer)}
                    aria-label={`Editar cliente ${customer.name}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                  >
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(customer)}
                    aria-label={`Excluir cliente ${customer.name}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-danger-50 hover:text-danger-600"
                  >
                    <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
