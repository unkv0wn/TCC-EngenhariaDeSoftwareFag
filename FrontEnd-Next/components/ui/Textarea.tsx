import { TextareaHTMLAttributes, forwardRef, useId } from "react";
import { AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  error?: string;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, label, error, id, ...props }, ref) => {
    const generatedId = useId();
    const textareaId = id ?? generatedId;
    const errorId = `${textareaId}-error`;

    return (
      <div className="flex flex-col gap-1.5">
        <label htmlFor={textareaId} className="text-sm font-medium text-gray-700">
          {label}
        </label>
        <textarea
          ref={ref}
          id={textareaId}
          aria-invalid={!!error}
          aria-describedby={error ? errorId : undefined}
          className={cn(
            "min-h-[80px] w-full resize-y rounded-lg border bg-white px-3.5 py-2.5 text-sm text-gray-900",
            "placeholder:text-gray-400",
            "transition-all duration-150 ease-out",
            "focus:outline-none focus:ring-2 focus:ring-primary-500/15 focus:border-primary-500",
            "disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-400",
            error
              ? "border-danger-300 focus:border-danger-500 focus:ring-danger-500/10"
              : "border-gray-200 hover:border-gray-300",
            className
          )}
          {...props}
        />
        {error && (
          <p id={errorId} role="alert" className="flex items-center gap-1 text-xs font-medium text-red-600">
            <AlertCircle className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
            {error}
          </p>
        )}
      </div>
    );
  }
);

Textarea.displayName = "Textarea";
