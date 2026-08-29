import type { Metadata } from "next";

import { CustomersPageContent } from "@/components/customers/CustomersPageContent";

export const metadata: Metadata = {
  title: "Clientes",
  description: "Gerencie seus clientes e fornecedores.",
};

export default function ClientesPage() {
  return <CustomersPageContent />;
}
