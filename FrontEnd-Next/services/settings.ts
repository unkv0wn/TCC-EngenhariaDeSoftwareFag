import { apiFetch } from "@/lib/apiClient";
import type { CompanySettingsFormData } from "@/lib/validations/settings";

const BASE_PATH = "/api/settings";

/** Shape achatado como o backend devolve/aceita. */
interface CompanySettingsApi {
  companyName: string;
  addressZipCode: string | null;
  addressStreet: string | null;
  addressNumber: string | null;
  addressDistrict: string | null;
  addressCity: string | null;
  addressState: string | null;
  latitude: number;
  longitude: number;
}

function toForm(api: CompanySettingsApi): CompanySettingsFormData {
  return {
    companyName: api.companyName,
    address: {
      zipCode: api.addressZipCode ?? "",
      street: api.addressStreet ?? "",
      number: api.addressNumber ?? "",
      district: api.addressDistrict ?? "",
      city: api.addressCity ?? "",
      state: api.addressState ?? "",
    },
    latitude: api.latitude,
    longitude: api.longitude,
  };
}

function toApi(data: CompanySettingsFormData): CompanySettingsApi {
  return {
    companyName: data.companyName,
    addressZipCode: data.address.zipCode || null,
    addressStreet: data.address.street || null,
    addressNumber: data.address.number || null,
    addressDistrict: data.address.district || null,
    addressCity: data.address.city || null,
    addressState: data.address.state || null,
    latitude: data.latitude,
    longitude: data.longitude,
  };
}

export async function getSettings(): Promise<CompanySettingsFormData> {
  return toForm(await apiFetch<CompanySettingsApi>(BASE_PATH));
}

export async function updateSettings(data: CompanySettingsFormData): Promise<CompanySettingsFormData> {
  return toForm(await apiFetch<CompanySettingsApi>(BASE_PATH, { method: "PUT", body: toApi(data) }));
}
