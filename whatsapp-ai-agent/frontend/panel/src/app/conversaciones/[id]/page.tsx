"use client";

import { FormEvent, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { useRequireAuth } from "@/lib/useRequireAuth";
import {
  ApiError,
  ConversationMessage,
  listarMensajesConversacion,
  responderConversacion,
} from "@/lib/api";

const ESTILO_BURBUJA: Record<ConversationMessage["sender"], string> = {
  CLIENTE: "self-start bg-gray-100 text-gray-900",
  BOT: "self-end bg-blue-50 text-blue-900",
  HUMANO: "self-end bg-green-600 text-white",
};

const NOMBRE_SENDER: Record<ConversationMessage["sender"], string> = {
  CLIENTE: "Cliente",
  BOT: "Bot",
  HUMANO: "Vos",
};

export default function ConversacionDetallePage() {
  const listo = useRequireAuth();
  const params = useParams<{ id: string }>();
  const conversationId = Number(params.id);

  const [mensajes, setMensajes] = useState<ConversationMessage[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);

  function cargar() {
    listarMensajesConversacion(conversationId)
      .then(setMensajes)
      .catch(() => setError("No se pudo cargar la conversación."));
  }

  useEffect(() => {
    if (listo) cargar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [listo, conversationId]);

  if (!listo) return null;

  async function onResponder(e: FormEvent) {
    e.preventDefault();
    if (!texto.trim()) return;
    setEnviando(true);
    setError(null);
    try {
      const enviado = await responderConversacion(conversationId, texto.trim());
      setMensajes((actuales) => [...(actuales ?? []), enviado]);
      setTexto("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "No se pudo enviar el mensaje.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="flex h-[calc(100vh-160px)] flex-col">
      <h1 className="mb-4 text-xl font-semibold">Conversación</h1>

      <div className="flex flex-1 flex-col gap-2 overflow-y-auto rounded-md border border-gray-200 bg-white p-4">
        {mensajes?.length === 0 && <p className="text-sm text-gray-500">Todavía no hay mensajes.</p>}
        {mensajes?.map((m) => (
          <div key={m.id} className={`flex max-w-[75%] flex-col rounded-lg px-3 py-2 text-sm ${ESTILO_BURBUJA[m.sender]}`}>
            <span className="mb-0.5 text-[10px] font-medium opacity-70">
              {NOMBRE_SENDER[m.sender]} · {m.sentAt.replace("T", " ").slice(0, 16)}
            </span>
            <span className="whitespace-pre-wrap">{m.content}</span>
          </div>
        ))}
      </div>

      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

      <form onSubmit={onResponder} className="mt-3 flex gap-2">
        <input
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          placeholder="Escribí una respuesta..."
          className="flex-1 rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none"
        />
        <button
          type="submit"
          disabled={enviando || !texto.trim()}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {enviando ? "..." : "Enviar"}
        </button>
      </form>
    </div>
  );
}
