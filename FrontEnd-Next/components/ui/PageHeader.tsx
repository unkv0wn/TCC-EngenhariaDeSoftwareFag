import { ReactNode } from "react";

interface PageHeaderProps {
  title: string;
  subtitle: string;
  children?: ReactNode;
}

export function PageHeader({ title, subtitle, children }: PageHeaderProps) {
  return (
    <div className="mb-5 flex items-start justify-between">
      <div>
        <h1 className="text-[19px] font-extrabold text-gray-900">{title}</h1>
        <p className="mt-1 text-[13px] font-medium text-gray-500">{subtitle}</p>
      </div>
      {children && <div className="flex items-center gap-2.5">{children}</div>}
    </div>
  );
}
