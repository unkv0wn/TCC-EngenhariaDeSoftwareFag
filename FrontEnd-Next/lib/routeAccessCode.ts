/**
 * Formatação e links do código de acesso da rota.
 *
 * Espelha `RouteAccessCodeGenerator` no backend: 12 caracteres em base32 de Crockford,
 * exibidos em grupos de 4. Aqui só formatamos e montamos links — a normalização de quem
 * digita acontece no servidor, que é quem decide se o código existe.
 */

const GROUP_SIZE = 4;

/** `XS4WJZ6P334C` → `XS4W-JZ6P-334C`. Grupos de quatro são o que se consegue ditar. */
export function formatAccessCode(code: string): string {
  const groups: string[] = [];
  for (let i = 0; i < code.length; i += GROUP_SIZE) {
    groups.push(code.slice(i, i + GROUP_SIZE));
  }
  return groups.join("-");
}

/**
 * Deep link que abre o app direto na rota. É o conteúdo do QR Code: assim a câmera nativa
 * do Android também oferece abrir o RouteWise, não só o leitor de dentro do app.
 */
export function buildDeepLink(code: string): string {
  return `routewise://rota/${code}`;
}

/** Mensagem pronta pro WhatsApp, com o deep link e o código para quem precisar digitar. */
export function buildDriverMessage(params: {
  code: string;
  driverFirstName?: string;
  departureTime: string;
  vehicleLabel?: string;
  stopCount: number;
}): string {
  const { code, driverFirstName, departureTime, vehicleLabel, stopCount } = params;

  return [
    `Olá${driverFirstName ? ` ${driverFirstName}` : ""}! Segue a rota de hoje:`,
    `Saída: ${departureTime}`,
    vehicleLabel ? `Veículo: ${vehicleLabel}` : null,
    `${stopCount} ${stopCount === 1 ? "parada" : "paradas"}`,
    "",
    "Abra no app RouteWise:",
    buildDeepLink(code),
    "",
    `Ou digite o código: ${formatAccessCode(code)}`,
  ]
    .filter((line): line is string => line !== null)
    .join("\n");
}

export function buildWhatsAppLink(phone: string | undefined, message: string): string {
  const digits = (phone ?? "").replace(/\D/g, "");
  return `https://wa.me/${digits ? `55${digits}` : ""}?text=${encodeURIComponent(message)}`;
}
