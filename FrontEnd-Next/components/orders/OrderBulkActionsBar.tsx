import { Ban, CheckCircle2, Receipt, ReceiptText, Trash2, Truck, X } from "lucide-react";

interface OrderBulkActionsBarProps {
  count: number;
  onMarkEmRota: () => void;
  onMarkEntregue: () => void;
  onCancel: () => void;
  onInvoice: () => void;
  onUninvoice: () => void;
  onDelete: () => void;
  onClear: () => void;
}

export function OrderBulkActionsBar({
  count,
  onMarkEmRota,
  onMarkEntregue,
  onCancel,
  onInvoice,
  onUninvoice,
  onDelete,
  onClear,
}: OrderBulkActionsBarProps) {
  return (
    <div className="mb-4 flex flex-wrap items-center gap-2.5 rounded-lg border border-primary-200 bg-primary-50 px-3.5 py-2.5">
      <span className="text-xs font-bold text-primary-700">{count} selecionado{count === 1 ? "" : "s"}</span>

      <div className="ml-auto flex flex-wrap items-center gap-1.5">
        <BulkButton label="Em rota" icon={Truck} onClick={onMarkEmRota} />
        <BulkButton label="Entregue" icon={CheckCircle2} onClick={onMarkEntregue} />
        <BulkButton label="Cancelar" icon={Ban} onClick={onCancel} variant="danger" />
        <BulkButton label="Faturar" icon={Receipt} onClick={onInvoice} />
        <BulkButton label="Desfaturar" icon={ReceiptText} onClick={onUninvoice} />
        <BulkButton label="Excluir" icon={Trash2} onClick={onDelete} variant="danger" />
        <button
          type="button"
          onClick={onClear}
          aria-label="Limpar seleção"
          className="rounded-md p-1.5 text-primary-400 hover:bg-primary-100 hover:text-primary-700"
        >
          <X className="h-3.5 w-3.5" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}

function BulkButton({
  label,
  icon: Icon,
  onClick,
  variant = "default",
}: {
  label: string;
  icon: typeof Truck;
  onClick: () => void;
  variant?: "default" | "danger";
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex items-center gap-1 rounded-md border px-2.5 py-1.5 text-xs font-bold ${
        variant === "danger"
          ? "border-danger-200 bg-white text-danger-600 hover:bg-danger-50"
          : "border-primary-200 bg-white text-primary-700 hover:bg-primary-100"
      }`}
    >
      <Icon className="h-3.5 w-3.5" aria-hidden="true" />
      {label}
    </button>
  );
}
