import type { Metadata } from "next";

import { DashboardPageContent } from "@/components/dashboard/DashboardPageContent";

export const metadata: Metadata = {
  title: "Dashboard",
  description: "O que a operação fez, e o que ela custou.",
};

export default function DashboardPage() {
  return <DashboardPageContent />;
}
