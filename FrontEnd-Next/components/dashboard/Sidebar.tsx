"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, usePathname } from "next/navigation";
import {
  Car,
  CalendarClock,
  ChevronDown,
  ClipboardList,
  Fuel,
  IdCard,
  LayoutDashboard,
  LogOut,
  Package,
  Route,
  Ruler,
  Settings,
  Users,
  Wallet,
} from "lucide-react";
import { cn } from "@/lib/utils";

interface NavLeafItem {
  href: string;
  label: string;
  icon: typeof LayoutDashboard;
}

interface NavSubgroup {
  label: string;
  items: readonly NavLeafItem[];
}

interface NavGroupData {
  label: string;
  items: readonly NavLeafItem[];
  subgroups?: readonly NavSubgroup[];
}

const NAV_GROUPS: readonly NavGroupData[] = [
  {
    label: "Geral",
    items: [{ href: "/dashboard", label: "Dashboard", icon: LayoutDashboard }],
  },
  {
    label: "Cadastros",
    items: [],
    subgroups: [
      {
        label: "Frota",
        items: [
          { href: "/dashboard/veiculos", label: "Veículos", icon: Car },
          { href: "/dashboard/motoristas", label: "Motoristas", icon: IdCard },
          { href: "/dashboard/abastecimentos", label: "Abastecimentos", icon: Fuel },
        ],
      },
      {
        label: "Catálogo",
        items: [
          { href: "/dashboard/unidades", label: "Unidades de Medida", icon: Ruler },
          { href: "/dashboard/produtos", label: "Produtos", icon: Package },
        ],
      },
      {
        label: "Comercial",
        items: [{ href: "/dashboard/clientes", label: "Clientes", icon: Users }],
      },
      {
        label: "Financeiro",
        items: [
          { href: "/dashboard/formas-pagamento", label: "Formas de Pagamento", icon: Wallet },
          { href: "/dashboard/condicoes-pagamento", label: "Condições", icon: CalendarClock },
        ],
      },
    ],
  },
  {
    label: "Operação",
    items: [
      { href: "/dashboard/pedidos", label: "Pedidos", icon: ClipboardList },
      { href: "/dashboard/rotas", label: "Rotas", icon: Route },
    ],
  },
];

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

function isNavItemActive(href: string, pathname: string): boolean {
  if (href === "/dashboard") return pathname === href;
  return pathname === href || pathname.startsWith(`${href}/`);
}

function NavGroup({ label, items, subgroups, pathname }: NavGroupData & { pathname: string }) {
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
      {expanded && (
        <>
          {items.map((item) => (
            <NavItem key={item.label} {...item} active={isNavItemActive(item.href, pathname)} />
          ))}
          {subgroups?.map((subgroup) => (
            <div key={subgroup.label} className="mt-1 flex flex-col gap-0.5 border-l border-gray-100 pl-2.5">
              <span className="px-2 py-1 text-[9.5px] font-extrabold uppercase tracking-wider text-gray-300">
                {subgroup.label}
              </span>
              {subgroup.items.map((item) => (
                <NavItem key={item.label} {...item} active={isNavItemActive(item.href, pathname)} />
              ))}
            </div>
          ))}
        </>
      )}
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
        <NavItem
          icon={Settings}
          label="Configurações"
          href="/dashboard/configuracoes"
          active={isNavItemActive("/dashboard/configuracoes", pathname)}
        />
        <NavItem icon={LogOut} label="Sair" variant="danger" onClick={handleLogout} />
      </div>
    </aside>
  );
}
