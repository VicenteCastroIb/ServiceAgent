"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useRequireAuth } from "@/lib/useRequireAuth";
import { useAuth } from "@/lib/auth-context";
import { ConversationSummary, listarConversaciones } from "@/lib/api";

const NOMBRE_CANAL: Record<ConversationSummary["channel"], string> = {
  WHATSAPP: "WhatsApp",
  INSTAGRAM: "Instagram",
};

export default function ConversacionesPage() {
  const listo = useRequireAuth();
  const { esAdmin } = useAuth();
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
      <h1 className="mb-1 text-[22px] font-bold tracking-[-0.01em] text-ink">Conversaciones</h1>
      <p className="mb-6 text-sm text-ink/50">
        Historial completo de mensajes por WhatsApp e Instagram. Entrá a una para ver el chat y responder a mano.
      </p>

      {error && (
        <p className="mb-4 rounded-lg border border-error/20 bg-error-bg px-3.5 py-2.5 text-sm text-error">
          {error}
        </p>
      )}

      <div
        className="grid overflow-hidden rounded-[14px] border border-ink/10 bg-card text-sm"
        style={{ gridTemplateColumns: esAdmin ? "1.2fr 0.8fr 1.4fr 1fr" : "0.8fr 1.4fr 1fr" }}
      >
        {esAdmin && (
          <div className="bg-cream px-4 py-2.5 text-xs font-semibold tracking-[0.02em] text-ink/45 uppercase">
            Negocio
          </div>
        )}
        <div className="bg-cream px-4 py-2.5 text-xs font-semibold tracking-[0.02em] text-ink/45 uppercase">
          Canal
        </div>
        <div className="bg-cream px-4 py-2.5 text-xs font-semibold tracking-[0.02em] text-ink/45 uppercase">
          Cliente
        </div>
        <div className="bg-cream px-4 py-2.5 text-xs font-semibold tracking-[0.02em] text-ink/45 uppercase">
          Último mensaje
        </div>

        {conversaciones?.length === 0 && (
          <div className="col-span-full px-4 py-6 text-center text-ink/45">Todavía no hay conversaciones.</div>
        )}

        {conversaciones?.map((c) => (
          <Link key={c.id} href={`/conversaciones/${c.id}`} className="col-span-full contents">
            {esAdmin && (
              <div className="border-t border-ink/8 px-4 py-2.5 text-ink hover:bg-cream">{c.businessName}</div>
            )}
            <div className="border-t border-ink/8 px-4 py-2.5 hover:bg-cream">
              <span
                className={`inline-flex rounded-full px-2 py-0.5 text-[11px] font-semibold ${
                  c.channel === "WHATSAPP" ? "bg-green/12 text-green-light" : "bg-violet/12 text-violet-light"
                }`}
              >
                {NOMBRE_CANAL[c.channel]}
              </span>
            </div>
            <div className="border-t border-ink/8 px-4 py-2.5 text-ink/60 hover:bg-cream">
              {c.clientContact}
            </div>
            <div className="border-t border-ink/8 px-4 py-2.5 text-ink/45 hover:bg-cream">
              {c.lastMessageAt ? c.lastMessageAt.replace("T", " ").slice(0, 16) : "-"}
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
