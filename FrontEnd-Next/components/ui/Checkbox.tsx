import { InputHTMLAttributes, forwardRef, useId } from "react";
import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

interface CheckboxProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
}

export const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(
  ({ className, label, id, ...props }, ref) => {
    const generatedId = useId();
    const checkboxId = id ?? generatedId;

    return (
      <label
        htmlFor={checkboxId}
        className="group inline-flex cursor-pointer select-none items-center gap-2"
      >
        <span className="relative flex h-4 w-4 shrink-0 items-center justify-center">
          <input
            ref={ref}
            id={checkboxId}
            type="checkbox"
            className={cn(
              "peer h-4 w-4 shrink-0 cursor-pointer appearance-none rounded border border-gray-300 bg-white",
              "transition-colors duration-150 ease-out",
              "checked:border-primary-600 checked:bg-primary-600",
              "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/30 focus-visible:ring-offset-1",
              className
            )}
            {...props}
          />
          <Check
            aria-hidden="true"
            className="pointer-events-none absolute h-3 w-3 scale-0 text-white transition-transform duration-150 ease-out peer-checked:scale-100"
          />
        </span>
        <span className="text-sm text-gray-600 transition-colors group-hover:text-gray-900">
          {label}
        </span>
      </label>
    );
  }
);

Checkbox.displayName = "Checkbox";
