import { Pencil, Trash2 } from "lucide-react";

import type { PaymentCondition } from "@/hooks/usePaymentConditions";

const TABLE_HEADINGS = ["Nome", "Parcelas", "Intervalo", ""];

interface PaymentConditionTableProps {
  paymentConditions: PaymentCondition[];
  onEdit: (paymentCondition: PaymentCondition) => void;
  onDelete: (paymentCondition: PaymentCondition) => void;
}

export function PaymentConditionTable({ paymentConditions, onEdit, onDelete }: PaymentConditionTableProps) {
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
          {paymentConditions.map((condition) => (
            <tr key={condition.id} className="border-b border-gray-100 last:border-0">
              <td className="px-3.5 py-3 font-bold text-gray-900">{condition.name}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">
                {condition.installments}x
              </td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">
                {condition.intervalDays === 0 ? "—" : `${condition.intervalDays} dias`}
              </td>
              <td className="px-3.5 py-3">
                <div className="flex items-center justify-end gap-1">
                  <button
                    type="button"
                    onClick={() => onEdit(condition)}
                    aria-label={`Editar condição de pagamento ${condition.name}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                  >
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(condition)}
                    aria-label={`Excluir condição de pagamento ${condition.name}`}
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
