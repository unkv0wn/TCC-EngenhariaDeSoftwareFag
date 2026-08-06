import { ProductCard } from "@/components/products/ProductCard";
import type { Product } from "@/hooks/useProducts";
import type { Unit } from "@/hooks/useUnits";

interface ProductGridProps {
  products: Product[];
  units: Unit[];
  onEdit: (product: Product) => void;
  onDelete: (product: Product) => void;
}

export function ProductGrid({ products, units, onEdit, onDelete }: ProductGridProps) {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
      {products.map((product) => (
        <ProductCard key={product.id} product={product} units={units} onEdit={onEdit} onDelete={onDelete} />
      ))}
    </div>
  );
}
