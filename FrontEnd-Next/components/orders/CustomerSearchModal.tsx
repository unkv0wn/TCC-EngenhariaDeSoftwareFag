"use client";

import { useMemo, useState } from "react";
import { Search } from "lucide-react";

import { Modal } from "@/components/ui/Modal";
import type { Customer } from "@/hooks/useCustomers";
import { formatDocument, formatPhone } from "@/lib/format";
import { CUSTOMER_TYPES } from "@/lib/validations/customer";

interface CustomerSearchModalProps {
  customers: Customer[];
  selectedId: string;
  onSelect: (customerId: string) => void;
  onClose: () => void;
}

/**
 * Busca dedicada de cliente pro formulário de pedido — o SearchableSelect do campo Cliente
 * já filtra por nome, mas com muitos clientes cadastrados é útil poder achar pelo documento
 * ou pela cidade também, com mais contexto visível por linha do que um dropdown permite.
 */
export function CustomerSearchModal({ customers, selectedId, onSelect, onClose }: CustomerSearchModalProps) {
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    if (!query) return customers;
    return customers.filter((customer) =>
      [customer.name, customer.tradeName, customer.document, customer.address.city].some((field) =>
        field?.toLowerCase().includes(query)
      )
    );
  }, [customers, search]);

  return (
    <Modal title="Buscar cliente" onClose={onClose} size="lg">
      <div className="flex flex-col gap-3 px-6 py-5">
        <div className="relative">
          <Search
            className="pointer-events-none absolute inset-y-0 left-3 my-auto h-4 w-4 text-gray-400"
            aria-hidden="true"
          />
          <input
            type="search"
            autoFocus
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar por nome, documento ou cidade..."
            aria-label="Buscar cliente"
            className="w-full rounded-lg border border-gray-200 bg-white py-2.5 pl-9 pr-3.5 text-sm text-gray-900 placeholder:text-gray-400 transition-all duration-150 ease-out hover:border-gray-300 focus:border-primary-500 focus:outline-none focus:ring-2 focus:ring-primary-500/15"
          />
        </div>

        <div className="flex max-h-96 flex-col overflow-y-auto rounded-lg border border-gray-100">
          {filtered.length === 0 ? (
            <p className="px-3.5 py-6 text-center text-sm font-medium text-gray-400">
              {search ? `Nenhum cliente encontrado para "${search}".` : "Nenhum cliente cadastrado."}
            </p>
          ) : (
            filtered.map((customer) => (
              <button
                key={customer.id}
                type="button"
                onClick={() => {
                  onSelect(customer.id);
                  onClose();
                }}
                className={`flex flex-col gap-0.5 border-b border-gray-100 px-3.5 py-2.5 text-left last:border-0 hover:bg-gray-50 ${
                  customer.id === selectedId ? "bg-primary-50" : ""
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-sm font-bold text-gray-900">{customer.name}</span>
                  <span className="shrink-0 text-[11px] font-bold uppercase tracking-wide text-primary-700">
                    {CUSTOMER_TYPES.find((option) => option.value === customer.type)?.label}
                  </span>
                </div>
                <span className="text-xs font-semibold text-gray-500">
                  {formatDocument(customer.document)} · {customer.address.city}/{customer.address.state} ·{" "}
                  {formatPhone(customer.phone)}
                </span>
              </button>
            ))
          )}
        </div>
      </div>
    </Modal>
  );
}
