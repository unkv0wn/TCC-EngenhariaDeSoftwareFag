import { Pencil, Smartphone, TriangleAlert, Trash2 } from "lucide-react";

import { cn } from "@/lib/utils";
import type { MobileUser } from "@/hooks/useMobileUsers";
import { formatDateTime } from "@/lib/format";
import type { MobileUserStatus } from "@/lib/validations/mobileUser";

const TABLE_HEADINGS = ["Motorista", "Usuário", "Aparelho autorizado", "Último acesso", "Status", ""];

const STATUS_BADGE_STYLES: Record<MobileUserStatus, string> = {
  ativo: "bg-success-50 text-success-700",
  bloqueado: "bg-danger-50 text-danger-600",
};

const STATUS_LABELS: Record<MobileUserStatus, string> = {
  ativo: "Ativo",
  bloqueado: "Bloqueado",
};

/** Em grupos de quatro, igual ao que o app mostra — é o que permite conferir de bate-pronto. */
function formatDeviceId(deviceId: string): string {
  return deviceId.replace(/(.{4})/g, "$1 ").trim();
}

interface MobileUserTableProps {
  mobileUsers: MobileUser[];
  onEdit: (user: MobileUser) => void;
  onDelete: (user: MobileUser) => void;
}

export function MobileUserTable({ mobileUsers, onEdit, onDelete }: MobileUserTableProps) {
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
          {mobileUsers.map((user) => (
            <tr key={user.id} className="border-b border-gray-100 last:border-0">
              <td className="px-3.5 py-3 font-bold text-gray-900">{user.driverName ?? "—"}</td>
              <td className="px-3.5 py-3 font-semibold text-gray-500">{user.username}</td>

              {/*
                Acesso sem aparelho cadastrado não entra em celular nenhum. É o estado
                normal logo depois de criar o usuário — mas também é o estado em que o
                motorista liga dizendo que o login não funciona, então fica em destaque
                com a instrução, não como um traço.
              */}
              <td className="px-3.5 py-3">
                {user.deviceId ? (
                  <div className="flex items-start gap-2">
                    <Smartphone className="mt-0.5 h-3.5 w-3.5 shrink-0 text-gray-400" aria-hidden="true" />
                    <div className="min-w-0">
                      {user.deviceLabel && (
                        <p className="truncate font-bold text-gray-700">{user.deviceLabel}</p>
                      )}
                      <p className="font-mono text-[11.5px] font-semibold text-gray-500">
                        {formatDeviceId(user.deviceId)}
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="flex items-start gap-2">
                    <TriangleAlert className="mt-0.5 h-3.5 w-3.5 shrink-0 text-warning-500" aria-hidden="true" />
                    <div>
                      <p className="font-bold text-warning-800">Nenhum aparelho</p>
                      <p className="text-[11.5px] font-semibold text-gray-500">
                        Peça ao motorista o código da tela de login
                      </p>
                    </div>
                  </div>
                )}
              </td>

              <td className="px-3.5 py-3 font-semibold text-gray-500">
                {user.lastLoginAt ? formatDateTime(user.lastLoginAt) : "Nunca entrou"}
              </td>

              <td className="px-3.5 py-3">
                <span
                  className={cn("rounded-full px-2 py-0.5 text-[10px] font-bold", STATUS_BADGE_STYLES[user.status])}
                >
                  {STATUS_LABELS[user.status]}
                </span>
              </td>

              <td className="px-3.5 py-3">
                <div className="flex items-center justify-end gap-1">
                  <button
                    type="button"
                    onClick={() => onEdit(user)}
                    aria-label={`Editar acesso de ${user.driverName ?? user.username}`}
                    className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
                  >
                    <Pencil className="h-3.5 w-3.5" aria-hidden="true" />
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(user)}
                    aria-label={`Excluir acesso de ${user.driverName ?? user.username}`}
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
