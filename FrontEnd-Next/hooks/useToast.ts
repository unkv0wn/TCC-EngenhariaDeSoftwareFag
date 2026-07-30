"use client";

import { useContext } from "react";

import { ToastContext, type ToastVariant } from "@/components/ui/toast/ToastProvider";

const TOAST_METHOD_TO_TOKEN: Record<"success" | "error" | "warning", ToastVariant> = {
  success: "success",
  error: "danger",
  warning: "warning",
};

export function useToast() {
  const context = useContext(ToastContext);

  if (!context) {
    throw new Error("useToast must be used within a ToastProvider");
  }

  const { addToast } = context;

  return {
    success: (title: string, description?: string) =>
      addToast(TOAST_METHOD_TO_TOKEN.success, title, description),
    error: (title: string, description?: string) =>
      addToast(TOAST_METHOD_TO_TOKEN.error, title, description),
    warning: (title: string, description?: string) =>
      addToast(TOAST_METHOD_TO_TOKEN.warning, title, description),
  };
}
