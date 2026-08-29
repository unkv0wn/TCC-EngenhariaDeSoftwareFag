"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import { Car, ChevronDown, LayoutDashboard, LogOut, Settings } from "lucide-react";
import { cn } from "@/lib/utils";

const NAV_GROUPS = [
  {
    label: "Geral",
    items: [{ href: "/dashboard", label: "Dashboard", icon: LayoutDashboard }],
  },
  {
    label: "Cadastros",
    items: [{ href: "/dashboard/veiculos", label: "Veículos", icon: Car }],
  },
] as const;

function NavItem({
  icon: Icon,
  label,
  href,
  active,
  variant = "default",
  onClick,
}: {
  icon: typeof LayoutDashboard;
  label: string;
  href?: string;
  active?: boolean;
  variant?: "default" | "danger";
  onClick?: () => void;
}) {
  const className = cn(
    "flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-semibold transition-colors",
    active
      ? "bg-primary-50 text-primary-700"
      : variant === "danger"
        ? "text-gray-600 hover:bg-danger-50 hover:text-danger-600"
        : "text-gray-600 hover:bg-gray-50"
  );

  const content = (
    <>
      <Icon className="h-[17px] w-[17px] shrink-0" aria-hidden="true" />
      {label}
    </>
  );

  if (href) {
    return (
      <Link href={href} className={className}>
        {content}
      </Link>
    );
  }

  return (
    <button type="button" onClick={onClick} className={cn(className, "w-full text-left")}>
      {content}
    </button>
  );
}

function NavGroup({ label, items, pathname }: (typeof NAV_GROUPS)[number] & { pathname: string }) {
  const [expanded, setExpanded] = useState(true);

  return (
    <div className="mb-3.5 flex flex-col gap-0.5">
      <button
        type="button"
        onClick={() => setExpanded((v) => !v)}
        aria-expanded={expanded}
        className="flex items-center justify-between px-2.5 py-1 text-left"
      >
        <span className="text-[10.5px] font-extrabold uppercase tracking-wider text-gray-400">
          {label}
        </span>
        <ChevronDown
          className={cn(
            "h-3.5 w-3.5 text-gray-400 transition-transform",
            !expanded && "-rotate-90"
          )}
          aria-hidden="true"
        />
      </button>
      {expanded &&
        items.map((item) => (
          <NavItem key={item.label} {...item} active={"href" in item && item.href === pathname} />
        ))}
    </div>
  );
}

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();

  async function handleLogout() {
    await fetch("/api/logout", { method: "POST" });
    router.push("/login");
  }

  return (
    <aside className="flex w-64 shrink-0 flex-col border-r border-gray-100 bg-white px-[18px] py-[22px]">
      <div className="mb-6 flex items-center gap-2 px-1">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary-600 text-sm font-extrabold text-white">
          R
        </div>
        <span className="text-[15px] font-extrabold text-gray-900">RouteWise</span>
      </div>

      {NAV_GROUPS.map((group) => (
        <NavGroup key={group.label} {...group} pathname={pathname} />
      ))}

      <div className="mt-auto pt-3">
        <div className="mb-2 border-t border-gray-100" />
        <NavItem icon={Settings} label="Configurações" />
        <NavItem icon={LogOut} label="Sair" variant="danger" onClick={handleLogout} />
      </div>
    </aside>
  );
}
