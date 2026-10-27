import { apiFetch } from "@/lib/apiClient";
import type { PanelUser } from "@/hooks/usePanelUsers";
import type { PanelUserFormData } from "@/lib/validations/panelUser";

/** Acessos ao painel. Como todo o resto do CRUD, exige a sessão do gestor. */
const BASE_PATH = "/api/users";

export function listPanelUsers(): Promise<PanelUser[]> {
  return apiFetch<PanelUser[]>(BASE_PATH);
}

export function createPanelUser(data: PanelUserFormData): Promise<PanelUser> {
  return apiFetch<PanelUser>(BASE_PATH, { method: "POST", body: data });
}

export function updatePanelUser(id: string, data: PanelUserFormData): Promise<PanelUser> {
  return apiFetch<PanelUser>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deletePanelUser(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}

/** Quem está logado. O cookie é httpOnly, então é o servidor que responde isso. */
export function fetchCurrentUser(): Promise<{ id: string; name: string; email: string }> {
  return apiFetch<{ id: string; name: string; email: string }>("/api/auth/me");
}
