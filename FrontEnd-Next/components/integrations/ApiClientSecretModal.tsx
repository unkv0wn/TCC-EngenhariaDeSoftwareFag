"use client";

import { useState } from "react";
import { Check, Copy, TriangleAlert } from "lucide-react";

import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import type { ApiClientCreated } from "@/hooks/useApiClients";

interface ApiClientSecretModalProps {
  created: ApiClientCreated;
  onClose: () => void;
}

/**
 * Mostra a credencial recém-criada — a única vez que o segredo existe fora do hash.
 *
 * O servidor guarda só o BCrypt; este valor não está em lugar nenhum depois que a tela
 * fecha, nem no banco, nem em log. Por isso o aviso é firme e o fechar é explícito: se o
 * gestor fechar sem copiar, a saída é gerar outro, não recuperar este.
 */
export function ApiClientSecretModal({ created, onClose }: ApiClientSecretModalProps) {
  const [copied, setCopied] = useState<"id" | "secret" | null>(null);

  async function copy(value: string, which: "id" | "secret") {
    await navigator.clipboard.writeText(value);
    setCopied(which);
    window.setTimeout(() => setCopied(null), 2000);
  }

  return (
    <Modal title="Credencial da integração" onClose={onClose} size="lg">
      <div className="flex flex-col gap-4 px-6 py-5">
        <div className="flex items-start gap-2.5 rounded-xl border border-warning-200 bg-warning-50 px-4 py-3">
          <TriangleAlert className="mt-0.5 h-4 w-4 shrink-0 text-warning-600" aria-hidden="true" />
          <p className="text-[12.5px] font-semibold leading-relaxed text-warning-700">
            Esta é a única vez que o segredo aparece. O servidor guarda apenas um hash dele — se
            esta janela fechar sem você copiar, não há como recuperá-lo, só gerar outro.
          </p>
        </div>

        <Field label="Client ID" value={created.client.clientId} onCopy={() => copy(created.client.clientId, "id")} copied={copied === "id"} />
        <Field label="Client Secret" value={created.clientSecret} onCopy={() => copy(created.clientSecret, "secret")} copied={copied === "secret"} />

        <div className="rounded-xl border border-gray-200 bg-gray-50 px-4 py-3">
          <p className="mb-1.5 text-[12.5px] font-extrabold text-gray-700">Como usar</p>
          <p className="text-[12px] font-semibold leading-relaxed text-gray-500">
            Troque a credencial por um token em <code className="font-mono">POST /api/integration/token</code> e
            envie-o como <code className="font-mono">Authorization: Bearer &lt;token&gt;</code> nas consultas. O
            token vale por 1 hora; a credencial não expira, mas pode ser revogada aqui.
          </p>
        </div>

        <div className="mt-2 flex justify-end border-t border-gray-100 pt-4">
          <Button type="button" className="w-auto" onClick={onClose}>
            Já copiei, fechar
          </Button>
        </div>
      </div>
    </Modal>
  );
}

function Field({
  label,
  value,
  onCopy,
  copied,
}: {
  label: string;
  value: string;
  onCopy: () => void;
  copied: boolean;
}) {
  return (
    <div>
      <p className="mb-1 text-[12.5px] font-bold text-gray-700">{label}</p>
      <div className="flex items-center gap-2">
        <code className="min-w-0 flex-1 truncate rounded-lg border border-gray-200 bg-white px-3.5 py-2.5 font-mono text-[12.5px] text-gray-900">
          {value}
        </code>
        <button
          type="button"
          onClick={onCopy}
          aria-label={`Copiar ${label}`}
          className="shrink-0 rounded-lg border border-gray-200 bg-white p-2.5 text-gray-400 hover:bg-gray-50 hover:text-gray-600"
        >
          {copied ? (
            <Check className="h-4 w-4 text-success-600" aria-hidden="true" />
          ) : (
            <Copy className="h-4 w-4" aria-hidden="true" />
          )}
        </button>
      </div>
    </div>
  );
}
