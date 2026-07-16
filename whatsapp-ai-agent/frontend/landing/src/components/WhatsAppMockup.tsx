"use client";

import { useEffect, useRef } from "react";
import { useReproduccionChat } from "@/lib/useReproduccionChat";

export interface ProductoChat {
  emoji: string;
  nombre: string;
  subtitulo?: string;
  precio: string;
  dominio: string;
}

export interface MensajeChat {
  de: "cliente" | "bot";
  texto?: string;
  hora: string;
  producto?: ProductoChat;
}

interface WhatsAppMockupProps {
  negocio: string;
  inicial?: string;
  mensajes: MensajeChat[];
  escribiendo?: boolean;
  /**
   * Si es true, en vez de mostrar todos los mensajes de una, los va
   * revelando de a uno (con el indicador "escribiendo..." antes de cada
   * respuesta del bot) apenas el mockup entra en pantalla, y hace loop.
   * Pensado para mockups de marketing en la landing (ver useReproduccionChat).
   * Ignora el prop `escribiendo` mientras está activo (lo maneja solo).
   */
  animado?: boolean;
  className?: string;
}

export default function WhatsAppMockup({
  negocio,
  inicial,
  mensajes,
  escribiendo = false,
  animado = false,
  className = "",
}: WhatsAppMockupProps) {
  const letra = inicial ?? negocio.charAt(0).toUpperCase();
  const { contenedorRef, mensajesVisibles, escribiendo: escribiendoAnimado } = useReproduccionChat(mensajes, animado);
  const mostrarEscribiendo = animado ? escribiendoAnimado : escribiendo;

  const mensajesRef = useRef<HTMLDivElement | null>(null);

  // Auto-scroll al último mensaje, como en un chat real, para que el
  // teléfono nunca cambie de tamaño aunque se acumulen mensajes.
  useEffect(() => {
    const nodo = mensajesRef.current;
    if (!nodo) return;
    nodo.scrollTo({ top: nodo.scrollHeight, behavior: "smooth" });
  }, [mensajesVisibles.length, mostrarEscribiendo]);

  return (
    <div
      ref={contenedorRef}
      className={`w-[300px] shrink-0 overflow-hidden rounded-[2.5rem] border-[10px] border-slate-950 bg-slate-950 shadow-2xl ${className}`}
    >
      <div className="overflow-hidden rounded-[1.75rem]">
        {/* Header del chat */}
        <div className="flex items-center gap-3 bg-[#075E54] px-4 py-3 text-white">
          <ChevronLeftIcon />
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-800 text-sm font-semibold">
            {letra}
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-semibold leading-tight">{negocio}</p>
            <p className="text-[11px] text-emerald-100/80">en línea</p>
          </div>
          <div className="flex items-center gap-3 text-emerald-100/90">
            <VideoIcon />
            <PhoneIcon />
            <DotsIcon />
          </div>
        </div>

        {/* Mensajes - alto fijo, hace scroll solo como un chat real */}
        <div
          ref={mensajesRef}
          className="flex h-[420px] flex-col gap-2 overflow-y-auto bg-[#ECE5DD] px-3 py-4 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
        >
          {mensajesVisibles.map((mensaje, i) => (
            <MessageBubble key={i} mensaje={mensaje} animado={animado} />
          ))}
          {mostrarEscribiendo && (
            <div
              className="flex w-fit items-center gap-1 self-start rounded-2xl rounded-bl-sm bg-white px-3 py-2.5 shadow-sm"
              style={animado ? { animation: "mensaje-chat-in 0.25s ease-out" } : undefined}
            >
              <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400 [animation-delay:-0.3s]" />
              <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400 [animation-delay:-0.15s]" />
              <span className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400" />
            </div>
          )}
        </div>

        {/* Input */}
        <div className="flex items-center gap-2 bg-[#ECE5DD] px-3 pt-1 pb-4">
          <div className="flex-1 rounded-full bg-white px-4 py-2 text-xs text-slate-400">Escribe un mensaje</div>
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#075E54] text-white">
            <MicIcon />
          </div>
        </div>
      </div>
    </div>
  );
}

function MessageBubble({ mensaje, animado }: { mensaje: MensajeChat; animado?: boolean }) {
  const esCliente = mensaje.de === "cliente";
  const estiloEntrada = animado ? { animation: "mensaje-chat-in 0.25s ease-out" } : undefined;

  if (mensaje.producto) {
    // Soporta texto + foto de producto en el mismo mensaje (el bot manda la
    // ficha con una frase a modo de caption, como haría un vendedor real).
    const p = mensaje.producto;
    return (
      <div
        className={`flex max-w-[85%] flex-col ${esCliente ? "self-end items-end" : "self-start items-start"}`}
        style={estiloEntrada}
      >
        <div className="overflow-hidden rounded-2xl bg-white shadow-sm">
          <div className="flex h-24 items-center justify-center bg-slate-100 text-4xl">{p.emoji}</div>
          <div className="px-3 py-2">
            <p className="text-xs font-semibold text-slate-800">{p.nombre}</p>
            {p.subtitulo && <p className="text-[11px] text-slate-500">{p.subtitulo}</p>}
            <p className="mt-0.5 text-xs font-semibold text-emerald-700">{p.precio}</p>
            <p className="mt-1 text-[9px] tracking-wide text-slate-400 uppercase">{p.dominio}</p>
          </div>
          {mensaje.texto && (
            <p className="whitespace-pre-line border-t border-slate-100 px-3 py-2 text-[13px] leading-snug text-slate-800">
              {mensaje.texto}
            </p>
          )}
        </div>
        <p className="mt-1 text-[10px] text-slate-400">{mensaje.hora}</p>
      </div>
    );
  }

  return (
    <div
      className={`max-w-[80%] rounded-2xl px-3 py-2 text-[13px] leading-snug shadow-sm ${
        esCliente ? "self-end rounded-br-sm bg-[#DCF8C6] text-slate-800" : "self-start rounded-bl-sm bg-white text-slate-800"
      }`}
      style={estiloEntrada}
    >
      <p className="whitespace-pre-line">{mensaje.texto}</p>
      <p className={`mt-1 flex items-center gap-1 text-[10px] text-slate-400 ${esCliente ? "justify-end" : ""}`}>
        {mensaje.hora}
        {esCliente && <CheckIcon />}
      </p>
    </div>
  );
}

function ChevronLeftIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M15 5l-7 7 7 7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function VideoIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M15 8.5v7a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-7a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1Zm0 2.2 5-2.7v8l-5-2.7"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function PhoneIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M6.6 10.8c1.3 2.6 3.4 4.7 6 6l2-2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.5.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1C10.6 21 3 13.4 3 4c0-.6.4-1 1-1h3.9c.6 0 1 .4 1 1 0 1.2.2 2.4.6 3.5.1.4 0 .8-.2 1l-2 2.3Z"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function DotsIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <circle cx="12" cy="5" r="1.6" />
      <circle cx="12" cy="12" r="1.6" />
      <circle cx="12" cy="19" r="1.6" />
    </svg>
  );
}

function MicIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M12 15a3 3 0 0 0 3-3V6a3 3 0 0 0-6 0v6a3 3 0 0 0 3 3Zm5-3a5 5 0 0 1-10 0M12 18v3"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg width="14" height="10" viewBox="0 0 16 11" fill="none" aria-hidden="true">
      <path
        d="M1 5.5 4.5 9 10 2.5M6 5.5 9.5 9 15 2.5"
        stroke="#53BDEB"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
