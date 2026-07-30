import type { Metadata } from "next";
import { Plus } from "lucide-react";

import { Sidebar } from "@/components/dashboard/Sidebar";
import { KpiRow } from "@/components/dashboard/KpiRow";

export const metadata: Metadata = {
  title: "Dashboard",
  description: "Visão geral das suas rotas.",
};

export default function DashboardPage() {
  return (
    <div className="flex flex-1">
      <Sidebar />

      <main className="flex-1 bg-gray-50 px-8 py-7">
        <div className="mb-5 flex items-start justify-between">
          <div>
            <h1 className="text-[19px] font-extrabold text-gray-900">Dashboard</h1>
            <p className="mt-1 text-[13px] font-medium text-gray-500">
              Visão geral das suas rotas
            </p>
          </div>
          <button
            type="button"
            className="flex items-center gap-1.5 rounded-lg bg-primary-600 px-4 py-2.5 text-[13.5px] font-bold text-white hover:bg-primary-700"
          >
            <Plus className="h-[15px] w-[15px]" aria-hidden="true" />
            Nova rota
          </button>
        </div>

        <KpiRow />

        <div className="mt-4 rounded-xl border border-dashed border-gray-300 px-6 py-8 text-center text-[12.5px] font-semibold text-gray-400">
          (espaço em aberto — definimos depois o que vem aqui)
        </div>
      </main>
    </div>
  );
}
