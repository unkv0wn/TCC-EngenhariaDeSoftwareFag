import type { Metadata } from "next";

import { ProductsPageContent } from "@/components/products/ProductsPageContent";

export const metadata: Metadata = {
  title: "Produtos",
  description: "Gerencie os produtos do seu catálogo.",
};

export default function ProdutosPage() {
  return <ProductsPageContent />;
}
