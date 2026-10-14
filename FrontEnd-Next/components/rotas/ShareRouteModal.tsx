"use client";

import { useEffect, useState } from "react";
import QRCode from "qrcode";
import { Check, Copy, Loader2, MessageCircle, RefreshCw } from "lucide-react";

import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import type { Driver } from "@/hooks/useDrivers";
import type { GeneratedRoute } from "@/hooks/useRoutes";
import type { Vehicle } from "@/hooks/useVehicles";
import { ApiError } from "@/lib/apiClient";
import {
  buildDeepLink,
  buildDriverMessage,
  buildWhatsAppLink,
  formatAccessCode,
} from "@/lib/routeAccessCode";
import { orderedStopSequence } from "@/lib/routeResult";
import { regenerateAccessCode } from "@/services/routes";

interface ShareRouteModalProps {
  route: GeneratedRoute;
  driver: Driver | undefined;
  vehicle: Vehicle | undefined;
  onClose: () => void;
  /** Avisa a tela de origem que o código mudou, pra lista refletir sem recarregar. */
  onCodeChanged: (code: string) => void;
}

/**
 * Entrega a rota ao motorista.
 *
 * Três caminhos, nesta ordem de preferência: apontar a câmera no QR (motorista no pátio,
 * ao lado do computador — não depende de sinal nem de telefone cadastrado), mandar pelo
 * WhatsApp (motorista que já saiu), ou ditar o código (quando a câmera não lê e o
 * WhatsApp não chega). O terceiro é o motivo de o código ter 12 caracteres e não 43.
 */
export function ShareRouteModal({ route, driver, vehicle, onClose, onCodeChanged }: ShareRouteModalProps) {
  const [code, setCode] = useState<string | null>(route.accessCode);
  /**
   * Guarda o código junto com a imagem: emitir um código novo dispara outra geração, e sem
   * esse par a promessa antiga poderia resolver depois e pintar o QR do código anterior.
   */
  const [qr, setQr] = useState<{ code: string; dataUrl: string } | null>(null);
  const [isIssuing, setIsIssuing] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!code) return;

    let cancelled = false;
    // `margin: 1` em vez do padrão 4: a "zona quieta" do padrão são 4 módulos, mas num QR
    // renderizado grande na tela 1 já basta e sobra área pro código embaixo.
    QRCode.toDataURL(buildDeepLink(code), { width: 480, margin: 1, errorCorrectionLevel: "M" })
      .then((dataUrl) => {
        if (!cancelled) setQr({ code, dataUrl });
      })
      .catch(() => {
        if (!cancelled) setError("Não foi possível gerar o QR Code.");
      });

    return () => {
      cancelled = true;
    };
  }, [code]);

  const qrDataUrl = qr && qr.code === code ? qr.dataUrl : null;

  async function issueCode() {
    setIsIssuing(true);
    setError(null);
    try {
      const updated = await regenerateAccessCode(route.id);
      if (updated.accessCode) {
        setCode(updated.accessCode);
        onCodeChanged(updated.accessCode);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Não foi possível gerar o código.");
    } finally {
      setIsIssuing(false);
    }
  }

  async function copyCode() {
    if (!code) return;
    try {
      await navigator.clipboard.writeText(formatAccessCode(code));
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setError("O navegador bloqueou a cópia. Selecione o código e copie manualmente.");
    }
  }

  const stopCount = orderedStopSequence(route.result, route.stops).length;
  const vehicleLabel = vehicle ? `${vehicle.plate} — ${vehicle.model}` : undefined;

  const whatsAppHref = code
    ? buildWhatsAppLink(
        driver?.phone,
        buildDriverMessage({
          code,
          driverFirstName: driver?.fullName.split(" ")[0],
          departureTime: route.departureTime,
          vehicleLabel,
          stopCount,
        })
      )
    : "";

  return (
    <Modal title="Compartilhar rota" onClose={onClose}>
      <div className="flex flex-col gap-4 px-6 py-5">
        {!code ? (
          <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-gray-300 px-6 py-8 text-center">
            <p className="text-[12.5px] font-semibold text-gray-500">
              Esta rota ainda não tem código de acesso.
            </p>
            <Button type="button" className="w-auto" onClick={issueCode} disabled={isIssuing}>
              {isIssuing ? "Gerando..." : "Gerar código"}
            </Button>
          </div>
        ) : (
          <>
            <div className="flex flex-col items-center gap-3">
              <div className="rounded-2xl border border-gray-200 bg-white p-3">
                {qrDataUrl ? (
                  // eslint-disable-next-line @next/next/no-img-element -- data URL gerada no cliente; next/image exigiria loader e não traria ganho
                  <img
                    src={qrDataUrl}
                    alt={`QR Code da rota, código ${formatAccessCode(code)}`}
                    className="h-[220px] w-[220px]"
                  />
                ) : (
                  <div className="flex h-[220px] w-[220px] items-center justify-center">
                    <Loader2 className="h-6 w-6 animate-spin text-gray-300" aria-hidden="true" />
                  </div>
                )}
              </div>
              <p className="text-center text-[12px] font-semibold text-gray-500">
                O motorista aponta a câmera do app para este código
              </p>
            </div>

            <div className="flex flex-col gap-2 rounded-xl border border-gray-200 bg-gray-50 px-4 py-3">
              <span className="text-[11px] font-bold uppercase tracking-wide text-gray-500">
                Ou digite no app
              </span>
              <div className="flex items-center justify-between gap-3">
                <code className="font-mono text-[22px] font-extrabold tracking-[0.12em] text-gray-900">
                  {formatAccessCode(code)}
                </code>
                <button
                  type="button"
                  onClick={copyCode}
                  title="Copiar código"
                  aria-label="Copiar código"
                  className="shrink-0 rounded-md p-1.5 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
                >
                  {copied ? (
                    <Check className="h-4 w-4 text-success-600" aria-hidden="true" />
                  ) : (
                    <Copy className="h-4 w-4" aria-hidden="true" />
                  )}
                </button>
              </div>
            </div>

            <div className="flex gap-2.5">
              <a
                href={whatsAppHref}
                target="_blank"
                rel="noopener noreferrer"
                aria-disabled={!driver?.phone}
                title={driver?.phone ? undefined : "Motorista sem telefone cadastrado."}
                className={
                  driver?.phone
                    ? "flex flex-1 items-center justify-center gap-1.5 rounded-lg border border-success-200 bg-success-50 py-2.5 text-[12.5px] font-bold text-success-700 hover:bg-success-100"
                    : "pointer-events-none flex flex-1 items-center justify-center gap-1.5 rounded-lg border border-gray-200 bg-gray-50 py-2.5 text-[12.5px] font-bold text-gray-400"
                }
              >
                <MessageCircle className="h-3.5 w-3.5" aria-hidden="true" />
                Enviar no WhatsApp
              </a>
              <button
                type="button"
                onClick={issueCode}
                disabled={isIssuing}
                title="Gera um código novo e invalida o atual"
                className="flex items-center justify-center gap-1.5 rounded-lg border border-gray-200 bg-white px-3.5 py-2.5 text-[12.5px] font-bold text-gray-600 hover:bg-gray-50 disabled:opacity-50"
              >
                <RefreshCw className={isIssuing ? "h-3.5 w-3.5 animate-spin" : "h-3.5 w-3.5"} aria-hidden="true" />
                Novo código
              </button>
            </div>

            <p className="text-center text-[11px] font-semibold text-gray-400">
              Quem tem o código abre esta rota — e só ela. Gerar um novo invalida o anterior.
            </p>
          </>
        )}

        {error && (
          <p className="rounded-lg border border-danger-200 bg-danger-50 px-3 py-2 text-[12px] font-semibold text-danger-700">
            {error}
          </p>
        )}
      </div>
    </Modal>
  );
}
