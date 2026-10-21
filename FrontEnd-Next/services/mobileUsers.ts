import { apiFetch } from "@/lib/apiClient";
import type { MobileUser } from "@/hooks/useMobileUsers";
import type { MobileUserFormData } from "@/lib/validations/mobileUser";

/**
 * Note o caminho: `/api/mobile-users`, e não `/api/mobile/...`. O prefixo `/api/mobile/`
 * é a superfície do celular e exige token de motorista; isto é cadastro do painel.
 */
const BASE_PATH = "/api/mobile-users";

export function listMobileUsers(): Promise<MobileUser[]> {
  return apiFetch<MobileUser[]>(BASE_PATH);
}

export function createMobileUser(data: MobileUserFormData): Promise<MobileUser> {
  return apiFetch<MobileUser>(BASE_PATH, { method: "POST", body: data });
}

export function updateMobileUser(id: string, data: MobileUserFormData): Promise<MobileUser> {
  return apiFetch<MobileUser>(`${BASE_PATH}/${id}`, { method: "PUT", body: data });
}

export function deleteMobileUser(id: string): Promise<void> {
  return apiFetch<void>(`${BASE_PATH}/${id}`, { method: "DELETE" });
}
