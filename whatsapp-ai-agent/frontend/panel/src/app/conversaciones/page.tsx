"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { esAdminSegunToken } from "@/lib/auth";
import { ConversationSummary, listarConversaciones } from "@/lib/api";

const NOMBRE_CANAL: Record<ConversationSummary["channel"], string> = {
  WHATSAPP: "WhatsApp",
  INSTAGRAM: "Instagram",
};

export default function ConversacionesPage() {
  const listo = useRequireAuth();
  const esAdmin = esAdminSegunToken();
  const [conversaciones, setConversaciones] = useState<ConversationSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!listo) return;
    listarConversaciones()
      .then(setConversaciones)
      .catch(() => setError("No se pudieron cargar las conversaciones."));
  }, [listo]);

  if (!listo) return null;

  return (
    <div>
      <h1 className="mb-1 text-xl font-semibold">Conversaciones</h1>
      <p className="mb-6 text-sm text-gray-500">
        Historial completo de mensajes por WhatsApp e Instagram. Entrá a una para ver el chat y responder a mano.
      </p>

      {error && <p className="mb-4 text-sm text-red-600">{error}</p>}

      <div
        className="grid overflow-hidden rounded-md border border-gray-200 bg-white text-sm"
        style={{ gridTemplateColumns: esAdmin ? "1.2fr 0.8fr 1.4fr 1fr" : "0.8fr 1.4fr 1fr" }}
      >
        {esAdmin && <div className="bg-gray-50 px-4 py-2 font-medium text-gray-500">Negocio</div>}
        <div className="bg-gray-50 px-4 py-2 font-medium text-gray-500">Canal</div>
        <div className="bg-gray-50 px-4 py-2 font-medium text-gray-500">Cliente</div>
        <div className="bg-gray-50 px-4 py-2 font-medium text-gray-500">Último mensaje</div>

        {conversaciones?.length === 0 && (
          <div className="col-span-full px-4 py-4 text-gray-500">Todavía no hay conversaciones.</div>
        )}

        {conversaciones?.map((c) => (
          <Link key={c.id} href={`/conversaciones/${c.id}`} className="col-span-full contents">
            {esAdmin && (
              <div className="border-t border-gray-100 px-4 py-2 hover:bg-gray-50">{c.businessName}</div>
            )}
            <div className="border-t border-gray-100 px-4 py-2 hover:bg-gray-50">{NOMBRE_CANAL[c.channel]}</div>
            <div className="border-t border-gray-100 px-4 py-2 text-gray-500 hover:bg-gray-50">
              {c.clientContact}
            </div>
            <div className="border-t border-gray-100 px-4 py-2 text-gray-500 hover:bg-gray-50">
              {c.lastMessageAt ? c.lastMessageAt.replace("T", " ").slice(0, 16) : "-"}
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
