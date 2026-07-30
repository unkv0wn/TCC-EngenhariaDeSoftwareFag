import { InputHTMLAttributes, forwardRef, useId } from "react";
import { AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  startIcon?: React.ReactNode;
  endAdornment?: React.ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, startIcon, endAdornment, id, ...props }, ref) => {
    const generatedId = useId();
    const inputId = id ?? generatedId;
    const errorId = `${inputId}-error`;

    return (
      <div className="flex flex-col gap-1.5">
        <label
          htmlFor={inputId}
          className="text-sm font-medium text-gray-700"
        >
          {label}
        </label>
        <div className="relative">
          {startIcon && (
            <span className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3 text-gray-400">
              {startIcon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            aria-invalid={!!error}
            aria-describedby={error ? errorId : undefined}
            className={cn(
              "w-full rounded-lg border bg-white px-3.5 py-2.5 text-sm text-gray-900",
              "placeholder:text-gray-400",
              "transition-all duration-150 ease-out",
              "focus:outline-none focus:ring-2 focus:ring-primary-500/15 focus:border-primary-500",
              "disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-400",
              startIcon && "pl-10",
              endAdornment && "pr-10",
              error
                ? "border-danger-300 focus:border-danger-500 focus:ring-danger-500/10"
                : "border-gray-200 hover:border-gray-300",
              className
            )}
            {...props}
          />
          {endAdornment && (
            <span className="absolute inset-y-0 right-0 flex items-center pr-3">
              {endAdornment}
            </span>
          )}
        </div>
        {error && (
          <p
            id={errorId}
            role="alert"
            className="flex items-center gap-1 text-xs font-medium text-red-600"
          >
            <AlertCircle className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
            {error}
          </p>
        )}
      </div>
    );
  }
);

Input.displayName = "Input";
