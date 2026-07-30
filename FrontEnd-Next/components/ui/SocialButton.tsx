import { ButtonHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

type SocialProvider = "google" | "microsoft";

interface SocialButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  provider: SocialProvider;
}

const providerConfig: Record<SocialProvider, { label: string; icon: React.ReactNode }> = {
  google: {
    label: "Google",
    icon: (
      <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true">
        <path
          fill="#4285F4"
          d="M23.49 12.27c0-.79-.07-1.54-.2-2.27H12v4.51h6.47a5.53 5.53 0 0 1-2.4 3.63v3.02h3.88c2.27-2.09 3.54-5.17 3.54-8.89Z"
        />
        <path
          fill="#34A853"
          d="M12 24c3.24 0 5.95-1.07 7.93-2.9l-3.87-3.02c-1.08.72-2.46 1.14-4.06 1.14-3.12 0-5.77-2.1-6.72-4.93H1.29v3.11A12 12 0 0 0 12 24Z"
        />
        <path
          fill="#FBBC05"
          d="M5.28 14.29a7.2 7.2 0 0 1 0-4.58V6.6H1.29a12 12 0 0 0 0 10.8l3.99-3.11Z"
        />
        <path
          fill="#EA4335"
          d="M12 4.75c1.76 0 3.34.61 4.59 1.8l3.44-3.44C17.94 1.19 15.24 0 12 0A12 12 0 0 0 1.29 6.6l3.99 3.11C6.23 6.86 8.88 4.75 12 4.75Z"
        />
      </svg>
    ),
  },
  microsoft: {
    label: "Microsoft",
    icon: (
      <svg viewBox="0 0 24 24" className="h-4 w-4" aria-hidden="true">
        <path fill="#F25022" d="M2 2h9.5v9.5H2z" />
        <path fill="#7FBA00" d="M12.5 2H22v9.5h-9.5z" />
        <path fill="#00A4EF" d="M2 12.5h9.5V22H2z" />
        <path fill="#FFB900" d="M12.5 12.5H22V22h-9.5z" />
      </svg>
    ),
  },
};

export function SocialButton({ provider, className, ...props }: SocialButtonProps) {
  const { label, icon } = providerConfig[provider];

  return (
    <button
      type="button"
      className={cn(
        "inline-flex w-full items-center justify-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2.5 text-sm font-medium text-gray-700",
        "transition-all duration-150 ease-out hover:border-gray-300 hover:bg-gray-50",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:ring-offset-2",
        "disabled:cursor-not-allowed disabled:opacity-60",
        className
      )}
      {...props}
    >
      {icon}
      {label}
    </button>
  );
}
