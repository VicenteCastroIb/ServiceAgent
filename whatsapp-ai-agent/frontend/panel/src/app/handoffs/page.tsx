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
      <h1 className="mb-1 text-xl font-semibold">Conversaciones pausadas</h1>
      <p className="mb-6 text-sm text-gray-500">
        Estos clientes fueron derivados a un humano. El bot no les responde hasta
        que reanudes la conversación.
      </p>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      <div className="overflow-hidden rounded-md border border-gray-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-gray-50 text-gray-500">
            <tr>
              <th className="px-4 py-2 font-medium">Cliente</th>
              <th className="px-4 py-2 font-medium">Motivo</th>
              <th className="px-4 py-2"></th>
            </tr>
          </thead>
          <tbody>
            {handoffs?.length === 0 && (
              <tr>
                <td colSpan={3} className="px-4 py-4 text-gray-500">
                  No hay conversaciones pausadas por ahora.
                </td>
              </tr>
            )}
            {handoffs?.map((h) => (
              <tr key={h.numeroCliente} className="border-t border-gray-100">
                <td className="px-4 py-2">{h.numeroCliente}</td>
                <td className="px-4 py-2 text-gray-500">{h.motivo}</td>
                <td className="px-4 py-2 text-right">
                  <button
                    onClick={() => onReanudar(h.numeroCliente)}
                    disabled={reanudando === h.numeroCliente}
                    className="rounded-md bg-green-600 px-3 py-1 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50"
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
