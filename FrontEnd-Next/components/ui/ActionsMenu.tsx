"use client";

import { useEffect, useRef, useState } from "react";
import { MoreVertical, type LucideIcon } from "lucide-react";
import { cn } from "@/lib/utils";

export interface ActionMenuItem {
  label: string;
  icon: LucideIcon;
  onClick: () => void;
  variant?: "default" | "danger";
}

interface ActionsMenuProps {
  actions: ActionMenuItem[];
  ariaLabel: string;
}

export function ActionsMenu({ actions, ariaLabel }: ActionsMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) return;

    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setIsOpen(false);
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  return (
    <div ref={containerRef} className="relative inline-block text-left">
      <button
        type="button"
        onClick={(event) => {
          event.stopPropagation();
          setIsOpen((value) => !value);
        }}
        aria-label={ariaLabel}
        aria-haspopup="true"
        aria-expanded={isOpen}
        className="rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
      >
        <MoreVertical className="h-3.5 w-3.5" aria-hidden="true" />
      </button>

      {isOpen && (
        <div
          role="menu"
          className="absolute right-0 top-full z-10 mt-1 w-44 overflow-hidden rounded-lg border border-gray-200 bg-white py-1 shadow-lg"
        >
          {actions.map((action) => {
            const Icon = action.icon;
            return (
              <button
                key={action.label}
                type="button"
                role="menuitem"
                onClick={(event) => {
                  event.stopPropagation();
                  setIsOpen(false);
                  action.onClick();
                }}
                className={cn(
                  "flex w-full items-center gap-2 px-3 py-2 text-left text-xs font-semibold",
                  action.variant === "danger" ? "text-danger-600 hover:bg-danger-50" : "text-gray-600 hover:bg-gray-50"
                )}
              >
                <Icon className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                {action.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}
