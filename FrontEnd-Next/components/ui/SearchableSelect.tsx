"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { AlertCircle, ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

export interface SearchableSelectOption {
  value: string;
  label: string;
}

interface SearchableSelectProps {
  label: string;
  placeholder?: string;
  options: readonly SearchableSelectOption[];
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  disabled?: boolean;
  error?: string;
  /** Quantos resultados mostrar de cada vez — o resto só aparece se a busca refinar. */
  maxResults?: number;
}

export function SearchableSelect({
  label,
  placeholder = "Selecione...",
  options,
  value,
  onChange,
  onBlur,
  disabled,
  error,
  maxResults = 15,
}: SearchableSelectProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState("");
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const selectedLabel = options.find((option) => option.value === value)?.label ?? "";

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    if (!normalizedQuery) return options;
    return options.filter((option) => option.label.toLowerCase().includes(normalizedQuery));
  }, [options, query]);

  const visible = filtered.slice(0, maxResults);
  const hiddenCount = filtered.length - visible.length;

  useEffect(() => {
    if (!isOpen) return;

    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setQuery("");
        onBlur?.();
      }
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsOpen(false);
        setQuery("");
        inputRef.current?.blur();
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  function handleSelect(option: SearchableSelectOption) {
    onChange(option.value);
    setQuery("");
    setIsOpen(false);
  }

  return (
    <div ref={containerRef} className="flex flex-col gap-1.5">
      {label && <label className="text-sm font-medium text-gray-700">{label}</label>}
      <div className="relative">
        <input
          ref={inputRef}
          type="text"
          disabled={disabled}
          value={isOpen ? query : selectedLabel}
          placeholder={placeholder}
          onFocus={() => {
            setIsOpen(true);
            setQuery("");
          }}
          onChange={(event) => setQuery(event.target.value)}
          aria-invalid={!!error}
          className={cn(
            "w-full rounded-lg border bg-white px-3.5 py-2.5 pr-9 text-sm text-gray-900",
            "transition-all duration-150 ease-out",
            "focus:outline-none focus:ring-2 focus:ring-primary-500/15 focus:border-primary-500",
            "disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-400",
            error ? "border-danger-300 focus:border-danger-500 focus:ring-danger-500/10" : "border-gray-200 hover:border-gray-300"
          )}
        />
        <ChevronDown
          className="pointer-events-none absolute inset-y-0 right-3 my-auto h-4 w-4 text-gray-400"
          aria-hidden="true"
        />

        {isOpen && !disabled && (
          <div
            role="listbox"
            className="absolute left-0 right-0 top-full z-10 mt-1 max-h-64 overflow-y-auto rounded-lg border border-gray-200 bg-white py-1 shadow-lg"
          >
            {visible.length === 0 ? (
              <p className="px-3.5 py-2 text-xs font-medium text-gray-400">Nenhum resultado.</p>
            ) : (
              visible.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  role="option"
                  aria-selected={option.value === value}
                  onClick={() => handleSelect(option)}
                  className={cn(
                    "flex w-full items-center px-3.5 py-2 text-left text-sm",
                    option.value === value ? "bg-primary-50 text-primary-700 font-semibold" : "text-gray-700 hover:bg-gray-50"
                  )}
                >
                  {option.label}
                </button>
              ))
            )}
            {hiddenCount > 0 && (
              <p className="border-t border-gray-100 px-3.5 py-1.5 text-[11px] font-medium text-gray-400">
                +{hiddenCount} resultado(s) — digite pra refinar a busca.
              </p>
            )}
          </div>
        )}
      </div>
      {error && (
        <p role="alert" className="flex items-center gap-1 text-xs font-medium text-red-600">
          <AlertCircle className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
          {error}
        </p>
      )}
    </div>
  );
}
