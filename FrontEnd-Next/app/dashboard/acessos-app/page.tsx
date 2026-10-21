import type { Metadata } from "next";

import { MobileUsersPageContent } from "@/components/mobile-users/MobileUsersPageContent";

export const metadata: Metadata = {
  title: "Acessos do App",
  description: "Controle quem entra no app do motorista e de qual celular.",
};

export default function AcessosAppPage() {
  return <MobileUsersPageContent />;
}
