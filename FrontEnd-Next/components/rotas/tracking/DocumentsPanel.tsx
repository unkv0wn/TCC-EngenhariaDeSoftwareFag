"use client";

import { useRef, useState } from "react";
import { FileText, Paperclip, Trash2 } from "lucide-react";

import { EmptyState } from "@/components/ui/EmptyState";

/**
 * MOCK — nada aqui sobe arquivo pra lugar nenhum.
 *
 * O upload de verdade exige decidir onde armazenar (disco, S3, coluna `bytea`),
 * limite de tamanho e política de retenção — nada disso está definido. Esta aba
 * existe pra mostrar o formato final da lista de anexos; "Anexar" só acrescenta o
 * nome do arquivo escolhido ao estado em memória, e tudo some ao recarregar.
 *
 * Mesmo espírito de `lib/routePrototype.ts`: dado falso, declarado como falso.
 */

interface MockDocument {
  id: string;
  name: string;
  kind: string;
  sizeLabel: string;
  addedAt: string;
}

const SEED_DOCUMENTS: MockDocument[] = [
  { id: "1", name: "canhoto-assinado-parada-1.pdf", kind: "Canhoto", sizeLabel: "184 KB", addedAt: "21/09/2026 20:41" },
  { id: "2", name: "nota-fiscal-12045.pdf", kind: "Nota fiscal", sizeLabel: "96 KB", addedAt: "21/09/2026 20:12" },
  { id: "3", name: "foto-entrega-parada-2.jpg", kind: "Foto da entrega", sizeLabel: "1,2 MB", addedAt: "21/09/2026 21:03" },
];

export function DocumentsPanel() {
  const [documents, setDocuments] = useState<MockDocument[]>(SEED_DOCUMENTS);
  const inputRef = useRef<HTMLInputElement>(null);

  function handleFiles(files: FileList | null) {
    if (!files || files.length === 0) return;
    const added: MockDocument[] = [...files].map((file, index) => ({
      id: `${Date.now()}-${index}`,
      name: file.name,
      kind: "Anexo",
      sizeLabel: `${Math.max(1, Math.round(file.size / 1024))} KB`,
      addedAt: new Date().toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" }),
    }));
    setDocuments((previous) => [...added, ...previous]);
  }

  return (
    <div className="flex flex-col gap-2.5">
      <div className="flex items-center justify-between">
        <p className="text-[13px] font-extrabold text-gray-900">Documentos</p>
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-3 py-1.5 text-[12px] font-bold text-white hover:bg-primary-700"
        >
          <Paperclip className="h-3.5 w-3.5" aria-hidden="true" />
          Anexar
        </button>
        <input
          ref={inputRef}
          type="file"
          multiple
          className="hidden"
          aria-hidden="true"
          tabIndex={-1}
          onChange={(event) => {
            handleFiles(event.target.files);
            event.target.value = "";
          }}
        />
      </div>

      <p className="rounded-lg border border-warning-200 bg-warning-50 px-3 py-2 text-[11px] font-semibold text-warning-700">
        Demonstração — os arquivos não são enviados nem armazenados, e a lista some ao recarregar.
      </p>

      {documents.length === 0 ? (
        <EmptyState message="Nenhum documento anexado a esta rota." />
      ) : (
        <ul className="flex flex-col gap-1.5">
          {documents.map((document) => (
            <li
              key={document.id}
              className="flex items-center gap-2.5 rounded-lg border border-gray-100 bg-white px-3 py-2.5"
            >
              <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-gray-100 text-gray-500">
                <FileText className="h-3.5 w-3.5" aria-hidden="true" />
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate text-[12.5px] font-bold text-gray-900" title={document.name}>
                  {document.name}
                </p>
                <p className="text-[11px] font-semibold text-gray-400">
                  {document.kind} · {document.sizeLabel} · {document.addedAt}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setDocuments((previous) => previous.filter((item) => item.id !== document.id))}
                title="Remover documento"
                aria-label="Remover documento"
                className="shrink-0 rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
              >
                <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
