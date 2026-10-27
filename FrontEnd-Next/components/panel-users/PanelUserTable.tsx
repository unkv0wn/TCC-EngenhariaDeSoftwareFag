import { Pencil, Trash2 } from "lucide-react";

import { cn } from "@/lib/utils";
import type { PanelUser } from "@/hooks/usePanelUsers";
import { formatDateTime } from "@/lib/format";
import type { PanelUserStatus } from "@/lib/validations/panelUser";

const TABLE_HEADINGS = ["Nome", "E-mail", "Último acesso", "Status", ""];

const STATUS_BADGE_STYLES: Record<PanelUserStatus, string> = {
  ativo: "bg-success-50 text-success-700",
  bloqueado: "bg-danger-50 text-danger-600",
};

const STATUS_LABELS: Record<PanelUserStatus, string> = {
  ativo: "Ativo",
  bloqueado: "Bloqueado",
};

interface PanelUserTableProps {
  panelUsers: PanelUser[];
  currentUserId: string | null;
  onEdit: (user: PanelUser) => void;
  onDelete: (user: PanelUser) => void;
}

export function PanelUserTable({
  panelUsers,
  currentUserId,
  onEdit,
  onDelete,
}: PanelUserTableProps) {
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
          {panelUsers.map((user) => {
            const isSelf = user.id === currentUserId;

            return (
              <tr key={user.id} className="border-b border-gray-100 last:border-0">
                <td className="px-3.5 py-3">
                  <span className="font-bold text-gray-900">{user.name}</span>
                  {isSelf && (
                    <span className="ml-2 rounded-full bg-primary-50 px-2 py-0.5 text-[10px] font-bold text-primary-700">
                      você
                    </span>
                  )}
                </td>
                <td className="px-3.5 py-3 font-semibold text-gray-500">{user.email}</td>
                <td className="whitespace-nowrap px-3.5 py-3 font-semibold text-gray-500">
                  {user.lastLoginAt ? formatDateTime(user.lastLoginAt) : "Nunca entrou"}
                </td>
                <td className="px-3.5 py-3">
                  <span
                    className={cn(
                      "rounded-full px-2 py-0.5 text-[10px] font-bold",
                      STATUS_BADGE_STYLES[user.status]
                    )}
                  >
                    {STATUS_LABELS[user.status]}
                  </span>
                </td>
                <td className="px-3.5 py-3">
                  <div className="flex items-center justify-end gap-1">
                    <button
                      type="button"
                      onClick={() => onEdit(user)}
                      aria-label={`Editar acesso de ${user.name}`}
                      className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                    >
                      <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                    </button>

                    {/*
                      O próprio acesso não oferece excluir: o servidor recusa de qualquer
                      forma, e um botão que só serve para dar erro é pior do que botão
                      nenhum.
                    */}
                    {!isSelf && (
                      <button
                        type="button"
                        onClick={() => onDelete(user)}
                        aria-label={`Excluir acesso de ${user.name}`}
                        className="rounded-md p-1.5 text-gray-400 hover:bg-danger-50 hover:text-danger-600"
                      >
                        <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
