"use client";

import { useCallback, useEffect, useState } from "react";

import { ApiError } from "@/lib/apiClient";
import type { CompanySettingsFormData } from "@/lib/validations/settings";
import { getSettings, updateSettings as updateSettingsApi } from "@/services/settings";

export function useSettings() {
  const [settings, setSettings] = useState<CompanySettingsFormData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      setSettings(await getSettings());
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível carregar as configurações.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => {
      refresh();
    });
  }, [refresh]);

  const updateSettings = useCallback(async (data: CompanySettingsFormData) => {
    const updated = await updateSettingsApi(data);
    setSettings(updated);
    return updated;
  }, []);

  return { settings, isLoading, error, refresh, updateSettings };
}
