import type { CSSProperties } from "react";

export interface ChatMensaje {
  de: "cliente" | "bot";
  texto?: string;
  /** Lista de líneas producto/precio dentro del mismo mensaje del bot (ej. varios ítems de un pedido). */
  productos?: { nombre: string; precio: string }[];
  /** Ficha de un solo producto dentro del mensaje del bot (nombre, detalle y precio). */
  ficha?: { titulo: string; subtitulo?: string; precio: string };
}

interface ChatCardProps {
  negocio: string;
  /** Define el color del avatar, el estado y el tinte de las burbujas del cliente. */
  canal: "whatsapp" | "instagram";
  mensajes: ChatMensaje[];
  /** Línea final tipo "✓ Cita agendada por IA", debajo del último mensaje. */
  notaFinal?: string;
  className?: string;
  style?: CSSProperties;
}

export default function ChatCard({ negocio, canal, mensajes, notaFinal, className = "", style }: ChatCardProps) {
  const esWhatsapp = canal === "whatsapp";
  const letra = negocio.charAt(0).toUpperCase();

  return (
    <div
      style={style}
      className={`w-[290px] rounded-[22px] border p-[18px] shadow-[0_18px_40px_rgba(43,38,32,0.08)] ${
        esWhatsapp ? "border-ink/10 bg-card" : "border-violet/25 bg-card"
      } ${className}`}
    >
      <div className="flex items-center gap-2.5 border-b border-ink/8 pb-3">
        <div
          className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[15px] font-bold text-white ${
            esWhatsapp ? "bg-green" : "bg-violet"
          }`}
        >
          {letra}
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-[13.5px] font-semibold text-ink">{negocio}</p>
          <p className={`text-[11px] ${esWhatsapp ? "text-green-light" : "text-violet-light"}`}>
            ● {esWhatsapp ? "en línea · WhatsApp" : "activo · Instagram"}
          </p>
        </div>
      </div>

      <div className="flex flex-col gap-2 pt-3">
        {mensajes.map((m, i) => (
          <MessageBubble key={i} mensaje={m} esWhatsapp={esWhatsapp} />
        ))}
        {notaFinal && (
          <p className={`mt-0.5 flex items-center gap-1.5 text-[11px] font-semibold ${esWhatsapp ? "text-green-light" : "text-violet-light"}`}>
            ✓ {notaFinal}
          </p>
        )}
      </div>
    </div>
  );
}

function MessageBubble({ mensaje, esWhatsapp }: { mensaje: ChatMensaje; esWhatsapp: boolean }) {
  const esCliente = mensaje.de === "cliente";

  if (esCliente) {
    return (
      <div
        className={`max-w-[90%] self-end rounded-[14px] rounded-br-[3px] px-[11px] py-2 text-[12.5px] leading-[1.4] ${
          esWhatsapp ? "border border-green/20 bg-green/14 text-green-dark" : "border border-violet/25 bg-violet/14 text-violet-light"
        }`}
      >
        {mensaje.texto}
      </div>
    );
  }

  if (mensaje.productos && mensaje.productos.length > 0) {
    return (
      <div className="w-[92%] self-start rounded-[14px] rounded-bl-[3px] bg-cream-alt px-[11px] py-[9px]">
        {mensaje.texto && <p className="text-[12.5px] leading-[1.4] text-ink/85">{mensaje.texto}</p>}
        <div className="mt-2 flex flex-col gap-1.5 border-t border-ink/10 pt-2">
          {mensaje.productos.map((p) => (
            <div key={p.nombre} className="flex items-center justify-between gap-2">
              <span className="text-[11.5px] font-semibold text-ink">{p.nombre}</span>
              <span className="text-[11.5px] font-bold whitespace-nowrap text-green-light">{p.precio}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (mensaje.ficha) {
    return (
      <div className="w-[90%] self-start rounded-[14px] rounded-bl-[3px] bg-cream-alt px-[11px] py-[9px]">
        {mensaje.texto && <p className="text-[12.5px] leading-[1.4] text-ink/85">{mensaje.texto}</p>}
        <div className="mt-2 border-t border-ink/10 pt-2">
          <p className="text-xs font-semibold text-ink">{mensaje.ficha.titulo}</p>
          {mensaje.ficha.subtitulo && <p className="mt-0.5 text-[11px] text-ink/50">{mensaje.ficha.subtitulo}</p>}
          <p className="mt-1 text-[12.5px] font-bold text-green-light">{mensaje.ficha.precio}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-[90%] self-start rounded-[14px] rounded-bl-[3px] bg-cream-alt px-[11px] py-2 text-[12.5px] leading-[1.4] text-ink/85">
      {mensaje.texto}
    </div>
  );
}
