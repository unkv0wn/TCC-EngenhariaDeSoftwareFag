"use client";

import { useState } from "react";

// 8 itens é o que cabe sem esticar a altura da tela nas telas de lista atuais
// (cards em grid ou linhas de tabela) — mais que isso força scroll vertical.
export const DEFAULT_PAGE_SIZE = 8;

export interface PaginationState<T> {
  page: number;
  setPage: (page: number) => void;
  totalPages: number;
  pageItems: T[];
  total: number;
  /** Índice 1-based do primeiro item da página (0 se lista vazia). */
  from: number;
  /** Índice 1-based do último item da página. */
  to: number;
}

/**
 * Paginação client-side sobre uma lista já carregada/filtrada.
 * Volta pra página 1 sempre que a referência de `items` muda (novo filtro, busca,
 * recarga) — padrão "ajustar estado quando um prop muda" do React (sem efeito).
 */
export function usePagination<T>(items: T[], pageSize: number = DEFAULT_PAGE_SIZE): PaginationState<T> {
  const [page, setPage] = useState(1);

  const [trackedItems, setTrackedItems] = useState(items);
  if (trackedItems !== items) {
    setTrackedItems(items);
    setPage(1);
  }

  const total = items.length;
  const totalPages = Math.max(1, Math.ceil(total / pageSize));
  const currentPage = Math.min(page, totalPages);
  const start = (currentPage - 1) * pageSize;

  return {
    page: currentPage,
    setPage,
    totalPages,
    pageItems: items.slice(start, start + pageSize),
    total,
    from: total === 0 ? 0 : start + 1,
    to: Math.min(start + pageSize, total),
  };
}
