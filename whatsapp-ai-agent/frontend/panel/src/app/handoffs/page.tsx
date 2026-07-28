"use client";

import { useEffect, useState } from "react";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { Handoff, listarHandoffs, reanudarHandoff } from "@/lib/api";

export default function HandoffsPage() {
  const listo = useRequireAuth();
  const [handoffs, setHandoffs] = useState<Handoff[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reanudando, setReanudando] = useState<string | null>(null);

  function cargar() {
    listarHandoffs()
      .then(setHandoffs)
      .catch(() => setError("No se pudieron cargar las conversaciones."));
  }

  useEffect(() => {
    if (listo) cargar();
  }, [listo]);

  if (!listo) return null;

  async function onReanudar(numeroCliente: string) {
    setReanudando(numeroCliente);
    try {
      await reanudarHandoff(numeroCliente);
      cargar();
    } catch {
      setError("No se pudo reanudar la conversación.");
    } finally {
      setReanudando(null);
    }
  }

  return (
    <div>
      <h1 className="mb-1 text-[22px] font-bold tracking-[-0.01em] text-ink">Conversaciones pausadas</h1>
      <p className="mb-6 text-sm text-ink/50">
        Estos clientes fueron derivados a un humano. El bot no les responde hasta
        que reanudes la conversación.
      </p>

      {error && (
        <p className="mb-4 rounded-lg border border-error/20 bg-error-bg px-3.5 py-2.5 text-sm text-error">
          {error}
        </p>
      )}

      <div className="overflow-hidden rounded-[14px] border border-ink/10 bg-card">
        <table className="w-full text-left text-sm">
          <thead className="bg-cream text-xs font-semibold tracking-[0.02em] text-ink/45 uppercase">
            <tr>
              <th className="px-4 py-2.5 font-semibold">Cliente</th>
              <th className="px-4 py-2.5 font-semibold">Motivo</th>
              <th className="px-4 py-2.5"></th>
            </tr>
          </thead>
          <tbody>
            {handoffs?.length === 0 && (
              <tr>
                <td colSpan={3} className="px-4 py-6 text-center text-ink/45">
                  No hay conversaciones pausadas por ahora.
                </td>
              </tr>
            )}
            {handoffs?.map((h) => (
              <tr key={h.numeroCliente} className="border-t border-ink/8">
                <td className="px-4 py-2.5 text-ink">{h.numeroCliente}</td>
                <td className="px-4 py-2.5 text-ink/60">
                  <span className="inline-flex rounded-full border border-warn/25 bg-warn-bg px-2 py-0.5 text-[11px] font-semibold text-warn">
                    {h.motivo}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-right">
                  <button
                    onClick={() => onReanudar(h.numeroCliente)}
                    disabled={reanudando === h.numeroCliente}
                    className="rounded-[8px] px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50"
                    style={{ backgroundImage: "linear-gradient(90deg,#b9862f,#8a5f22)" }}
                  >
                    {reanudando === h.numeroCliente ? "..." : "Reanudar bot"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
