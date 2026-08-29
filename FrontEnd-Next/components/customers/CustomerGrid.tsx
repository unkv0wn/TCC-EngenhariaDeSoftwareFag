import { CustomerCard } from "@/components/customers/CustomerCard";
import type { Customer } from "@/hooks/useCustomers";

interface CustomerGridProps {
  customers: Customer[];
  onEdit: (customer: Customer) => void;
  onDelete: (customer: Customer) => void;
}

export function CustomerGrid({ customers, onEdit, onDelete }: CustomerGridProps) {
  return (
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {customers.map((customer) => (
        <CustomerCard key={customer.id} customer={customer} onEdit={onEdit} onDelete={onDelete} />
      ))}
    </div>
  );
}
